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
 * Defines a tuple of values.
 *
 * @param <T> the type of the first object in the tuple
 * @param <U> the type of the second object in the tuple
 * @param <V> the type of the third object in the tuple
 * @param <W> the type of the fourth object in the tuple
 * @param <X> the type of the fifth object in the tuple
 */
public final class Quintuple<T, U, V, W, X> extends Quad<T, U, V, W> {
  @Nullable public final X fifth;

  /**
   * Constructs the tuple. This method is marked package-private to direct the user to constructing
   * objects using {@link Tuple#of(Object, Object, Object, Object, Object)}.
   *
   * @param first the first object
   * @param second the second object
   * @param third the third object
   * @param fourth the fourth object
   * @param fifth the fifth object
   */
  Quintuple(
      @Nullable T first,
      @Nullable U second,
      @Nullable V third,
      @Nullable W fourth,
      @Nullable X fifth) {
    super(first, second, third, fourth);
    this.fifth = fifth;
  }

  /**
   * Constructs a new {@link Quintuple} with the updated value.
   *
   * @param quintuple the original quintuple that will provide the other values
   * @param value the new value to set
   * @param <T> the type of the first object in the tuple
   * @param <U> the type of the second object in the tuple
   * @param <V> the type of the third object in the tuple
   * @param <W> the type of the fourth object in the tuple
   * @param <X> the type of the fifth object in the tuple
   * @return a new object with the value updated
   */
  public static <T, U, V, W, X> Quintuple<T, U, V, W, X> updateFirst(
      @Nullable Quintuple<T, U, V, W, X> quintuple, @Nullable T value) {
    if (quintuple == null) {
      return new Quintuple<>(value, null, null, null, null);
    }

    return new Quintuple<>(
        value, quintuple.second, quintuple.third, quintuple.fourth, quintuple.fifth);
  }

  /**
   * Constructs a new {@link Quintuple} with the updated value.
   *
   * @param quintuple the original quintuple that will provide the other values
   * @param value the new value to set
   * @param <T> the type of the first object in the tuple
   * @param <U> the type of the second object in the tuple
   * @param <V> the type of the third object in the tuple
   * @param <W> the type of the fourth object in the tuple
   * @param <X> the type of the fifth object in the tuple
   * @return a new object with the value updated
   */
  public static <T, U, V, W, X> Quintuple<T, U, V, W, X> updateSecond(
      @Nullable Quintuple<T, U, V, W, X> quintuple, @Nullable U value) {
    if (quintuple == null) {
      return new Quintuple<>(null, value, null, null, null);
    }

    return new Quintuple<>(
        quintuple.first, value, quintuple.third, quintuple.fourth, quintuple.fifth);
  }

  /**
   * Constructs a new {@link Quintuple} with the updated value.
   *
   * @param quintuple the original quintuple that will provide the other values
   * @param value the new value to set
   * @param <T> the type of the first object in the tuple
   * @param <U> the type of the second object in the tuple
   * @param <V> the type of the third object in the tuple
   * @param <W> the type of the fourth object in the tuple
   * @param <X> the type of the fifth object in the tuple
   * @return a new object with the value updated
   */
  public static <T, U, V, W, X> Quintuple<T, U, V, W, X> updateThird(
      @Nullable Quintuple<T, U, V, W, X> quintuple, @Nullable V value) {
    if (quintuple == null) {
      return new Quintuple<>(null, null, value, null, null);
    }

    return new Quintuple<>(
        quintuple.first, quintuple.second, value, quintuple.fourth, quintuple.fifth);
  }

  /**
   * Constructs a new {@link Quintuple} with the updated value.
   *
   * @param quintuple the original quintuple that will provide the other values
   * @param value the new value to set
   * @param <T> the type of the first object in the tuple
   * @param <U> the type of the second object in the tuple
   * @param <V> the type of the third object in the tuple
   * @param <W> the type of the fourth object in the tuple
   * @param <X> the type of the fifth object in the tuple
   * @return a new object with the value updated
   */
  public static <T, U, V, W, X> Quintuple<T, U, V, W, X> updateFourth(
      @Nullable Quintuple<T, U, V, W, X> quintuple, @Nullable W value) {
    if (quintuple == null) {
      return new Quintuple<>(null, null, null, value, null);
    }

    return new Quintuple<>(
        quintuple.first, quintuple.second, quintuple.third, value, quintuple.fifth);
  }

  /**
   * Constructs a new {@link Quintuple} with the updated value.
   *
   * @param quintuple the original quintuple that will provide the other values
   * @param value the new value to set
   * @param <T> the type of the first object in the tuple
   * @param <U> the type of the second object in the tuple
   * @param <V> the type of the third object in the tuple
   * @param <W> the type of the fourth object in the tuple
   * @param <X> the type of the fifth object in the tuple
   * @return a new object with the value updated
   */
  public static <T, U, V, W, X> Quintuple<T, U, V, W, X> updateFifth(
      @Nullable Quintuple<T, U, V, W, X> quintuple, @Nullable X value) {
    if (quintuple == null) {
      return new Quintuple<>(null, null, null, null, value);
    }

    return new Quintuple<>(
        quintuple.first, quintuple.second, quintuple.third, quintuple.fourth, value);
  }

  /**
   * Convert this tuple to a pair, using its first two values.
   *
   * @return a pair containing this object's first two values
   */
  @Override
  public Pair<T, U> toPair() {
    return new Pair<>(this.first, this.second);
  }

  /**
   * Convert this tuple to a triple, using its first three values.
   *
   * @return a triple containing this object's first three values
   */
  @Override
  public Triple<T, U, V> toTriple() {
    return new Triple<>(this.first, this.second, this.third);
  }

  /**
   * Convert this tuple to a quad, using its first four values.
   *
   * @return a quad containing this object's first four values
   */
  public Quad<T, U, V, W> toQuad() {
    return new Quad<>(this.first, this.second, this.third, this.fourth);
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
    if (!(o instanceof Quintuple)) {
      return false;
    }

    Quintuple<?, ?, ?, ?, ?> tuple = (Quintuple<?, ?, ?, ?, ?>) o;
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
