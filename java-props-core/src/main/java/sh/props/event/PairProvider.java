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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import sh.props.Prop;
import sh.props.tuples.Pair;
import sh.props.tuples.Tuple;

public class PairProvider<T, U> implements Supplier<Pair<T, U>> {

  protected final AtomicReference<T> refT = new AtomicReference<>();
  protected final AtomicReference<U> refU = new AtomicReference<>();
  private final Subscribable<Pair<T, U>> subscribers;

  /**
   * Constructs the provider. Any updates are processed synchronously by default.
   *
   * @param propT the first prop
   * @param propU the second prop
   */
  public PairProvider(Prop<T> propT, Prop<U> propU) {
    this(propT, propU, Subscribable.processSync());
  }

  /**
   * Constructs the provider.
   *
   * @param propT the first prop
   * @param propU the second prop
   * @param subscribers the subscribers that get notified when any of the values change
   */
  public PairProvider(Prop<T> propT, Prop<U> propU, Subscribable<Pair<T, U>> subscribers) {
    // initialize
    this.subscribers = subscribers;

    // set initial prop values
    this.refT.set(propT.value());
    this.refU.set(propU.value());

    // subscribe to individual updates
    propT.subscribe(this::updateT, this.subscribers::handleError);
    propU.subscribe(this::updateU, this.subscribers::handleError);
  }

  private void updateT(T t) {
    this.refT.set(t);
    this.subscribers.sendUpdate(Tuple.of(t, this.refU.get()));
  }

  protected void updateU(U u) {
    this.refU.set(u);
    this.subscribers.sendUpdate(Tuple.of(this.refT.get(), u));
  }

  @Override
  public Pair<T, U> get() {
    return Tuple.of(this.refT.get(), this.refU.get());
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
