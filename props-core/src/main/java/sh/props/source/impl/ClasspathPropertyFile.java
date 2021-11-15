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

package sh.props.source.impl;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.source.Source;
import sh.props.source.SourceFactory;

/** Retrieves properties from a Java properties file, located on the classpath. */
public class ClasspathPropertyFile extends Source {
  public static final String ID = "classpath";
  private static final Logger log = Logger.getLogger(ClasspathPropertyFile.class.getName());
  private final String location;

  /**
   * Constructs a {@link Source} which reads values from a property file in the classpath.
   *
   * @param location the classpath location of the required properties resource
   */
  public ClasspathPropertyFile(String location) {
    this.location = location;
  }

  @Override
  public String id() {
    return ID + "://" + this.location;
  }

  @Override
  public Map<String, String> get() {
    try (InputStream stream = this.getClass().getResourceAsStream(this.location)) {
      if (stream != null) {
        return Source.loadPropertiesFromStream(stream);
      }

      // the stream could not be read
      log.log(Level.WARNING, () -> format("Could not find in classpath: %s", this.location));

    } catch (IOException | IllegalArgumentException e) {
      log.log(
          Level.WARNING,
          e,
          () -> format("Could not read properties from classpath: %s", this.location));
    }

    return Collections.emptyMap();
  }

  /** Factory implementation. */
  public static class Factory implements SourceFactory<ClasspathPropertyFile> {

    /**
     * Initializes a {@link ClasspathPropertyFile} object from the specified id.
     *
     * @param id the identifier representing this source
     * @return a constructed Source object
     */
    @Override
    public ClasspathPropertyFile create(String id) {
      @SuppressWarnings("StringSplitter")
      String[] parts = id.split("=");
      if (parts.length != 2 || !ClasspathPropertyFile.ID.equals(parts[0])) {
        throw new IllegalArgumentException("Invalid id '" + id + "' for the current class " + this);
      }

      return new ClasspathPropertyFile(parts[1]);
    }
  }
}
