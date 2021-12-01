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

package sh.props.converters;

import java.nio.file.Path;

/** Converter that returns the inputted {@link String}. */
public interface PathConverter extends Converter<Path> {

  /**
   * Prefixes the specified path with the user's home directory, if the input starts with <code>~
   * </code>.
   *
   * <p>It is the caller's responsibility to prevent any edge-cases or bad input (e.g., '~~'); this
   * method will simply replace the first tilda character with the user's home directory.
   *
   * @param path the path to process
   * @return the resulting path, prefixe with the user's home directory
   */
  static String replaceTildeWithUserHomeDir(String path) {
    // if the specified path starts with '~'
    if (path.startsWith("~")) {
      // prefix the path with the user's home directory
      return System.getProperty("user.home") + path.substring(1);
    }

    return path;
  }

  /**
   * If true, <code>~</code> will expand to the user's home directory, as returned by <code>
   * System.getProperty("user.home")</code>.
   *
   * @return true if a tilde should be expanded to the user's home directory
   */
  boolean expandUserHomeDir();

  /**
   * Parses the specified input string into a <code>Path</code>.
   *
   * <p>This method supports expanding <code>~</code> to the user's home directory (as returned by
   * <code>System.getProperty("user.home")</code>), if {@link #expandUserHomeDir()} returns <code>
   * true</code>.
   *
   * <p>This method does NOT support the <code>~username</code> shorthand (which will incorrectly
   * expand to <code>/path/to/home/usernameusername</code>.
   */
  @Override
  default Path decode(String value) {
    // determine if we are allowed to expand '~' to the user's home dir
    if (this.expandUserHomeDir()) {
      return Path.of(replaceTildeWithUserHomeDir(value));
    }
    return Path.of(value);
  }
}
