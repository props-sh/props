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

import static java.lang.String.format;
import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;

public class ClasspathPropertyFile implements Resolver {

  private static final Logger log = Logger.getLogger(ClasspathPropertyFile.class.getName());

  private final Map<String, String> store = new HashMap<>();
  private final String location;
  private final boolean isReloadable;

  /** Constructs a {@link Resolver} which should only read properties from the classpath once. */
  public ClasspathPropertyFile(String location) {
    this(location, false);
  }

  public ClasspathPropertyFile(String location, boolean isReloadable) {
    this.location = location;
    this.isReloadable = isReloadable;
  }

  @Override
  @Nullable
  public String get(String key) {
    return this.store.get(key);
  }

  @Override
  public Set<String> reload() {
    try (InputStream stream = this.getClass().getResourceAsStream(this.location)) {
      if (isNull(stream)) {
        log.fine(() -> format("Skipping %s; resource not found in classpath", this.location));
        return Set.of();
      }

      return ResolverUtils.mergeMapsInPlace(
          this.store, ResolverUtils.loadPropertiesFromStream(stream));
    } catch (IOException | IllegalArgumentException e) {
      log.log(
          Level.SEVERE,
          e,
          () -> format("Could not read properties from classpath: %s", this.location));
    }

    return Set.of();
  }

  @Override
  public boolean isReloadable() {
    return this.isReloadable;
  }

  @Override
  public String id() {
    return this.location;
  }
}
