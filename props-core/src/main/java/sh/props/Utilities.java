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

package sh.props;

import static java.lang.String.format;

import sh.props.annotations.Nullable;

public class Utilities {

  /**
   * Ensures that the specified param is not null.
   *
   * @param key the key to validate
   * @param param the name of the parameter
   * @param <T> the type of the key
   * @return the specified key
   */
  public static <T> T assertNotNull(@Nullable T key, String param) {
    if (key == null) {
      throw new IllegalArgumentException(format("%s cannot be null", param));
    }

    return key;
  }

  /**
   * Ensures thrown exceptions are unchecked.
   *
   * @param t the exception to be thrown
   * @return the object cast as {@link RuntimeException} or a new exception, wrapping the passed
   *     throwable
   */
  public static RuntimeException ensureUnchecked(Throwable t) {
    if (t instanceof RuntimeException) {
      return (RuntimeException) t;
    }

    return new RuntimeException(t);
  }
}
