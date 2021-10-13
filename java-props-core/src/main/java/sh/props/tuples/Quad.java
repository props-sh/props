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
public class Quad<T, U, V, W> extends Triple<T, U, V> {

  @Nullable public final W fourth;

  /**
   * Constructs the triple.
   *
   * @param first the first object
   * @param second the second object
   * @param third the third object
   */
  Quad(@Nullable T first, @Nullable U second, @Nullable V third, @Nullable W fourth) {
    super(first, second, third);
    this.fourth = fourth;
  }

  /**
   * Constructs a new {@link Quad} with the updated value.
   *
   * @param value the new value to set
   * @return a new object with the value updated
   */
  @Override
  public Quad<T, U, V, W> updateFirst(T value) {
    System.out.println("Quad first: " + value);
    return new Quad<>(value, this.second, this.third, this.fourth);
  }

  /**
   * Constructs a new {@link Quad} with the updated value.
   *
   * @param value the new value to set
   * @return a new object with the value updated
   */
  @Override
  public Quad<T, U, V, W> updateSecond(U value) {
    System.out.println("Quad second: " + value);
    return new Quad<>(this.first, value, this.third, this.fourth);
  }

  /**
   * Constructs a new {@link Quad} with the updated value.
   *
   * @param value the new value to set
   * @return a new object with the value updated
   */
  @Override
  public Quad<T, U, V, W> updateThird(V value) {
    System.out.println("Quad third: " + value);
    return new Quad<>(this.first, this.second, value, this.fourth);
  }

  /**
   * Constructs a new {@link Quad} with the updated value.
   *
   * @param value the new value to set
   * @return a new object with the value updated
   */
  public Quad<T, U, V, W> updateFourth(W value) {
    System.out.println("Quad fourth: " + value);
    return new Quad<>(this.first, this.second, this.third, value);
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
