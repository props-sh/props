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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import sh.props.annotations.Nullable;
import sh.props.source.Source;

/** Useful for tests, when the implementation requires overriding values. */
public class InMemory extends Source {
  public static final boolean UPDATE_REGISTRY_ON_EVERY_WRITE = true;
  public static final boolean UPDATE_REGISTRY_MANUALLY = false;
  public static final String ID = "memory";

  private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

  private final boolean updateOnEveryWrite;

  /**
   * Class constructor that defaults to not notifying subscribers on {@link #put(String, String)}s.
   *
   * <p>When you want to make a series of changes final, you will have to manually call {@link
   * #updateSubscribers()}.
   */
  public InMemory() {
    this(UPDATE_REGISTRY_MANUALLY);
  }

  /**
   * Class constructor.
   *
   * @param updateOnEveryWrite if true, {@link #updateSubscribers()} will be called on every put.
   */
  public InMemory(boolean updateOnEveryWrite) {
    this.updateOnEveryWrite = updateOnEveryWrite;
  }

  @Override
  public String id() {
    return ID;
  }

  /**
   * Retrieves an unmodifiable map containing all (key,value) pairs defined in the {@link #store}.
   *
   * @return a map
   */
  @Override
  public Map<String, String> get() {
    return Collections.unmodifiableMap(this.store);
  }

  /**
   * Overridden for performance reasons, to avoid creating the unmodifiable {@link #store} map.
   *
   * @param key the key to retrieve
   * @return a value, or <code>null</code> if the key was not found
   */
  @Nullable
  public String get(String key) {
    return this.store.get(key);
  }

  /**
   * Stores the specified (key, value) pair in memory.
   *
   * @param key the key to update
   * @param value the value to set; pass <code>null</code> if you want to remove the key,value
   *     mapping
   */
  public void put(String key, @Nullable String value) {
    try {
      if (value == null) {
        this.store.remove(key);
        return;
      }

      this.store.put(key, value);
    } finally {
      if (this.updateOnEveryWrite) {
        // notify subscribers automatically on every update
        this.updateSubscribers();
      }
    }
  }

  /**
   * Removes the specified key from memory.
   *
   * @param key the key to remove from the store
   */
  public void remove(String key) {
    this.put(key, null);
  }
}
