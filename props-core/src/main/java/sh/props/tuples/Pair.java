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
 * Represents a pair of objects.
 *
 * @param <T> the type of the first object
 * @param <U> the type of the second object
 */
public class Pair<T, U> {

  @Nullable public final T first;
  @Nullable public final U second;

  /**
   * Constructs the pair. This method is marked package-private to direct the user to constructing
   * objects using {@link Tuple#of(Object, Object)}.
   *
   * @param first the first object
   * @param second the second object
   */
  Pair(@Nullable T first, @Nullable U second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Constructs a new {@link Pair} with the updated value.
   *
   * @param pair the original pair that will provide the other values
   * @param value the new value to set
   * @param <T> the type of the first object
   * @param <U> the type of the second object
   * @return a new object with the value updated
   */
  public static <T, U> Pair<T, U> updateFirst(@Nullable Pair<T, U> pair, @Nullable T value) {
    if (pair == null) {
      return new Pair<>(value, null);
    }

    return new Pair<>(value, pair.second);
  }

  /**
   * Constructs a new {@link Pair} with the updated value.
   *
   * @param pair the original pair that will provide the other values
   * @param value the new value to set
   * @param <T> the type of the first object
   * @param <U> the type of the second object
   * @return a new object with the value updated
   */
  public static <T, U> Pair<T, U> updateSecond(@Nullable Pair<T, U> pair, @Nullable U value) {
    if (pair == null) {
      return new Pair<>(null, value);
    }

    return new Pair<>(pair.first, value);
  }

  /**
   * Generated equals implementation.
   *
   * @param o the object to compare
   * @return true if both values are equal
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Pair)) {
      return false;
    }
    Pair<?, ?> pair = (Pair<?, ?>) o;
    return Objects.equals(this.first, pair.first) && Objects.equals(this.second, pair.second);
  }

  /**
   * Determines if the specified deconstructed arguments are the same as this tuple.
   *
   * @param first the first object to compare
   * @param second the second object to compare
   * @return true if all objects match the current tuple's underlying value
   */
  public boolean equalTo(T first, U second) {
    return Objects.equals(first, this.first) && Objects.equals(second, this.second);
  }

  /**
   * Generated hashcode implementation.
   *
   * @return this object's computed hashcode
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.first, this.second);
  }

  /**
   * Renders this object as a string.
   *
   * @return this object's string representation
   */
  @Override
  public String toString() {
    return format("(%s, %s)", this.first, this.second);
  }
}
