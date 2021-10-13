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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import sh.props.tuples.Pair;
import sh.props.tuples.Tuple;

public class PairSupplier<T, U> implements Supplier<Pair<T, U>> {

  private final Prop<T> propT;
  private final Prop<U> propU;
  private final SubscriberProxy<Pair<T, U>> subscribers;
  private final AtomicLong epoch = new AtomicLong();

  /**
   * Constructs the provider. Any updates are processed synchronously by default.
   *
   * @param propT the first prop
   * @param propU the second prop
   */
  public PairSupplier(Prop<T> propT, Prop<U> propU) {
    this(propT, propU, SubscriberProxy.processSync());
  }

  /**
   * Constructs the provider.
   *
   * @param propT the first prop
   * @param propU the second prop
   * @param subscribers the subscribers that get notified when any of the values change
   */
  public PairSupplier(Prop<T> propT, Prop<U> propU, SubscriberProxy<Pair<T, U>> subscribers) {
    // initialize
    this.subscribers = subscribers;
    this.propT = propT;
    this.propU = propU;

    // subscribe to updates
    propT.subscribe(
        value -> this.subscribers.sendUpdate(Tuple.of(value, this.propU.value())),
        this.subscribers::handleError);
    propU.subscribe(
        value -> {
          this.subscribers.sendUpdate(Tuple.of(this.propT.value(), value));
        },
        this.subscribers::handleError);
  }

  @Override
  public Pair<T, U> get() {
    return Tuple.of(this.propT.value(), this.propU.value());
  }

  /**
   * Subscribes to value updates and errors.
   *
   * @param onUpdate called when any value is updated
   * @param onError called when an update fails
   */
  public void subscribe(Consumer<Pair<T, U>> onUpdate, Consumer<Throwable> onError) {
    this.subscribers.subscribe(onUpdate, onError);
  }
}
