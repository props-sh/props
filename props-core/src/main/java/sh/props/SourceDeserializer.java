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
import static java.util.function.Predicate.not;
import static sh.props.Utilities.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import sh.props.annotations.Nullable;
import sh.props.interfaces.SourceFactory;
import sh.props.sources.ClasspathPropertyFile;
import sh.props.sources.Environment;
import sh.props.sources.PropertyFile;
import sh.props.sources.SystemProperties;
import sh.props.tuples.Pair;
import sh.props.tuples.Tuple;

/**
 * Allows reading and deserializing {@link Source} configuration from an input stream.
 *
 * <p>The syntax for defining a list of sources in a configuration file is: <code>KEY=options</code>
 * . The key is used to identify the appropriate {@link SourceFactory} that can instantiate the
 * desired source. The configuration line is then passed to the factory object, allowing it to
 * appropriately parse it and construct the {@link Source}.
 */
public class SourceDeserializer {
  private final Map<String, SourceFactory<? extends Source>> deserializers;

  /**
   * Class constructor that initializes the known deserializers.
   *
   * @param deserializers the list of {@link SourceFactory} objects to register
   */
  protected SourceDeserializer(Map<String, SourceFactory<? extends Source>> deserializers) {
    this.deserializers = Collections.unmodifiableMap(deserializers);
  }

  /**
   * Finds a potential {@link Source} identifier in the specified line.
   *
   * @param line the line to process
   * @return an (id, options) pair
   */
  static Pair<String, String> processSourceConfig(String line) {
    line = line.trim();
    int pos = line.indexOf('=');
    if (pos == -1) {
      // the options part was not specified
      return Tuple.of(line.toLowerCase(), null);
    }

    // return the (id, options) pair
    return Tuple.of(line.substring(0, pos).toLowerCase(), line.substring(pos + 1));
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
  Source constructSource(String line) {
    // identify an implementation that can process this config line
    Pair<String, String> config = processSourceConfig(line);
    var factory = this.deserializers.get(config.first);
    if (factory == null) {
      // fail if the configuration file contains other artifacts
      // this is to prevent users making wrong assumptions about their registry configuration
      // e.g., assuming a key configuration file is there, when in fact it isn't
      throw new IllegalStateException(
          format("The specified config (%s) could not be mapped to a Source", line));
    }

    // construct a Source by parsing the specified line
    return factory.create(config.second);
  }

  /**
   * Reads {@link Source} configuration from the specified input stream and return an array of
   * sources, which can be passed to {@link RegistryBuilder#RegistryBuilder(Source...)}.
   *
   * @param stream the input stream representing the configuration data
   * @return an array of Source objects
   */
  public Source[] read(InputStream stream) {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, Charset.defaultCharset()))) {

      var sourceConfig = reader.lines().filter(not(String::isBlank)).collect(Collectors.toList());
      return read(sourceConfig);

    } catch (Exception e) {
      throw new IllegalStateException("Error encountered while reading source configuration", e);
    }
  }

  /**
   * Reads {@link Source} configuration from the specified input stream and return an array of
   * sources, which can be passed to {@link RegistryBuilder#RegistryBuilder(Source...)}.
   *
   * @param sourceConfig a list of source configurations
   * @return an array of Source objects
   * @throws IllegalStateException if the configuration line cannot be used to identify an *
   *     appropriate source
   */
  public Source[] read(List<String> sourceConfig) {
    return sourceConfig.stream().map(this::constructSource).toArray(Source[]::new);
  }

  /** Builder pattern for constructing {@link SourceDeserializer} objects. */
  public static class Builder {
    public static final String CLASSPATH_SOURCE = "classpath";
    public static final String ENV_SOURCE = "env";
    public static final String FILE_SOURCE = "file";
    public static final String SYSTEM_SOURCE = "system";

    final Map<String, SourceFactory<? extends Source>> deserializers = new HashMap<>();

    /**
     * Registers the default {@link SourceFactory} objects provided by props-core.
     *
     * @return this builder object (fluent interface)
     */
    public Builder withDefaults() {
      deserializers.put(CLASSPATH_SOURCE, new ClasspathPropertyFile.Factory());
      deserializers.put(ENV_SOURCE, new Environment.Factory());
      deserializers.put(FILE_SOURCE, new PropertyFile.Factory());
      deserializers.put(SYSTEM_SOURCE, new SystemProperties.Factory());
      return this;
    }

    /**
     * Allows external implementations to register additional sources, which will then be usable for
     * deserialization from a configuration file.
     *
     * @param key the key to register; must not have been previously bound
     * @param factory the {@link Source} this key should be instantiated to
     * @param <T> the type of the Source associated with the specified key
     * @return this builder object (fluent interface)
     */
    public <T extends Source> Builder withSource(String key, SourceFactory<T> factory) {
      assertNotNull(factory, "source factory");

      // register the source class and ensure the operation succeeded
      var prev = deserializers.putIfAbsent(key.toLowerCase(), factory);
      if (prev != null) {
        throw new IllegalArgumentException(
            key.toLowerCase() + " is already registered for " + prev.getClass().getSimpleName());
      }
      return this;
    }

    /**
     * Builds the source deserializer.
     *
     * @return an initialized {@link SourceDeserializer}
     */
    public SourceDeserializer build() {
      return new SourceDeserializer(deserializers);
    }
  }
}
