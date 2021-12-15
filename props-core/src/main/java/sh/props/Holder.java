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

package sh.props;

import java.util.Objects;
import sh.props.annotations.Nullable;

/**
 * Holder class that keep references to a value/error, as well as an epoch that can be used to
 * determine the most current value in a series of concurrent operations.
 *
 * <p>This holder class can transition between values and errors, but cannot contain both at the
 * same time.
 *
 * @param <V> the type of the value held by this class
 */
public class Holder<V> {

  public final long epoch;
  public final @Nullable V value;

  @SuppressWarnings("UnusedVariable")
  public final @Nullable Throwable error;

  /** Default constructor that initializes with empty values, starting from epoch 0. */
  public Holder() {
    this(0, null, null);
  }

  /**
   * Internal constructor for initializing a new object.
   *
   * @param epoch the epoch to set
   * @param value a value
   * @param error or an error
   */
  private Holder(long epoch, @Nullable V value, @Nullable Throwable error) {
    this.epoch = epoch;
    this.value = value;
    this.error = error;
  }

  /**
   * Create a new Holder, with the updated value. This method updates the class's epoch.
   *
   * @param value the value to set
   * @return a new object
   */
  public Holder<V> value(@Nullable V value) {
    return new Holder<>(this.epoch + 1, value, null);
  }

  /**
   * Create a new Holder, with the specified error. This method updates the class's epoch.
   *
   * @param throwable the error that was encountered
   * @return a new object
   */
  public Holder<V> error(Throwable throwable) {
    return new Holder<>(this.epoch + 1, null, throwable);
  }

  /**
   * Returns the specified value, or throws the underlying {@link Throwable}.
   *
   * @return the associated value
   * @throws Throwable in case the Holder is in an errored state
   */
  @Nullable
  public V get() throws Throwable {
    if (error != null) {
      throw error;
    }

    return value;
  }

  @Override
  public String toString() {
    return error == null ? Objects.toString(value) : error.toString();
  }
}
