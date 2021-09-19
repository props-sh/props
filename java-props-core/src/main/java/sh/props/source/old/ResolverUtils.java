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

package sh.props.source.old;

import static java.util.logging.Level.SEVERE;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import sh.props.source.ClasspathPropertyFile;
import sh.props.source.Environment;
import sh.props.source.PropertyFile;
import sh.props.source.Source;
import sh.props.source.SystemProperties;

public class ResolverUtils {

  private static final Logger log = Logger.getLogger(ResolverUtils.class.getName());
  private static final Pattern configLinePattern =
      Pattern.compile("^(?<type>[a-z]+)(?:://(?<path>[^,]+))?$", Pattern.CASE_INSENSITIVE);

  /**
   * Reads all lines from an {@link InputStream} that specifies multiple sources.
   *
   * @return a list of {@link Source}s
   */
  public static List<Source> readResolverConfig(InputStream stream) {
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
  static Source readConfigLine(String line) {
    Matcher matcher = configLinePattern.matcher(line);

    if (!matcher.matches()) {
      throw new IllegalArgumentException("Cannot read config line, syntax incorrect: " + line);
    }

    String type = matcher.group("type");
    String path = matcher.group("path");

    if ("file".equalsIgnoreCase(type)) {
      return new PropertyFile(Paths.get(path));
    } else if ("classpath".equalsIgnoreCase(type)) {
      return new ClasspathPropertyFile(path);
    } else if ("system".equalsIgnoreCase(type)) {
      return new SystemProperties();
    } else if ("env".equalsIgnoreCase(type)) {
      return new Environment();
    } else {
      throw new IllegalArgumentException("Unknown type " + type + " defined at: " + line);
    }
  }
}
