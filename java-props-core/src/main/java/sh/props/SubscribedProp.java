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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import sh.props.annotations.Nullable;

/**
 * Encapsulates the logic for notifying subscribers on value updates or errors while receiving these
 * updates.
 *
 * @param <T> the Prop's parameter
 */
abstract class SubscribedProp<T> {

  private final AtomicReference<Consumer<T>> onUpdate = new AtomicReference<>();

  private final AtomicReference<Consumer<Throwable>> onError = new AtomicReference<>();

  /**
   * Subscribes two consumers to this Prop object. This method should only be called once.
   * Subsequent calls to this method will result in a no-op and the passed consumers will be
   * ignored.
   *
   * @param onUpdate called when a new value is received
   * @param onError called if a new value is received but cannot be set
   * @return true if the subscribers were set, or false if this is not the first time the method is
   *     called
   */
  public boolean subscribe(Consumer<T> onUpdate, Consumer<Throwable> onError) {
    return this.onUpdate.compareAndSet(null, onUpdate) && this.onError.compareAndSet(null, onError);
  }

  /**
   * Subscribes a consumer that accepts updated values. This method should only be called once.
   * Subsequent calls to this method will result in a no-op and the passed consumer will be ignored.
   *
   * <p>Any exceptions thrown while updating the value will be logged.
   *
   * @param onUpdate called when a new value is received
   * @return true if the subscribers were set, or false if this is not the first time the method is
   *     called
   */
  public boolean subscribe(Consumer<T> onUpdate) {
    return this.subscribe(onUpdate, this::logError);
  }

  /**
   * Sends the updated value to a subscribers (if one exists).
   *
   * @param value the updated value
   */
  protected void sendUpdate(@Nullable T value) {
    Consumer<T> consumer = this.onUpdate.get();
    if (consumer != null) {
      consumer.accept(value);
    }
  }

  /**
   * Signals the subscriber that the value could not be updated.
   *
   * @param throwable the thrown exception
   */
  protected void signalError(Throwable throwable) {
    Consumer<Throwable> consumer = this.onError.get();
    if (consumer != null) {
      consumer.accept(throwable);
    }
  }

  /**
   * Helper method used to log exceptions.
   *
   * @param t the exception to log
   */
  protected abstract void logError(Throwable t);
}
