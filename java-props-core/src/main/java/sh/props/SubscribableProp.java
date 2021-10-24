/*
 * MIT License
 *
 * Copyright (c) 2021 Mihai Bojin
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
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;

/**
 * This class implements the base functionality required to notify subscribers asynchronously that a
 * {@link BaseProp} has updated its value, or that an update operation failed.
 *
 * <p>* It's important to note that this implementation will attempt to send a single event when the
 * value in concurrently updated, but it will send each error when one is encountered.
 *
 * @param <T> the type of the prop object
 */
public abstract class SubscribableProp<T> extends AbstractProp<T> implements Prop<T> {

  private static final Logger log = Logger.getLogger(SubscribableProp.class.getName());
  protected final List<Consumer<T>> updateHandlers = new CopyOnWriteArrayList<>();
  protected final List<Consumer<Throwable>> errorHandlers = new CopyOnWriteArrayList<>();
  private final ReentrantLock sendStage = new ReentrantLock();
  protected AtomicLong epoch = new AtomicLong();

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
   * Retrieves the current value of the prop.
   *
   * @return the current value of this object
   */
  @Override
  @Nullable
  public abstract T get();

  /**
   * Notifies all subscribers of the newest value of this prop. This method does not accept a value
   * but instead retrieves the latest value at the time of execution (the update is processed using
   * the default {@link ForkJoinPool}, only if another update is not in-progress and also ensuring a
   * single update event for a group of concurrent changes.
   */
  protected void onUpdatedValue() {
    if (this.updateHandlers.isEmpty()) {
      // nothing to do if we have no consumers
      return;
    }

    // Mark the epoch at which this value was updated.
    // This is important since, by submitting the update to be processed by the ForkJoinPool
    // we have no guarantees regarding the order of execution.
    // Without this mechanism, the subscribing code could end up receiving updates in the wrong
    // order, e.g.: ('newer', 'older'), while the current value of the prop equals 'newer'
    long currentEpoch = this.epoch.incrementAndGet();

    ForkJoinPool.commonPool()
        .execute(
            () -> {
              if (Long.compareUnsigned(currentEpoch, this.epoch.get()) < 0) {
                // we know a more recent update took place
                // skip this execution
                return;
              }

              if (!this.sendStage.tryLock()) {
                // another thread is already sending
                // skip this execution
                return;
              }

              try {
                // send the same value to all consumers
                T value = this.get();
                this.updateHandlers.forEach(c -> c.accept(value));
              } finally {
                // ensure the updated value was received by all consumers
                // before allowing a new update to be processed
                this.sendStage.unlock();
              }
            });
  }

  /**
   * Notifies subscribers of any errors that occurred while updating this object's value.
   *
   * @param t the error that prevented the update
   */
  protected void onUpdateError(Throwable t) {
    this.errorHandlers.forEach(c -> c.accept(t));
  }
}
