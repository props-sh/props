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

import static java.lang.String.format;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;
import sh.props.source.AbstractSource;
import sh.props.source.Schedulable;
import sh.props.source.Source;

public class Layer implements Consumer<Map<String, String>> {

  private static final Logger log = Logger.getLogger(Layer.class.getName());

  private final AbstractSource source;
  private final Registry registry;
  private final HashMap<String, String> store = new HashMap<>();

  private final ReentrantLock lock = new ReentrantLock();

  @Nullable
  Layer prev;
  @Nullable
  Layer next;

  private final int priority;
  private volatile boolean initialized = false;

  /**
   * Class constructor.
   *
   * @param source   the source that provides data for this layer
   * @param registry a reference to the associated registry
   */
  protected Layer(AbstractSource source, Registry registry, int priority) {
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
    if (this.initialized) {
      // the layer is already initialized, nothing to do
      return;
    }

    if (this.source instanceof Schedulable && ((Schedulable) this.source).scheduled()) {
      // nothing to do as the source was scheduled elsewhere
      // this layer will receive the next update
      this.initialized = true;
      return;
    }

    // if this source is schedulable, but was not already scheduled
    if (this.source instanceof Schedulable) {
      // log a warning since this may be a user error
      log.warning(
          () ->
              format(
                  "The '%s' custom source is schedulable but was not already scheduled. "
                      + "If this is an error, override #scheduled() and return true",
                  this.source));

    } else {
      // for any other source types, ensure the data is read at least once
      this.accept(this.source.get());
      this.initialized = true;
    }
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
    // disallow more than one concurrent update from taking place
    if (!this.lock.tryLock()) {
      log.warning(() -> "Could not update layer while another update is taking place");
    }

    try {
      // iterate over the current values
      var it = this.store.entrySet().iterator(); // 2 4
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
      this.lock.unlock();
    }
  }

  @Override
  public String toString() {
    return format("Layer(id=%s, priority=%d)", this.id(), this.priority);
  }
}
