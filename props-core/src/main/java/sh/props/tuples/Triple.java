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
 * Represents a triple of objects.
 *
 * @param <T> the type of the first object
 * @param <U> the type of the second object
 * @param <V> the type of the third object
 */
public class Triple<T, U, V> extends Pair<T, U> {

  @Nullable public final V third;

  /**
   * Constructs the triple.
   *
   * @param first the first object
   * @param second the second object
   * @param third the third object
   */
  Triple(@Nullable T first, @Nullable U second, @Nullable V third) {
    super(first, second);
    this.third = third;
  }

  /**
   * Convert this triple to a pair, using its first two values.
   *
   * @return a pair containing this object's first two values
   */
  public Pair<T, U> toPair() {
    return Tuple.of(this.first, this.second);
  }

  /**
   * Constructs a new {@link Pair} with the updated value.
   *
   * @param value the new value to set
   * @return a new object with the value updated
   */
  @Override
  public Triple<T, U, V> updateFirst(@Nullable T value) {
    return new Triple<>(value, this.second, this.third);
  }

  /**
   * Constructs a new {@link Pair} with the updated value.
   *
   * @param value the new value to set
   * @return a new object with the value updated
   */
  @Override
  public Triple<T, U, V> updateSecond(@Nullable U value) {
    return new Triple<>(this.first, value, this.third);
  }

  /**
   * Constructs a new {@link Triple} with the updated value.
   *
   * @param value the new value to set
   * @return a new object with the value updated
   */
  public Triple<T, U, V> updateThird(@Nullable V value) {
    return new Triple<>(this.first, this.second, value);
  }

  /**
   * Generated equals implementation.
   *
   * @param o the object to compare
   * @return true if all three values are equal
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Triple)) {
      return false;
    }

    Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
    return Objects.equals(this.first, triple.first)
        && Objects.equals(this.second, triple.second)
        && Objects.equals(this.third, triple.third);
  }

  /**
   * Determines if the specified deconstructed arguments are the same as this tuple.
   *
   * @param first the first object to compare
   * @param second the second object to compare
   * @param third the third object to compare
   * @return true if all objects match the current tuple's underlying value
   */
  public boolean equalTo(T first, U second, V third) {
    return Objects.equals(this.first, first)
        && Objects.equals(this.second, second)
        && Objects.equals(this.third, third);
  }

  /**
   * Generated hashcode implementation.
   *
   * @return this object's computed hashcode
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.first, this.second, this.third);
  }

  /**
   * Renders this object as a string.
   *
   * @return this object's string representation
   */
  @Override
  public String toString() {
    return format("(%s, %s, %s)", this.first, this.second, this.third);
  }
}
