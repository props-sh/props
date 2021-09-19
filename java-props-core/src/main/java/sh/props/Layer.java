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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import sh.props.annotations.Nullable;
import sh.props.source.Source;

class Layer {

  private final HashMap<String, String> store = new HashMap<>();
  private final Registry registry;
  private final int priority;
  private final Source source;
  @Nullable Layer prev;
  @Nullable Layer next;

  Layer(Source source, Registry registry, int priority) {
    // reads from source
    this.source = source;
    // TODO: lazy or async load
    this.onReload(source.read());

    this.registry = registry;
    this.priority = priority;
  }

  /**
   * Delegates to {@link Source#id()}
   *
   * @return
   */
  public String id() {
    return this.source.id();
  }

  // decides the priority of this layer, in the current registry
  public int priority() {
    return this.priority;
  }

  // retrieves the value for the specified key, from this layer alone
  @Nullable
  String get(String key) {
    return this.store.get(key);
  }

  boolean containsKey(String key) {
    return this.store.containsKey(key);
  }

  // processes the reloaded data
  private void onReload(Map<String, String> freshValues) {
    // iterate over the current values
    var it = this.store.entrySet().iterator();
    while (it.hasNext()) {
      var entry = it.next();
      var existingKey = entry.getKey();

      // check if the updated map still contains this key
      if (!freshValues.containsKey(existingKey)) {
        // the key was deleted
        // remove the entry from the underlying store
        it.remove();

        // and notify the registry that this layer no longer defines this key
        this.registry.unbindKey(existingKey, this);
        continue;
      }

      // retrieve the (potentially) new value
      String maybeNewValue = freshValues.get(existingKey);

      // check if the value has changed
      if (!Objects.equals(this.store.get(existingKey), maybeNewValue)) {
        // if it has changed, update it
        entry.setValue(maybeNewValue);

        // and notify that registry that we have a new value
        this.registry.updateKey(existingKey, this);

        // then remove it from the new map
        // so that we end up with a map containing only new entries
        freshValues.remove(existingKey);
      }
    }

    // finally, add the new entries to the store
    this.store.putAll(freshValues);

    // and notify the registry of any keys that this layer now defines
    freshValues.keySet().forEach(key -> this.registry.bindKey(key, this));
  }
}
