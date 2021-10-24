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

package sh.props.group;

import java.util.function.UnaryOperator;
import sh.props.interfaces.Prop;
import sh.props.tuples.Pair;
import sh.props.tuples.Tuple;

/**
 * Groups two Props, retrieving them all at once. If any of the underlying Props fails to update its
 * value and calls {@link #onUpdateError(Throwable)}, the exception will be passed down to any
 * subscribers and the corresponding Prop will not be updated in the resulting {@link Tuple}.
 *
 * @param <T> the type of the first prop
 * @param <U> the type of the second prop
 */
class SynchronizedPair<T, U> extends AbstractPropGroup<Pair<T, U>> implements Prop<Pair<T, U>> {
  /**
   * Constructs a synchronized quad of values. At least two {@link Prop}s should be specified (not
   * nullable), otherwise using this implementation makes no sense.
   *
   * @param first the first prop
   * @param second the second prop
   */
  SynchronizedPair(Prop<T> first, Prop<U> second) {
    // generate a key represented by each prop
    super(AbstractPropGroup.multiKey(first.key(), second.key()));

    // subscribe to all updates and errors
    first.subscribe(v -> this.apply(SynchronizedPair::updateFirst, v), this::onUpdateError);
    second.subscribe(v -> this.apply(SynchronizedPair::updateSecond, v), this::onUpdateError);

    // retrieve the current state of the underlying props
    // it's important for this step to execute after we have subscribed to the underlying props
    // since any change operations will have been captured and will be applied on the underlying
    // atomic reference
    this.value.set(Tuple.of(first.get(), second.get()));
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its first value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @return a new object with the value updated
   */
  private static <T, U> UnaryOperator<Pair<T, U>> updateFirst(T value) {
    return prev -> Tuple.of(value, prev.second);
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its second value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @return a new object with the value updated
   */
  private static <T, U> UnaryOperator<Pair<T, U>> updateSecond(U value) {
    return prev -> Tuple.of(prev.first, value);
  }
}
