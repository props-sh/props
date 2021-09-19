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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;

/** Useful for tests, when the implementation requires overriding values. */
public class InMemory implements Source {

  private static final Logger log = Logger.getLogger(InMemory.class.getName());

  private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

  @Override
  public String id() {
    return "memory";
  }

  /**
   * Retrieves an unmodifiable map containing all (key,value) pairs defined in the {@link #store}.
   *
   * @return a map
   */
  @Override
  public Map<String, String> read() {
    return Collections.unmodifiableMap(this.store);
  }

  /**
   * Overridden for performance reasons, to avoid making the {@link #store} unmodifiable.
   *
   * @param key the key to retrieve
   * @return a value, or <code>null</code> if the key was not found
   */
  @Override
  @Nullable
  public String get(String key) {
    return this.store.get(key);
  }

  /** Stores the specified (key, value) pair in memory. */
  public void put(String key, String value) {
    this.store.put(key, value);
  }

  @Override
  public void register(Consumer<Map<String, String>> downstream) {
    log.warning("onUpdate(...) not implemented, updates will not be sent downstream");
  }
}
