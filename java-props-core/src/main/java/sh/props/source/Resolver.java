/*
 * MIT License
 *
 * Copyright (c) 2020 Mihai Bojin
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

package sh.props.source;

import java.util.Set;
import sh.props.annotations.Nullable;

public interface Resolver {

  /** Returns a string identifying the resolver. */
  String id();

  /**
   * Returns the value of the specified key, or <code>null</code> if the property is not defined.
   */
  @Nullable
  String get(String key);

  /**
   * Reloads all properties managed by the implementing Resolver.
   *
   * @return a {@link Set} of all the property keys updated by the last execution of this method
   */
  Set<String> reload();

  /**
   * Returns <code>true</code> if the implementation can reload its properties, or <code>false
   * </code> if it cannot.
   */
  default boolean isReloadable() {
    return true;
  }
}
