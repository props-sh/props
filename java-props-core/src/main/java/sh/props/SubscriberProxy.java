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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongUnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;

public class SubscriberProxy<T> implements Subscribable<T> {

  private final int maxNotificationPerTask;

  private static final Logger log = Logger.getLogger(SubscriberProxy.class.getName());

  private final List<Consumer<T>> updateConsumers = new ArrayList<>();
  private final List<Consumer<Throwable>> errorHandlers = new ArrayList<>();

  private final AtomicLong epoch = new AtomicLong();
  private final AtomicLong lastEpoch = new AtomicLong();

  /**
   * Class constructor.
   *
   * @param maxNotificationPerTask the maximum number of subscribers that is processed in a single
   *     {@link ForkJoinPool} task
   */
  public SubscriberProxy(int maxNotificationPerTask) {
    this.maxNotificationPerTask = maxNotificationPerTask;
  }

  /**
   * Subscribes the passed update/error consumers. This method is not thread-safe and care must be
   * taken when registering subscribers.
   *
   * @param onUpdate called when a new value is received
   * @param onError called when an error occurs (a value cannot be received)
   */
  @Override
  public void subscribe(Consumer<T> onUpdate, Consumer<Throwable> onError) {
    this.updateConsumers.add(safe(onUpdate, onError));
    this.errorHandlers.add(safe(onError, null));
  }

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
  private static <T> Consumer<T> safe(Consumer<T> consumer, @Nullable Consumer<Throwable> onError) {
    return value -> {
      try {
        consumer.accept(value);
      } catch (Exception e) {
        // do not allow any exceptions to permeate out of this consumer

        // log exceptions so that developers are aware where they originated
        SubscriberProxy.log.log(
            Level.WARNING, e, () -> format("Unexpected exception in consumer %s", consumer));

        // and also pass them to the associated Throwable handler
        if (onError != null) {
          onError.accept(e);
        }
      }
    };
  }

  /**
   * Sends the updated value to all subscribers. If more than {@link #maxNotificationPerTask}
   * subscribers are registered, the update is asynchronously executed by the {@link ForkJoinPool}.
   *
   * @param value the updated value
   */
  public void sendUpdate(@Nullable T value) {
    if (this.updateConsumers.isEmpty()) {
      // nothing to do if we have no consumers
      return;
    }

    // submit the update for processing
    var epoch = this.epoch.getAndIncrement();
    ForkJoinPool.commonPool()
        .execute(
            new SubNotifier<>(this.updateConsumers, 0, this.updateConsumers.size(), value, epoch));
  }

  private class SubNotifier<N> extends RecursiveAction {

    private static final long serialVersionUID = 7089582888343373810L;
    private final List<Consumer<N>> consumers;
    private final int start;
    private final int end;
    @Nullable private final N value;
    private final long epoch;

    private SubNotifier(
        List<Consumer<N>> consumers, int start, int end, @Nullable N value, long epoch) {
      this.consumers = consumers;
      this.start = start;
      this.end = end;
      this.value = value;
      this.epoch = epoch;
    }

    @Override
    protected void compute() {
      // split the work into multiple subtasks, if needed
      if (this.end - this.start > SubscriberProxy.this.maxNotificationPerTask) {
        int middle = this.start + (this.end - this.start) / 2;
        ForkJoinTask.invokeAll(
            new SubNotifier<>(this.consumers, this.start, middle, this.value, this.epoch),
            new SubNotifier<>(this.consumers, middle, this.end, this.value, this.epoch));
        return;
      }

      // determine if the update can be accepted
      SubscriberProxy.this.lastEpoch.updateAndGet(this.epochComparator(this.epoch));

      // notify consumers between start-end (non-inclusive), as long as the epoch is not stale
      for (int i = this.start; i < this.end && this.isNotStale(); i++) {
        // notify consumers
        this.consumers.get(i).accept(this.value);
      }
    }

    /**
     * Determines if the epoch is still correct.
     *
     * @return true if the epoch is valid and the operation can be applied
     */
    private boolean isNotStale() {
      return SubscriberProxy.this.lastEpoch.get() == this.epoch;
    }

    /**
     * Creates an {@link AtomicLong} comparator.
     *
     * @param epoch the epoch to test against
     * @return an operator that can be applied to a {@link AtomicLong}
     */
    private LongUnaryOperator epochComparator(long epoch) {
      return currentEpoch -> {
        // if a more recent epoch is passed
        if (Long.compareUnsigned(epoch, currentEpoch) > 0) {
          // return it
          return epoch;
        } else {
          // otherwise return the existing epoch
          return currentEpoch;
        }
      };
    }
  }

  /**
   * Signals to subscribers that an error occurred. In most cases this means that the value could
   * not be updated or {@link #sendUpdate(Object)}} threw an exception while accepting the value.
   *
   * @param throwable the thrown exception
   */
  public void handleError(Throwable throwable) {
    if (this.errorHandlers.isEmpty()) {
      // nothing to do if we have no consumers
      return;
    }

    // submit the update for processing
    var epoch = this.epoch.getAndIncrement();
    ForkJoinPool.commonPool()
        .execute(
            new SubNotifier<>(this.errorHandlers, 0, this.errorHandlers.size(), throwable, epoch));
  }
}
