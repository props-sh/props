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

package sh.props.source;

import static java.lang.String.format;
import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Abstract class that permits subscribers to register for updates.
 *
 * <p>This functionality is useful for triggering a single {@link Source#get()}, which can be an
 * expensive operation, and notifying multiple layers.
 */
public abstract class Source implements Supplier<Map<String, String>>, Subscribable {

  protected final List<Consumer<Map<String, String>>> subscribers = new ArrayList<>();

  /**
   * Loads a {@link Properties} object from the passed {@link InputStream} and returns a {@link Map}
   * containing all key,value mappings.
   *
   * @param stream the stream to process
   * @return a valid map of properties
   * @throws IllegalArgumentException if a null <code>InputStream</code> was passed
   * @throws IOException if the <code>InputStream</code> cannot be read
   */
  protected static Map<String, String> loadPropertiesFromStream(InputStream stream)
      throws IOException {
    if (isNull(stream)) {
      throw new IllegalArgumentException(
          "loadPropertiesFromStream expects a non-null input stream");
    }

    Properties properties = new Properties();
    properties.load(stream);
    return Source.readPropertiesToMap(properties);
  }

  /**
   * Iterates over all the input {@link Properties} and returns a {@link Map} containing all
   * key,value mappings.
   *
   * @param properties the properties object to read
   * @return a map containing all the defined properties
   */
  protected static Map<String, String> readPropertiesToMap(Properties properties) {
    return properties.stringPropertyNames().stream()
        .collect(Collectors.toUnmodifiableMap(Function.identity(), properties::getProperty));
  }

  /**
   * An unique identifier representing this source in the {@link sh.props.Registry}.
   *
   * @return an unique id
   */
  public abstract String id();

  /**
   * Reads all known (key,value) pairs from the source.
   *
   * @return an updated {@link Map} containing all known key,value pairs
   */
  @Override
  public abstract Map<String, String> get();

  /**
   * Registers a new downstream subscriber.
   *
   * @param subscriber a subscriber that accepts any updates this source may be sending
   */
  @Override
  public void register(Consumer<Map<String, String>> subscriber) {
    this.subscribers.add(subscriber);
  }

  /** Triggers a {@link #get()} call and sends all values to the registered downstream consumers. */
  @Override
  public void updateSubscribers() {
    Map<String, String> data = Collections.unmodifiableMap(this.get());
    for (Consumer<Map<String, String>> subscriber : this.subscribers) {
      subscriber.accept(data);
    }
  }

  @Override
  public String toString() {
    return format("Source(%s)", this.id());
  }
}
