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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;
import sh.props.source.Source;
import sh.props.source.impl.ClasspathPropertyFile;
import sh.props.source.impl.Environment;
import sh.props.source.impl.InMemory;
import sh.props.source.impl.PropertyFile;
import sh.props.source.impl.SystemProperties;

/** Allows reading and deserializing {@link Source} configuration from an input stream. */
public class SourceDeserializer {
  private static final Logger log = Logger.getLogger(SourceDeserializer.class.getName());

  /**
   * Allows external implementations to register additional sources, which will then be usable for
   * deserialization from a configuration file.
   *
   * @param key the key to register; must not have been previously bound
   * @param sourceClass the {@link Source} this key should be instantiated to
   */
  public static void register(String key, Class<? extends Source> sourceClass) {
    if (sourceClass == null) {
      throw new IllegalArgumentException("Cannot register null class");
    }

    // register the source class and ensure the operation succeeded
    Class<? extends Source> prev = Holder.MAP.putIfAbsent(key, sourceClass);
    if (prev != null) {
      throw new IllegalArgumentException(
          key + " cannot be overwritten; it was previously registered with " + prev);
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

      return reader
          .lines()
          .filter(not(String::isBlank))
          .map(SourceDeserializer::constructSource)
          .filter(Objects::nonNull)
          .toArray(Source[]::new);

    } catch (Exception e) {
      throw new IllegalStateException("Error encountered while reading source configuration", e);
    }
  }

  /**
   * Helper method that attempts to construct a {@link Source} object by calling {@link
   * Source#from(String)} through Java reflection.
   *
   * @param line the configuration line to process
   * @return a constructed Source object
   */
  @Nullable
  static Source constructSource(String line) {
    // identify an implementation that can process this config line
    String id = findId(line);
    Class<? extends Source> sourceImpl = Holder.MAP.get(id);
    if (sourceImpl == null) {
      // do not fail when the configuration file contains other artifacts; log and skip
      log.warning(
          () ->
              format(
                  "The specified config (%s) could not be mapped to a Source. Will not add to configuration.",
                  line));
      return null;
    }

    try {
      // construct a Source by parsing the specified line
      Method from = sourceImpl.getMethod("from", String.class);
      return (Source) from.invoke(null, line);

    } catch (NoSuchMethodException
        | InvocationTargetException
        | IllegalArgumentException
        | IllegalAccessException e) {
      throw new IllegalStateException(
          "Source.from was not found through reflection. This should not happen and represents a developer error",
          e);
    }
  }

  /**
   * Finds a potential {@link Source} identifier in the specified line.
   *
   * @param line the line to process
   * @return the potential id
   */
  static String findId(String line) {
    line = line.trim();
    int pos = line.indexOf(':');
    if (pos == -1) {
      // return the line, as-is, since it doesn't contain any separators
      return line;
    }

    // return only the identifier
    return line.substring(0, pos);
  }

  /** Static holder, ensuring the map does not get initialized if this feature is not used. */
  private static class Holder {
    public static final Map<String, Class<? extends Source>> MAP = new HashMap<>();

    static {
      // define the core source implementations
      MAP.put(ClasspathPropertyFile.ID, ClasspathPropertyFile.class);
      MAP.put(Environment.ID, Environment.class);
      MAP.put(InMemory.ID, InMemory.class);
      MAP.put(PropertyFile.ID, PropertyFile.class);
      MAP.put(SystemProperties.ID, SystemProperties.class);
    }
  }
}
