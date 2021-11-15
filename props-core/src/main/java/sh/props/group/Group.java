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

import sh.props.AbstractProp;
import sh.props.interfaces.Subscribable;
import sh.props.tuples.Pair;
import sh.props.tuples.Quad;
import sh.props.tuples.Triple;
import sh.props.tuples.Tuple;

public class Group {

  /**
   * Groups two Props, allowing the user to retrieve all four values concurrently. The returned type
   * implements {@link Subscribable}, allowing the user to receive events when any of the values are
   * updated.
   *
   * @param first the first prop
   * @param second the second prop
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @return a synchronized Quad of props, which can be retrieved together
   */
  public static <T, U> AbstractPropGroup<Pair<T, U>> of(
      AbstractProp<T> first, AbstractProp<U> second) {
    return new SynchronizedPair<>(first, second);
  }

  /**
   * Groups three Props, allowing the user to retrieve all four values concurrently. The returned
   * type implements {@link Subscribable}, allowing the user to receive events when any of the
   * values are updated.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   * @return a synchronized Quad of props, which can be retrieved together
   */
  public static <T, U, V> AbstractPropGroup<Triple<T, U, V>> of(
      AbstractProp<T> first, AbstractProp<U> second, AbstractProp<V> third) {
    return new SynchronizedTriple<>(first, second, third);
  }

  /**
   * Groups four Props, allowing the user to retrieve all four values concurrently. The returned
   * type implements {@link Subscribable}, allowing the user to receive events when any of the
   * values are updated.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param fourth the fourth prop
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   * @param <W> the type of the fourth prop
   * @return a synchronized Quad of props, which can be retrieved together
   */
  public static <T, U, V, W> AbstractPropGroup<Quad<T, U, V, W>> of(
      AbstractProp<T> first,
      AbstractProp<U> second,
      AbstractProp<V> third,
      AbstractProp<W> fourth) {
    return new SynchronizedQuad<>(first, second, third, fourth);
  }

  /**
   * Groups five Props, allowing the user to retrieve all four values concurrently. The returned
   * type implements {@link Subscribable}, allowing the user to receive events when any of the
   * values are updated.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param fourth the fourth prop
   * @param fifth the fourth prop
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   * @param <W> the type of the fourth prop
   * @param <X> the type of the fifth prop
   * @return a synchronized Quad of props, which can be retrieved together
   */
  public static <T, U, V, W, X> AbstractPropGroup<Tuple<T, U, V, W, X>> of(
      AbstractProp<T> first,
      AbstractProp<U> second,
      AbstractProp<V> third,
      AbstractProp<W> fourth,
      AbstractProp<X> fifth) {
    return new SynchronizedTuple<>(first, second, third, fourth, fifth);
  }
}
