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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;
import sh.props.util.BackgroundExecutorFactory;

@Deprecated
public class SubscriberProxy<T> implements Subscribable<T> {

  private static final Logger log = Logger.getLogger(SubscriberProxy.class.getName());

  private final ReentrantLock ensureOrderedMessageReceipt = new ReentrantLock();
  private final List<Consumer<T>> updateConsumers = new ArrayList<>();
  private final List<Consumer<Throwable>> errorHandlers = new ArrayList<>();

  private final BlockingQueue<T> values = new LinkedBlockingQueue<>();
  private final BlockingQueue<Throwable> errors = new LinkedBlockingQueue<>();

  /** Class constructor. */
  @SuppressWarnings("FutureReturnValueIgnored")
  public SubscriberProxy() {
    ScheduledExecutorService executor = BackgroundExecutorFactory.create(2);
    executor.submit(this::sendValues);
    executor.submit(this::handleErrors);
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

  private void sendValues() {
    try {
      while (true) {
        T value = this.values.take();
        this.compute(this.updateConsumers, value);
      }
    } catch (InterruptedException e) {
      this.errors.add(e);
    }
  }

  private void handleErrors() {
    try {
      while (true) {
        Throwable throwable = this.errors.take();
        this.compute(this.errorHandlers, throwable);
      }
    } catch (InterruptedException e) {
      this.errors.add(e);
    }
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
   * Queues the update to be sent to all subscribers, via the {@link ForkJoinPool}.
   *
   * @param value the updated value
   */
  public void sendUpdate(@Nullable T value) {
    this.values.add(value);
  }

  /**
   * Signals to subscribers that an error occurred. In most cases this means that the value could
   * not be updated or {@link #sendUpdate(Object)}} threw an exception while accepting the value.
   *
   * @param throwable the thrown exception
   */
  public void handleError(Throwable throwable) {
    this.errors.add(throwable);
  }

  private <N> void compute(List<Consumer<N>> consumers, @Nullable N value) {
    if (consumers.isEmpty()) {
      // nothing to do if we have no consumers
      return;
    }

    // synchronize sending updates
    // only send one update at a time
    SubscriberProxy.this.ensureOrderedMessageReceipt.lock();
    try {
      consumers.forEach(c -> c.accept(value));
    } finally {
      // ensure the updated value was received by all consumers
      // before allowing a new update to be processed
      SubscriberProxy.this.ensureOrderedMessageReceipt.unlock();
    }
  }
}
