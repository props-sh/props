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

package sh.props.sources;

import java.util.Map;
import sh.props.Source;
import sh.props.SourceFactory;
import sh.props.annotations.Nullable;

/** Retrieves all environment variables. */
public class Environment extends Source {

  /**
   * Retrieves all environment variables.
   *
   * @return a map containing all env. variables
   */
  @Override
  public Map<String, String> get() {
    return System.getenv();
  }

  /** Factory implementation. */
  public static class Factory implements SourceFactory<Environment> {

    /**
     * Initializes an {@link Environment} source.
     *
     * @param ignored unused for this source
     * @return a constructed Source object
     */
    @Override
    public Environment create(@Nullable String ignored) {
      return new Environment();
    }
  }
}
