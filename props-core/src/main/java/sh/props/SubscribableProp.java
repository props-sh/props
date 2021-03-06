/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Mihai Bojin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package sh.props;

import static java.lang.String.format;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.LongUnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;

/**
 * This class implements the base functionality required to notify subscribers asynchronously that a
 * prop has accepted or rejected a value update.
 *
 * <p>It's important to note that this implementation ensures only the most recent events are sent
 * to subscribers. Coincidentally it does not ensure that every single update event is sent to
 * downstream subscribers. If multiple values or errors are received concurrently, events out of
 * order (from the epoch's point of view) will not be sent.
 *
 * <p>For example, if events are processed in the <code>T1 T2 T3</code> order, all events will be
 * sent, however, if events are processed in the <code>T3 T1 T2</code> order, only <code>T3</code>
 * will be sent.
 *
 * @param <T> the type of the prop object
 */
public abstract class SubscribableProp<T> implements Subscribable<T> {

  private static final Logger log = Logger.getLogger(SubscribableProp.class.getName());
  protected final List<Consumer<T>> updateHandlers = new CopyOnWriteArrayList<>();
  protected final List<Consumer<Throwable>> errorHandlers = new CopyOnWriteArrayList<>();
  private final ReentrantLock sendStage = new ReentrantLock();
  protected AtomicLong lastProcessedEpoch = new AtomicLong();
  @Nullable protected Scheduler scheduler = null;

  /**
   * Wrap the passed consumer and catch any exceptions. If any exceptions are thrown by {@link
   * Consumer#accept(Object)}, they will be logged. If <code>onError</code> is non-null, any
   * exceptions thrown by the <code>consumer</code> will be sent to it.
   *
   * @param consumer the consumer to wrap
   * @param onError an error handler
   * @param <T> the type of the consumer
   * @return a wrapped, safe consumer that never throws exceptions
   */
  static <T> Consumer<T> safe(Consumer<T> consumer, @Nullable Consumer<Throwable> onError) {
    return value -> {
      try {
        consumer.accept(value);
      } catch (Exception e) {
        // do not allow any exceptions to permeate out of this consumer

        // log exceptions so that developers are aware where they originated
        log.log(Level.WARNING, e, () -> format("Unexpected exception in consumer %s", consumer));

        // and also pass them to the associated Throwable handler
        if (onError != null) {
          onError.accept(e);
        }
      }
    };
  }

  private static LongUnaryOperator setIfMoreRecent(long epoch) {
    return last -> {
      if (Long.compareUnsigned(epoch, last) > 0) {
        // update the epoch if it is more recent than the last processed epoch
        return epoch;
      }

      // otherwise, keep the last processed epoch, since it is more current
      return last;
    };
  }

  /**
   * Subscribes the passed update/error consumers. This method is thread-safe, due to relying on
   * {@link CopyOnWriteArrayList} as a backing data structure for the two lists holding update/error
   * handlers.
   *
   * <p>The expected usage pattern for this class is that one or a few subscribers will be
   * registered initially, after which the implementation will iterate over these collections when
   * notifying subscribers of updates. The read ops will vastly outnumber the writes.
   *
   * @param onUpdate called when a new value is received (favoring the most recent value)
   * @param onError called when an error occurs (a value cannot be received)
   */
  @Override
  public void subscribe(Consumer<T> onUpdate, Consumer<Throwable> onError) {
    this.updateHandlers.add(safe(onUpdate, onError));
    this.errorHandlers.add(safe(onError, null));
  }

  /**
   * Accepts a value and an epoch. Since this method offloads the actual work of sending the
   * notification to a {@link ForkJoinPool} (provided by {@link #scheduler}), the value is
   * accompanied by an ever-increasing epoch. The epoch is used to determine if the event should
   * still be propagated, thus ensuring that only the most recent updates are propagated.
   *
   * <p>For example, since the ForkJoinPool could be busy with other work, the value could be
   * updated to A and then quickly to B, before the task has a chance to run. In that case, only B
   * should be sent to subscribers, and A should be discarded.
   *
   * @param value the value to set
   * @param epoch the epoch at which this value was recorded
   */
  protected void onValueUpdate(@Nullable T value, long epoch) {
    if (this.updateHandlers.isEmpty()) {
      // nothing to do if we have no consumers
      return;
    }

    Scheduler runner = scheduler != null ? scheduler : Scheduler.instance();
    runner.forkJoinExecute(
        () -> {
          long current =
              this.lastProcessedEpoch.updateAndGet(SubscribableProp.setIfMoreRecent(epoch));

          // the epoch was not updated, which means that the current update event should be
          // discarded
          if (current != epoch) {
            return;
          }

          // lock to ensure that the updated value was received by all consumers
          // before allowing a new update to be processed
          this.sendStage.lock();

          try {
            // since some time may have passed, recheck that this epoch is still valid
            if (this.lastProcessedEpoch.get() != epoch) {
              // if not, discard the update
              return;
            }

            // send the same value to all consumers
            this.updateHandlers.forEach(c -> c.accept(value));
          } finally {
            // allow the next update to be sent
            this.sendStage.unlock();
          }
        });
  }

  /**
   * Notifies subscribers of any errors that occurred while updating this object's value.
   *
   * @param error the error that prevented the update
   * @param epoch the epoch at which this error was recorded
   */
  protected void onUpdateError(Throwable error, long epoch) {
    if (this.errorHandlers.isEmpty()) {
      // nothing to do if we have no consumers
      return;
    }

    // attempt to store the 'current' epoch
    long current = this.lastProcessedEpoch.updateAndGet(SubscribableProp.setIfMoreRecent(epoch));

    // the epoch was not updated, which means that the current update event should be
    // discarded
    if (current != epoch) {
      return;
    }

    this.sendStage.lock();
    try {
      // since some time may have passed, recheck that this epoch is still valid
      if (this.lastProcessedEpoch.get() != epoch) {
        // if not, discard the update
        return;
      }

      // send the same value to all consumers
      this.errorHandlers.forEach(c -> c.accept(error));
    } finally {
      // allow the next update to be sent
      this.sendStage.unlock();
    }
  }
}
