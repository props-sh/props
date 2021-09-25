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

import static java.lang.String.format;
import static java.util.logging.Level.SEVERE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/** Retrieves properties from a Java properties file, located on disk. */
public class PropertyFile extends AbstractSource {

  private static final Logger log = Logger.getLogger(PropertyFile.class.getName());
  private final Path location;

  @Override
  public String id() {
    return "file://" + this.location.toString();
  }

  /**
   * Constructs a file-based {@link Source}.
   *
   * @param location the path, on disk, of the property file
   */
  public PropertyFile(Path location) {
    this.location = location;
  }

  @Override
  public Map<String, String> get() {
    try (InputStream stream = Files.newInputStream(this.location)) {
      return this.loadPropertiesFromStream(stream);

    } catch (IOException | IllegalArgumentException e) {
      log.log(SEVERE, e, () -> format("Could not read properties from: %s", this.location));
    }

    return Collections.emptyMap();
  }
}
