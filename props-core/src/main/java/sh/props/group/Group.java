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

import sh.props.BaseProp;
import sh.props.tuples.Pair;
import sh.props.tuples.Quad;
import sh.props.tuples.Quintuple;
import sh.props.tuples.Triple;

/** Convenience class for constructing groups of {@link BaseProp}s. */
public final class Group {

  /**
   * Groups two Props, allowing the user to retrieve all four values concurrently, via a {@link
   * BaseProp}, which can be read and consumed.
   *
   * @param first the first prop
   * @param second the second prop
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @return a synchronized Quad of props, which can be retrieved together
   */
  public static <T, U> BaseProp<Pair<T, U>> of(BaseProp<T> first, BaseProp<U> second) {
    return new PropGroupPair<>(first, second);
  }

  /**
   * Groups three Props, allowing the user to retrieve all four values concurrently, via a {@link
   * BaseProp}, which can be read and consumed.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   * @return a synchronized Quad of props, which can be retrieved together
   */
  public static <T, U, V> BaseProp<Triple<T, U, V>> of(
      BaseProp<T> first, BaseProp<U> second, BaseProp<V> third) {
    return new PropGroupTriple<>(first, second, third);
  }

  /**
   * Groups four Props, allowing the user to retrieve all four values concurrently, via a {@link
   * BaseProp}, which can be read and consumed.
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
  public static <T, U, V, W> BaseProp<Quad<T, U, V, W>> of(
      BaseProp<T> first, BaseProp<U> second, BaseProp<V> third, BaseProp<W> fourth) {
    return new PropGroupQuad<>(first, second, third, fourth);
  }

  /**
   * Groups five Props, allowing the user to retrieve all four values concurrently, via a {@link
   * BaseProp}, which can be read and consumed.
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
  public static <T, U, V, W, X> BaseProp<Quintuple<T, U, V, W, X>> of(
      BaseProp<T> first,
      BaseProp<U> second,
      BaseProp<V> third,
      BaseProp<W> fourth,
      BaseProp<X> fifth) {
    return new PropGroupQuintuple<>(first, second, third, fourth, fifth);
  }
}
