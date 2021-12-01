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

package sh.props;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import sh.props.annotations.Nullable;
import sh.props.source.Source;

/**
 * Wraps a {@link Source} and holds the logic for prioritizing which Source will return the
 * effective value for each requested key.
 */
public class Layer implements Consumer<Map<String, String>> {

  protected final String alias;
  final Source source;
  private final Registry registry;
  private final HashMap<String, String> store = new HashMap<>();

  private final ReentrantLock lock = new ReentrantLock();
  private final int priority;
  private final AtomicBoolean initialized = new AtomicBoolean(false);
  @Nullable Layer prev;
  @Nullable Layer next;

  /**
   * Class constructor.
   *
   * @param source the source that provides data for this layer
   * @param alias a unique reference that can identify the source in a {@link Registry}; if <code>
   *     null</code> is provided, the implementation will use {@link Source#id()} as an alias
   * @param registry a reference to the associated registry
   * @param priority the priority of this layer
   */
  protected Layer(Source source, @Nullable String alias, Registry registry, int priority) {
    this.source = source;
    this.alias = alias != null ? alias : source.id();
    this.registry = registry;
    this.priority = priority;

    // register the passed source for receiving updates
    source.register(this);
  }

  /**
   * Initializes the source, ensuring that it is read at least once, and that any scheduled
   * operations or watch services are tracking the source.
   *
   * @return the current layer (fluent interface)
   */
  Layer initialize() {
    if (this.initialized.get()) {
      // the layer is already initialized, nothing to do
      return this;
    }

    // otherwise, update current subscribers (e.g. the current layer)
    this.source.refresh();

    return this;
  }

  /**
   * References the previous layer in logical order.
   *
   * @return a valid object, or null if the current layer is the last one
   */
  @Nullable
  public Layer prev() {
    return this.prev;
  }

  /**
   * References the next layer in logical order.
   *
   * @return a valid object, or null if the current layer is the last one
   */
  @Nullable
  public Layer next() {
    return this.next;
  }

  /**
   * Retrieves the current value of the Prop identified by the specified key.
   *
   * @param key the key to retrieve
   * @return a value if one exists, or <code>null</code>
   */
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
    // disallow more than one concurrent update from taking place
    // TODO(mihaibojin): this lock has the potential to create a lot of contention, especially if
    //                   using `OnDemandSource`s which rely on CompletableFutures to process
    //                   updates, upon a new key registration (LoadOnDemand#registerKey)
    this.lock.lock();
    try {
      // iterate over the current values
      var it = this.store.entrySet().iterator();
      while (it.hasNext()) {
        var entry = it.next();
        var existingKey = entry.getKey();

        // check if the updated map still contains this key
        if (!data.containsKey(existingKey)) {
          // the key was deleted
          // remove the entry from the underlying store
          it.remove();

          // and notify the registry that this layer no longer defines this key
          this.registry.store.put(existingKey, null, this);
          continue;
        }

        // retrieve the (potentially) new value
        String maybeNewValue = data.get(existingKey);

        // check if the value has changed
        if (!Objects.equals(this.store.get(existingKey), maybeNewValue)) {
          // if it has changed, update it
          entry.setValue(maybeNewValue);

          // and notify that registry that we have a new value
          this.registry.store.put(existingKey, maybeNewValue, this);
        }
      }

      // process incoming data
      for (var e : data.entrySet()) {
        // if the specific key was not already processed (i.e., existed in the store, before
        // this method call
        if (!this.store.containsKey(e.getKey())) {
          // add the new key,value to the store
          this.store.put(e.getKey(), e.getValue());
          // and notify the registry
          this.registry.store.put(e.getKey(), e.getValue(), this);
        }
      }
    } finally {
      // unlock to allow further updates from proceeding
      this.lock.unlock();
      // and mark this layer as initialized, if not already so
      this.initialized.compareAndSet(false, true);
    }
  }

  @Override
  public String toString() {
    return format("Layer(%s, alias=%s, priority=%d)", this.source.id(), this.alias, this.priority);
  }
}
