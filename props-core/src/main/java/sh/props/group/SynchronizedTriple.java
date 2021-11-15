/*
 * MIT License
 *
 * Copyright (c) 2021 - 2021 Mihai Bojin
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
import sh.props.AbstractProp;
import sh.props.interfaces.Prop;
import sh.props.tuples.Triple;
import sh.props.tuples.Tuple;

/**
 * Groups three Props, retrieving them all at once. If any of the underlying Props fails to update
 * its value and calls {@link #error(Throwable)}, the exception will be passed down to any
 * subscribers and the corresponding Prop will not be updated in the resulting {@link Tuple}.
 *
 * @param <T> the type of the first prop
 * @param <U> the type of the second prop
 * @param <V> the type of the third prop
 */
class SynchronizedTriple<T, U, V> extends AbstractPropGroup<Triple<T, U, V>>
    implements Prop<Triple<T, U, V>> {

  private final AbstractProp<T> first;
  private final AbstractProp<U> second;
  private final AbstractProp<V> third;

  /**
   * Constructs a synchronized quad of values. At least two {@link Prop}s should be specified (not
   * nullable), otherwise using this implementation makes no sense.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   */
  SynchronizedTriple(AbstractProp<T> first, AbstractProp<U> second, AbstractProp<V> third) {
    this.first = first;
    this.second = second;
    this.third = third;

    // subscribe to all updates and errors
    first.subscribe(v -> this.apply(SynchronizedTriple.updateFirst(v)), this::error);
    second.subscribe(v -> this.apply(SynchronizedTriple.updateSecond(v)), this::error);
    third.subscribe(v -> this.apply(SynchronizedTriple.updateThird(v)), this::error);

    // retrieve the current state of the underlying props
    // it's important for this step to execute after we have subscribed to the underlying props
    // since any change operations will have been captured and will be applied on the underlying
    // atomic reference
    this.initialize(Tuple.of(first.get(), second.get(), third.get()));
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its first value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @param <V> the type of the third object in the value
   * @return a new object with the value updated
   */
  private static <T, U, V> UnaryOperator<Triple<T, U, V>> updateFirst(T value) {
    return prev -> prev.updateFirst(value);
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its second value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @param <V> the type of the third object in the value
   * @return a new object with the value updated
   */
  private static <T, U, V> UnaryOperator<Triple<T, U, V>> updateSecond(U value) {
    return prev -> prev.updateSecond(value);
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its third value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @param <V> the type of the third object in the value
   * @return a new object with the value updated
   */
  private static <T, U, V> UnaryOperator<Triple<T, U, V>> updateThird(V value) {
    return prev -> prev.updateThird(value);
  }

  /**
   * Generates and returns a key for this object. The key is generated using {@link
   * AbstractPropGroup#multiKey(String, String...)}.
   *
   * @return the key that identifies this prop group
   */
  @Override
  public String key() {
    return AbstractPropGroup.multiKey(this.first.key(), this.second.key(), this.third.key());
  }
}
