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

package sh.props.tuples;

import sh.props.annotations.Nullable;

/** Helper class that provides a fluent interface for creating tuples. */
public final class Tuple {

  /** Private constructor, preventing instantiation. */
  private Tuple() {
    // intentionally not to be instantiated
  }

  /**
   * Convenience method for constructing a pair of objects.
   *
   * @param first the first object
   * @param second the second object
   * @param <T> the first object's type
   * @param <U> the second object's type
   * @return a pair of objects
   */
  public static <T, U> Pair<T, U> of(@Nullable T first, @Nullable U second) {
    return new Pair<>(first, second);
  }

  /**
   * Convenience method for constructing a pair of objects.
   *
   * @param first the first object
   * @param second the second object
   * @param third the third object
   * @param <T> the first object's type
   * @param <U> the second object's type
   * @param <V> the third object's type
   * @return a pair of objects
   */
  public static <T, U, V> Triple<T, U, V> of(
      @Nullable T first, @Nullable U second, @Nullable V third) {
    return new Triple<>(first, second, third);
  }

  /**
   * Convenience method for constructing a quadruple of objects.
   *
   * @param first the first object
   * @param second the second object
   * @param third the third object
   * @param fourth the fourth object
   * @param <T> the first object's type
   * @param <U> the second object's type
   * @param <V> the third object's type
   * @return a pair of objects
   */
  public static <T, U, V, W> Quad<T, U, V, W> of(
      @Nullable T first, @Nullable U second, @Nullable V third, @Nullable W fourth) {
    return new Quad<>(first, second, third, fourth);
  }
}
