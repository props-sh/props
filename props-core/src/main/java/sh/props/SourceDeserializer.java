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

package sh.props;

import static java.lang.String.format;
import static java.util.function.Predicate.not;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import sh.props.annotations.Nullable;
import sh.props.source.Source;
import sh.props.source.SourceFactory;
import sh.props.source.impl.ClasspathPropertyFile;
import sh.props.source.impl.Environment;
import sh.props.source.impl.PropertyFile;
import sh.props.source.impl.SystemProperties;

/**
 * Allows reading and deserializing {@link Source} configuration from an input stream.
 *
 * <p>The syntax for defining a list of sources in a configuration file is: <code>KEY=options</code>
 * . The key is used to identify the appropriate {@link SourceFactory} that can instantiate the
 * desired source. The configuration line is then passed to the factory object, allowing it to
 * appropriately parse it and construct the {@link Source}.
 */
public class SourceDeserializer {

  /**
   * Allows external implementations to register additional sources, which will then be usable for
   * deserialization from a configuration file.
   *
   * @param key the key to register; must not have been previously bound
   * @param factory the {@link Source} this key should be instantiated to
   * @param <T> the type of the Source associated with the specified key
   */
  public static <T extends Source> void register(String key, SourceFactory<T> factory) {
    if (factory == null) {
      throw new IllegalArgumentException("Cannot register a null factory");
    }

    // register the source class and ensure the operation succeeded
    SourceFactory<? extends Source> prev = Holder.MAP.putIfAbsent(key.toLowerCase(), factory);
    if (prev != null) {
      throw new IllegalArgumentException(
          key
              + " cannot be overwritten; it was previously registered with "
              + prev.getClass().getSimpleName());
    }
  }

  /**
   * Reads {@link Source} configuration from the specified input stream and return an array of
   * sources, which can be passed to {@link sh.props.RegistryBuilder#RegistryBuilder(Source...)}.
   *
   * @param stream the input stream representing the configuration data
   * @return an array of Source objects
   */
  public static Source[] read(InputStream stream) {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, Charset.defaultCharset()))) {

      List<String> sourceConfig =
          reader.lines().filter(not(String::isBlank)).collect(Collectors.toList());
      return read(sourceConfig);

    } catch (Exception e) {
      throw new IllegalStateException("Error encountered while reading source configuration", e);
    }
  }

  /**
   * Reads {@link Source} configuration from the specified input stream and return an array of
   * sources, which can be passed to {@link sh.props.RegistryBuilder#RegistryBuilder(Source...)}.
   *
   * @param sourceConfig a list of source configurations
   * @return an array of Source objects
   * @throws IllegalStateException if the configuration line cannot be used to identify an *
   *     appropriate source
   */
  public static Source[] read(List<String> sourceConfig) {
    return sourceConfig.stream().map(SourceDeserializer::constructSource).toArray(Source[]::new);
  }

  /**
   * Helper method that attempts to construct a {@link Source} object by calling {@link
   * SourceFactory#create(String)} through Java reflection.
   *
   * @param line the configuration line to process
   * @return a constructed Source object
   * @throws IllegalStateException if the configuration line cannot be used to identify an
   *     appropriate source
   */
  @Nullable
  static Source constructSource(String line) {
    // identify an implementation that can process this config line
    String id = findId(line).toLowerCase();
    SourceFactory<? extends Source> factory = Holder.MAP.get(id);
    if (factory == null) {
      // fail if the configuration file contains other artifacts
      // this is to prevent users making wrong assumptions about their registry configuration
      // e.g., assuming a key configuration file is there, when in fact it isn't
      throw new IllegalStateException(
          format(
              "The specified config (%s) could not be mapped to a Source. Will not add to configuration.",
              id));
    }

    // construct a Source by parsing the specified line
    return factory.create(line);
  }

  /**
   * Finds a potential {@link Source} identifier in the specified line.
   *
   * @param line the line to process
   * @return the potential id
   */
  static String findId(String line) {
    line = line.trim();
    int pos = line.indexOf('=');
    if (pos == -1) {
      // return the line, as-is, since it doesn't contain any separators
      return line;
    }

    // return only the identifier
    return line.substring(0, pos);
  }

  /** Static holder, ensuring the map does not get initialized if this feature is not used. */
  private static class Holder {
    public static final Map<String, SourceFactory<? extends Source>> MAP = new HashMap<>();

    static {
      // define the core source implementations
      MAP.put(ClasspathPropertyFile.ID, new ClasspathPropertyFile.Factory());
      MAP.put(Environment.ID, new Environment.Factory());
      MAP.put(PropertyFile.ID, new PropertyFile.Factory());
      MAP.put(SystemProperties.ID, new SystemProperties.Factory());
    }
  }
}
