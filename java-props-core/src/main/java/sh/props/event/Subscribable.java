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

package sh.props.event;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;

public class Subscribable<T> {

  private final int parallelThreshold;

  private static final Logger log = Logger.getLogger(Subscribable.class.getName());

  private final List<Consumer<T>> updateConsumers = new ArrayList<>();
  private final List<Consumer<Throwable>> errorHandlers = new ArrayList<>();

  /**
   * Class constructor.
   *
   * @param parallelThreshold the number of subscribers after which the processing is offloaded to
   *     the {@link ForkJoinPool}
   */
  public Subscribable(int parallelThreshold) {
    this.parallelThreshold = parallelThreshold;
  }

  /**
   * Constructs a {@link Subscribable} which notifies all subscribers synchronously.
   *
   * @param <T> the type of the subscribers
   * @return a holder of subscribers
   */
  public static <T> Subscribable<T> processSync() {
    return new Subscribable<>(0);
  }

  /**
   * Constructs a {@link Subscribable} which notifies all subscribers asynchronously, by offloading
   * sending the notifications to the {@link ForkJoinPool}.
   *
   * @param <T> the type of the subscribers
   * @return a holder of subscribers
   */
  public static <T> Subscribable<T> processAsync() {
    return new Subscribable<>(Integer.MAX_VALUE);
  }

  /**
   * Subscribes the passed update/error consumers. This method is not thread-safe and care must be
   * taken when registering subscribers.
   *
   * @param onUpdate called when a new value is received
   * @param onError called when an error occurs (a value cannot be received)
   */
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
        Subscribable.log.log(
            Level.WARNING, e, () -> format("Unexpected exception in consumer %s", consumer));

        // and also pass them to the associated Throwable handler
        if (onError != null) {
          onError.accept(e);
        }
      }
    };
  }

  /**
   * Sends the updated value to all subscribers. If more than {@link #parallelThreshold} subscribers
   * are registered, the update is asynchronously executed by the {@link ForkJoinPool}.
   *
   * @param value the updated value
   */
  public void sendUpdate(@Nullable T value) {
    syncAsyncProcessing(this.updateConsumers, this.parallelThreshold, value);
  }

  /**
   * Signals to subscribers that an error occurred. In most cases this means that the value could
   * not be updated or {@link #sendUpdate(Object)}} threw an exception while accepting the value.
   *
   * @param throwable the thrown exception
   */
  public void handleError(Throwable throwable) {
    syncAsyncProcessing(this.errorHandlers, this.parallelThreshold, throwable);
  }

  /**
   * Method that decides how to consume the value, based on the specified threshold.
   *
   * @param consumers the consumers to which the value is sent
   * @param parallelThreshold the threshold that decides if the processing should by sync/async
   * @param value the value to send
   * @param <T> the type of the value
   */
  private static <T> void syncAsyncProcessing(
      List<Consumer<T>> consumers, int parallelThreshold, @Nullable T value) {
    if (consumers.isEmpty()) {
      // nothing to do if we have no consumers
      return;
    }

    // if we have less than the threshold, process synchronously
    if (consumers.size() < parallelThreshold) {
      consumers.forEach(c -> c.accept(value));
      return;
    }

    // otherwise process asynchronously
    // TODO: Refactor as RecursiveAction and split into multiple subtasks
    ForkJoinTask<?> task = ForkJoinTask.adapt(() -> consumers.forEach(c -> c.accept(value)));
    ForkJoinPool.commonPool().execute(task);
  }
}
