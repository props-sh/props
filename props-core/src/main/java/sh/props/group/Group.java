/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Mihai Bojin
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

import sh.props.Prop;
import sh.props.tuples.Pair;
import sh.props.tuples.Quad;
import sh.props.tuples.Quintuple;
import sh.props.tuples.Triple;

/** Convenience class for constructing groups of {@link Prop}s. */
public final class Group {

  /**
   * Groups two Props, allowing the user to retrieve all four values concurrently, via a {@link
   * Prop}, which can be read and consumed.
   *
   * @param first the first prop
   * @param second the second prop
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @return a synchronized Quad of props, which can be retrieved together
   */
  public static <T, U> Prop<Pair<T, U>> of(Prop<T> first, Prop<U> second) {
    return new PropGroupPair<>(first, second);
  }

  /**
   * Groups three Props, allowing the user to retrieve all four values concurrently, via a {@link
   * Prop}, which can be read and consumed.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   * @return a synchronized Quad of props, which can be retrieved together
   */
  public static <T, U, V> Prop<Triple<T, U, V>> of(Prop<T> first, Prop<U> second, Prop<V> third) {
    return new PropGroupTriple<>(first, second, third);
  }

  /**
   * Groups four Props, allowing the user to retrieve all four values concurrently, via a {@link
   * Prop}, which can be read and consumed.
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
  public static <T, U, V, W> Prop<Quad<T, U, V, W>> of(
      Prop<T> first, Prop<U> second, Prop<V> third, Prop<W> fourth) {
    return new PropGroupQuad<>(first, second, third, fourth);
  }

  /**
   * Groups five Props, allowing the user to retrieve all four values concurrently, via a {@link
   * Prop}, which can be read and consumed.
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
  public static <T, U, V, W, X> Prop<Quintuple<T, U, V, W, X>> of(
      Prop<T> first, Prop<U> second, Prop<V> third, Prop<W> fourth, Prop<X> fifth) {
    return new PropGroupQuintuple<>(first, second, third, fourth, fifth);
  }
}
