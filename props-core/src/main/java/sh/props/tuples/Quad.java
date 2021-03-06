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

package sh.props.tuples;

import static java.lang.String.format;

import java.util.Objects;
import sh.props.annotations.Nullable;

/**
 * Represents a quadruple of objects.
 *
 * @param <T> the type of the first object
 * @param <U> the type of the second object
 * @param <V> the type of the third object
 * @param <W> the type of the fourth object
 */
public class Quad<T, U, V, W> extends Triple<T, U, V> {

  @Nullable public final W fourth;

  /**
   * Constructs the quad. This method is marked package-private to direct the user to constructing *
   * objects using {@link Tuple#of(Object, Object, Object, Object)}.
   *
   * @param first the first object
   * @param second the second object
   * @param third the third object
   * @param fourth the fourth object
   */
  Quad(@Nullable T first, @Nullable U second, @Nullable V third, @Nullable W fourth) {
    super(first, second, third);
    this.fourth = fourth;
  }

  /**
   * Constructs a new {@link Quad} with the updated value.
   *
   * @param quad the original quad that will provide the other values
   * @param value the new value to set
   * @param <T> the type of the first object
   * @param <U> the type of the second object
   * @param <V> the type of the third object
   * @param <W> the type of the fourth object
   * @return a new object with the value updated
   */
  public static <T, U, V, W> Quad<T, U, V, W> updateFirst(
      @Nullable Quad<T, U, V, W> quad, @Nullable T value) {
    if (quad == null) {
      return new Quad<>(value, null, null, null);
    }

    return new Quad<>(value, quad.second, quad.third, quad.fourth);
  }

  /**
   * Constructs a new {@link Quad} with the updated value.
   *
   * @param quad the original quad that will provide the other values
   * @param value the new value to set
   * @param <T> the type of the first object
   * @param <U> the type of the second object
   * @param <V> the type of the third object
   * @param <W> the type of the fourth object
   * @return a new object with the value updated
   */
  public static <T, U, V, W> Quad<T, U, V, W> updateSecond(
      @Nullable Quad<T, U, V, W> quad, @Nullable U value) {
    if (quad == null) {
      return new Quad<>(null, value, null, null);
    }

    return new Quad<>(quad.first, value, quad.third, quad.fourth);
  }

  /**
   * Constructs a new {@link Quad} with the updated value.
   *
   * @param quad the original quad that will provide the other values
   * @param value the new value to set
   * @param <T> the type of the first object
   * @param <U> the type of the second object
   * @param <V> the type of the third object
   * @param <W> the type of the fourth object
   * @return a new object with the value updated
   */
  public static <T, U, V, W> Quad<T, U, V, W> updateThird(
      @Nullable Quad<T, U, V, W> quad, @Nullable V value) {
    if (quad == null) {
      return new Quad<>(null, null, value, null);
    }

    return new Quad<>(quad.first, quad.second, value, quad.fourth);
  }

  /**
   * Constructs a new {@link Quad} with the updated value.
   *
   * @param quad the original quad that will provide the other values
   * @param value the new value to set
   * @param <T> the type of the first object
   * @param <U> the type of the second object
   * @param <V> the type of the third object
   * @param <W> the type of the fourth object
   * @return a new object with the value updated
   */
  public static <T, U, V, W> Quad<T, U, V, W> updateFourth(
      @Nullable Quad<T, U, V, W> quad, @Nullable W value) {
    if (quad == null) {
      return new Quad<>(null, null, null, value);
    }

    return new Quad<>(quad.first, quad.second, quad.third, value);
  }

  /**
   * Convert this quad to a pair, using its first two values.
   *
   * @return a pair containing this object's first two values
   */
  @Override
  public Pair<T, U> toPair() {
    return new Pair<>(this.first, this.second);
  }

  /**
   * Convert this quad to a triple, using its first three values.
   *
   * @return a triple containing this object's first three values
   */
  public Triple<T, U, V> toTriple() {
    return new Triple<>(this.first, this.second, this.third);
  }

  /**
   * Generated equals implementation.
   *
   * @param o the object to compare
   * @return true if all four values are equal
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Quad)) {
      return false;
    }

    Quad<?, ?, ?, ?> quad = (Quad<?, ?, ?, ?>) o;
    return Objects.equals(this.first, quad.first)
        && Objects.equals(this.second, quad.second)
        && Objects.equals(this.third, quad.third)
        && Objects.equals(this.fourth, quad.fourth);
  }

  /**
   * Determines if the specified deconstructed arguments are the same as this tuple.
   *
   * @param first the first object to compare
   * @param second the second object to compare
   * @param third the third object to compare
   * @param fourth the fourth object to compare
   * @return true if all objects match the current tuple's underlying value
   */
  public boolean equalTo(T first, U second, V third, W fourth) {
    return Objects.equals(this.first, first)
        && Objects.equals(this.second, second)
        && Objects.equals(this.third, third)
        && Objects.equals(this.fourth, fourth);
  }

  /**
   * Generated hashcode implementation.
   *
   * @return this object's computed hashcode
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.first, this.second, this.third, this.fourth);
  }

  /**
   * Renders this object as a string.
   *
   * @return this object's string representation
   */
  @Override
  public String toString() {
    return format("(%s, %s, %s, %s)", this.first, this.second, this.third, this.fourth);
  }
}
