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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import sh.props.annotations.Nullable;
import sh.props.source.PathBackedSource;
import sh.props.source.Source;
import sh.props.source.refresh.FileWatchSvc;
import sh.props.source.refresh.RefreshableSource;
import sh.props.source.refresh.Scheduler;

public class Layer implements Consumer<Map<String, String>> {

  private final Source source;
  private final Registry registry;
  private final HashMap<String, String> store = new HashMap<>();

  @Nullable Layer prev;
  @Nullable Layer next;

  private final int priority;

  /**
   * Class constructor.
   *
   * @param source the source that provides data for this layer
   * @param registry a reference to the associated registry
   */
  protected Layer(Source source, Registry registry, int priority) {
    this.source = source;
    this.registry = registry;
    this.priority = priority;

    // register the passed source for receiving updates
    source.register(this);
  }

  /**
   * Initializes the source, ensuring that it is read at least once, and that any scheduled
   * operations or watch services are tracking the source.
   */
  public void initialize() throws IOException {
    if (this.source.initialized()) {
      // the source is already initialized, nothing to do
      return;
    }

    // path-backed sources
    if (this.source instanceof PathBackedSource) {
      FileWatchSvc.instance().register((PathBackedSource) this.source);
      return;
    }

    // periodically refreshable sources
    if (this.source instanceof RefreshableSource) {
      // schedule it to be refreshed
      Scheduler.instance().schedule((RefreshableSource) this.source);
      return;
    }

    // for any other source types, ensure the data is read at least once
    this.accept(this.source.get());
  }

  @Nullable
  public Layer prev() {
    return this.prev;
  }

  @Nullable
  public Layer next() {
    return this.next;
  }

  /**
   * Delegates to {@link Source#id()}.
   *
   * @return the id of the underlying source
   */
  public String id() {
    return this.source.id();
  }

  @Nullable
  public String get(String key) {
    return this.store.get(key);
  }

  public int priority() {
    return this.priority;
  }

  public Registry registry() {
    return this.registry;
  }

  @Override
  public void accept(Map<String, String> data) {
    // TODO: interim implementation, refactor!
    this.store.clear();
    this.store.putAll(data);
  }

  // processes the reloaded data
  private void onReload(Map<String, String> data) {
    // make a copy to avoid wrongly changing the input
    // TODO: rethink this algorithm to avoid this copy op
    Map<String, String> freshValues = new HashMap<>(data);

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
        this.registry.store.put(existingKey, null, this);
        continue;
      }

      // retrieve the (potentially) new value
      String maybeNewValue = freshValues.get(existingKey);

      // check if the value has changed
      if (!Objects.equals(this.store.get(existingKey), maybeNewValue)) {
        // if it has changed, update it
        entry.setValue(maybeNewValue);

        // and notify that registry that we have a new value
        this.registry.store.put(existingKey, maybeNewValue, this);

        // then remove it from the new map
        // so that we end up with a map containing only new entries
        freshValues.remove(existingKey);
      }
    }

    // finally, add the new entries to the store
    this.store.putAll(freshValues);

    // and notify the registry of any keys that this layer now defines
    freshValues.forEach((key, value) -> this.registry.store.put(key, value, this));
  }

  @Override
  public String toString() {
    return String.format("Layer(id=%s, priority=%d)", this.id(), this.priority);
  }
}
