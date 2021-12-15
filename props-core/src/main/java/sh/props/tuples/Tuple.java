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

package sh.props.tuples;

import static java.lang.String.format;

import java.util.Objects;
import sh.props.annotations.Nullable;

/**
 * Defines a tuple of values.
 *
 * @param <T> the type of the first object in the tuple
 * @param <U> the type of the second object in the tuple
 * @param <V> the type of the third object in the tuple
 * @param <W> the type of the fourth object in the tuple
 * @param <X> the type of the fifth object in the tuple
 */
public final class Tuple<T, U, V, W, X> extends Quad<T, U, V, W> {
  @Nullable public final X fifth;

  /**
   * Constructs the tuple.
   *
   * @param first the first object
   * @param second the second object
   * @param third the third object
   * @param fourth the fourth object
   * @param fifth the fifth object
   */
  Tuple(
      @Nullable T first,
      @Nullable U second,
      @Nullable V third,
      @Nullable W fourth,
      @Nullable X fifth) {
    super(first, second, third, fourth);
    this.fifth = fifth;
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
   * @return a triple of objects
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
   * @param <W> the fourth object's type
   * @return a quad of objects
   */
  public static <T, U, V, W> Quad<T, U, V, W> of(
      @Nullable T first, @Nullable U second, @Nullable V third, @Nullable W fourth) {
    return new Quad<>(first, second, third, fourth);
  }

  /**
   * Convenience method for constructing a quintuple of objects.
   *
   * @param first the first object
   * @param second the second object
   * @param third the third object
   * @param fourth the fourth object
   * @param fifth the fifth object
   * @param <T> the first object's type
   * @param <U> the second object's type
   * @param <V> the third object's type
   * @param <W> the fourth object's type
   * @param <X> the fifth object's type
   * @return a quintuple of objects
   */
  public static <T, U, V, W, X> Tuple<T, U, V, W, X> of(
      @Nullable T first,
      @Nullable U second,
      @Nullable V third,
      @Nullable W fourth,
      @Nullable X fifth) {
    return new Tuple<>(first, second, third, fourth, fifth);
  }

  /**
   * Convert this tuple to a pair, using its first two values.
   *
   * @return a pair containing this object's first two values
   */
  @Override
  public Pair<T, U> toPair() {
    return Tuple.of(this.first, this.second);
  }

  /**
   * Convert this tuple to a triple, using its first three values.
   *
   * @return a triple containing this object's first three values
   */
  @Override
  public Triple<T, U, V> toTriple() {
    return Tuple.of(this.first, this.second, this.third);
  }

  /**
   * Convert this tuple to a quad, using its first four values.
   *
   * @return a quad containing this object's first four values
   */
  public Quad<T, U, V, W> toQuad() {
    return Tuple.of(this.first, this.second, this.third, this.fourth);
  }

  /**
   * Constructs a new {@link Tuple} with the updated value.
   *
   * @param value the new value to set
   * @return a new object with the value updated
   */
  @Override
  public Tuple<T, U, V, W, X> updateFirst(@Nullable T value) {
    return new Tuple<>(value, this.second, this.third, this.fourth, this.fifth);
  }

  /**
   * Constructs a new {@link Tuple} with the updated value.
   *
   * @param value the new value to set
   * @return a new object with the value updated
   */
  @Override
  public Tuple<T, U, V, W, X> updateSecond(@Nullable U value) {
    return new Tuple<>(this.first, value, this.third, this.fourth, this.fifth);
  }

  /**
   * Constructs a new {@link Tuple} with the updated value.
   *
   * @param value the new value to set
   * @return a new object with the value updated
   */
  @Override
  public Tuple<T, U, V, W, X> updateThird(@Nullable V value) {
    return new Tuple<>(this.first, this.second, value, this.fourth, this.fifth);
  }

  /**
   * Constructs a new {@link Tuple} with the updated value.
   *
   * @param value the new value to set
   * @return a new object with the value updated
   */
  @Override
  public Tuple<T, U, V, W, X> updateFourth(@Nullable W value) {
    return new Tuple<>(this.first, this.second, this.third, value, this.fifth);
  }

  /**
   * Constructs a new {@link Tuple} with the updated value.
   *
   * @param value the new value to set
   * @return a new object with the value updated
   */
  public Tuple<T, U, V, W, X> updateFifth(@Nullable X value) {
    return new Tuple<>(this.first, this.second, this.third, this.fourth, value);
  }

  /**
   * Generated equals implementation.
   *
   * @param o the object to compare
   * @return true if all five values are equal
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Tuple)) {
      return false;
    }

    Tuple<?, ?, ?, ?, ?> tuple = (Tuple<?, ?, ?, ?, ?>) o;
    return Objects.equals(this.first, tuple.first)
        && Objects.equals(this.second, tuple.second)
        && Objects.equals(this.third, tuple.third)
        && Objects.equals(this.fourth, tuple.fourth)
        && Objects.equals(this.fifth, tuple.fifth);
  }

  /**
   * Determines if the specified deconstructed arguments are the same as this tuple.
   *
   * @param first the first object to compare
   * @param second the second object to compare
   * @param third the third object to compare
   * @param fourth the fourth object to compare
   * @param fifth the fifth object to compare
   * @return true if all objects match the current tuple's underlying value
   */
  public boolean equalTo(T first, U second, V third, W fourth, X fifth) {
    return Objects.equals(this.first, first)
        && Objects.equals(this.second, second)
        && Objects.equals(this.third, third)
        && Objects.equals(this.fourth, fourth)
        && Objects.equals(this.fifth, fifth);
  }

  /**
   * Generated hashcode implementation.
   *
   * @return this object's computed hashcode
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.first, this.second, this.third, this.fourth, this.fifth);
  }

  /**
   * Renders this object as a string.
   *
   * @return this object's string representation
   */
  @Override
  public String toString() {
    return format(
        "(%s, %s, %s, %s, %s)", this.first, this.second, this.third, this.fourth, this.fifth);
  }
}
