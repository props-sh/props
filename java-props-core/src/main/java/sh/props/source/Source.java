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

package sh.props.source;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface Source extends Supplier<Map<String, String>> {

  /**
   * An unique identifier representing this source in the {@link sh.props.Registry}.
   *
   * @return an unique id
   */
  String id();

  /**
   * Reads all known (key,value) pairs from the source.
   *
   * @return an updated {@link Map} containing all known key,value pairs
   */
  @Override
  Map<String, String> get();

  /**
   * Should return <code>true</code> after the source has been read at least once.
   *
   * @return true if initialized
   */
  boolean initialized();

  /**
   * Loads a {@link Properties} object from the passed {@link InputStream} and returns a {@link Map}
   * containing all key,value mappings.
   *
   * @param stream the stream to process
   * @return a valid map of properties
   * @throws IllegalArgumentException if a null <code>InputStream</code> was passed
   * @throws IOException if the <code>InputStream</code> cannot be read
   */
  default Map<String, String> loadPropertiesFromStream(InputStream stream) throws IOException {
    if (isNull(stream)) {
      throw new IllegalArgumentException(
          "loadPropertiesFromStream expects a non-null input stream");
    }

    Properties properties = new Properties();
    properties.load(stream);
    return this.readPropertiesToMap(properties);
  }

  /**
   * Iterates over all the input {@link Properties} and returns a {@link Map} containing all
   * key,value mappings.
   *
   * @param properties the properties object to read
   * @return a map containing all the defined properties
   */
  default Map<String, String> readPropertiesToMap(Properties properties) {
    return properties.stringPropertyNames().stream()
        .collect(Collectors.toUnmodifiableMap(Function.identity(), properties::getProperty));
  }
}
