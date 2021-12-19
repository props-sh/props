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

import static java.lang.String.format;
import static sh.props.Utilities.assertNotNull;

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
import sh.props.converters.Converter;

/**
 * Abstract class that permits subscribers to register for updates.
 *
 * <p>This functionality is useful for triggering a single {@link Source#get()}, which can be an
 * expensive operation, and notifying multiple layers.
 */
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public abstract class Source implements Supplier<Map<String, String>>, LoadOnDemand {

  protected final List<Consumer<Map<String, String>>> layers = new ArrayList<>();

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
    assertNotNull(stream, "input stream");

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
   * Reads all known (key,value) pairs from the source.
   *
   * @return an updated {@link Map} containing all known key,value pairs
   */
  @Override
  public abstract Map<String, String> get();

  /**
   * Registers a {@link sh.props.Layer}, allowing the Source to notify it on any updates.
   *
   * @param subscriber a subscribing Layer that accepts any updates this source may be sending
   */
  public void register(Consumer<Map<String, String>> subscriber) {
    this.layers.add(subscriber);
  }

  /** Triggers a {@link #get()} call and sends all values to all subscribing layers. */
  public void refresh() {
    sendLayerUpdate(Collections.unmodifiableMap(this.get()));
  }

  /**
   * Provides a unique reference for each Source implementation. By default, this method returns the
   * implementation's name and a unique hash code computed at runtime. This method can help users
   * reference a source in {@link sh.props.Registry#get(String, Converter, String)}.
   *
   * <p>This method is final and cannot be overwritten since that would create potential for
   * confusion. When a user desired to reference sources by a deterministic identifier (reproducible
   * in subsequent JVM runs), they should alias {@link Layer}s via {@link
   * sh.props.RegistryBuilder#withSource(Source, String)}.
   *
   * @return a unique reference that's meant to enable client code to identify sources in a {@link
   *     sh.props.Registry}, allowing users to retrieve a value directly from a source
   */
  public final String id() {
    var hex = Integer.toHexString(System.identityHashCode(this));
    return format("%s@%s", this.getClass().getSimpleName(), hex);
  }

  @Override
  public String toString() {
    return format("Source(%s)", id());
  }

  /**
   * Sends the provided <code>data</code> to the wrapping layer.
   *
   * @param data the data to send to the Layer
   */
  protected void sendLayerUpdate(Map<String, String> data) {
    for (Consumer<Map<String, String>> subscriber : this.layers) {
      subscriber.accept(data);
    }
  }
}
