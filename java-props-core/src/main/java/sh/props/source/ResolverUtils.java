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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.logging.Level.SEVERE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResolverUtils {

  private static final Logger log = Logger.getLogger(ResolverUtils.class.getName());
  private static final Pattern configLinePattern =
      Pattern.compile(
          "^(?<type>[a-z]+)(?:=(?<path>[^,]+)(?:,(?<reload>(true|false)))?)?$",
          Pattern.CASE_INSENSITIVE);

  /**
   * Loads a {@link Properties} object from the passed {@link InputStream} and returns a {@link Map}
   * containing all key->value mappings.
   *
   * @throws IllegalArgumentException if a null <code>InputStream</code> was passed
   * @throws IOException if the <code>InputStream</code> cannot be read
   */
  public static Map<String, String> loadPropertiesFromStream(InputStream stream)
      throws IOException {
    if (isNull(stream)) {
      throw new IllegalArgumentException(
          "loadPropertiesFromStream expects a non-null input stream");
    }

    Properties properties = new Properties();
    properties.load(stream);
    return readPropertiesToMap(properties);
  }

  /**
   * Iterates over all the input {@link Properties} and returns a {@link Map} containing all
   * key->value mappings.
   */
  private static Map<String, String> readPropertiesToMap(Properties properties) {
    Map<String, String> store = new HashMap<>();
    for (String key : properties.stringPropertyNames()) {
      store.put(key, properties.getProperty(key));
    }
    return store;
  }

  /**
   * Merges the <code>collector</code> and <code>updated</code> maps by.
   * <li/>- deleting any keys which are no longer defined in <code>updated</code>
   * <li/>- updating any keys whose values have changed in <code>updated</code>
   * <li/>- setting any new keys whose values have been added in <code>updated</code>
   *
   * @return the {@link Set} of new, updated, and deleted keys
   */
  public static Set<String> mergeMapsInPlace(
      Map<String, String> collector, Map<String, String> updated) {
    var toDel = new HashSet<String>();

    // if the key doesn't exist in the updated set, mark it for deletion
    for (Entry<String, String> val : collector.entrySet()) {
      if (!updated.containsKey(val.getKey())) {
        toDel.add(val.getKey());
      }
    }

    // delete all keys which do not appear in the updated list
    for (String key : toDel) {
      collector.remove(key);
    }

    // set all updated values
    for (Entry<String, String> newVal : updated.entrySet()) {
      if (!Objects.equals(collector.get(newVal.getKey()), newVal.getValue())) {
        collector.put(newVal.getKey(), newVal.getValue());
        // store each updated key into the same 'toDel' set, to avoid creating a new object
        toDel.add(newVal.getKey());
      }
    }

    // return all deleted, new, and updated keys
    return toDel;
  }

  /**
   * Reads all lines from an {@link InputStream} that specifies multiple resolver configurations.
   *
   * @return a list of {@link Resolver}s
   */
  public static List<Resolver> readResolverConfig(InputStream stream) {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, Charset.defaultCharset()))) {
      return reader.lines().map(ResolverUtils::readConfigLine).collect(Collectors.toList());

    } catch (Exception e) {
      log.log(SEVERE, e, () -> "Could not read resolver configuration");
      return List.of();
    }
  }

  /**
   * Parses a config line and instantiates a resolver.
   *
   * @throws IllegalArgumentException if the line is invalid, or an unknown <code>type</code> was
   *     specified
   */
  static Resolver readConfigLine(String line) {
    Matcher matcher = configLinePattern.matcher(line);

    if (!matcher.matches()) {
      throw new IllegalArgumentException("Cannot read config line, syntax incorrect: " + line);
    }

    String type = matcher.group("type");
    if (nonNull(type)) {
      type = type.toLowerCase();
    }
    String path = matcher.group("path");
    boolean reload = Boolean.parseBoolean(matcher.group("reload"));

    if (Objects.equals(type, "file")) {
      return new PropertyFile(Paths.get(path), reload);
    } else if (Objects.equals(type, "classpath")) {
      return new ClasspathPropertyFile(path, reload);
    } else if (Objects.equals(type, "system")) {
      return new SystemProperty();
    } else if (Objects.equals(type, "env")) {
      return new Environment();
    } else {
      throw new IllegalArgumentException("Did not recognize " + type + " in: " + line);
    }
  }
}
