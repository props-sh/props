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

package sh.props.source; /*
                          * Copyright 2021 Mihai Bojin
                          *
                          * Licensed under the Apache License, Version 2.0 (the "License");
                          * you may not use this file except in compliance with the License.
                          * You may obtain a copy of the License at
                          *
                          *     http://www.apache.org/licenses/LICENSE-2.0
                          *
                          * Unless required by applicable law or agreed to in writing, software
                          * distributed under the License is distributed on an "AS IS" BASIS,
                          * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                          * See the License for the specific language governing permissions and
                          * limitations under the License.
                          */

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import sh.props.annotations.Nullable;

public interface Source {

  /**
   * An unique identifier representing this source in the {@link sh.props.Registry}.
   *
   * @return an unique id
   */
  String id();

  /**
   * Reads all known (key,value) pairs from the source.
   *
   * @return an updated {@link Map} containing all known properties
   */
  Map<String, String> read();

  /**
   * Retrieves a single value by key.
   *
   * @param key the key to retrieve
   * @return a {@link String} value (serialized), or <code>null</code> if they key was not found
   */
  @Nullable
  default String get(String key) {
    return this.read().get(key);
  }

  /**
   * Any implementing classes can decide how to implement this call.
   *
   * @param downstream a consumer that accepts any updates this source may be sending
   */
  void register(Consumer<Map<String, String>> downstream);

  /**
   * Loads a {@link Properties} object from the passed {@link InputStream} and returns a {@link Map}
   * containing all key->value mappings.
   *
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
   * key->value mappings.
   */
  default Map<String, String> readPropertiesToMap(Properties properties) {
    return properties.stringPropertyNames().stream()
        .collect(Collectors.toUnmodifiableMap(Function.identity(), System::getProperty));
  }
}
