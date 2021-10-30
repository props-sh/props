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

package sh.props.interfaces;

import java.util.function.Supplier;
import sh.props.annotations.Nullable;
import sh.props.converter.Converter;

/**
 * Interface that is returned to the caller to explicitly call out the public contract exported by
 * this library.
 *
 * @param <T> the property's type
 */
public interface Prop<T> extends Supplier<T>, Subscribable<T> {
  /**
   * Helper method that can encode the provided value using a converter, if the associated prop
   * implements {@link Converter}.
   *
   * @param <PropT> the type of the prop
   * @param value the value to encode as string
   * @param prop the prop to use as a Converter
   * @return the prop's value string representation, or null if the provided value was null
   */
  @Nullable
  static <T, PropT extends Prop<T>> String encodeValue(@Nullable T value, PropT prop) {
    // if the value is null, stop here
    if (value == null) {
      return null;
    }

    if (prop instanceof Converter) {
      // if the Prop is a Converter, use it to encode the value
      @SuppressWarnings("unchecked")
      Converter<T> converter = (Converter<T>) prop;
      return converter.encode(value);
    }

    // fall back to using Object.toString()
    return value.toString();
  }

  /**
   * Designates this {@link Prop}'s key identifier.
   *
   * @return a string id
   */
  String key();
}
