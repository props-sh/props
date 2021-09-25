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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;
import sh.props.interfaces.Layer;
import sh.props.interfaces.Source;

/**
 * Implements the main logic that allows a {@link Registry} to retrieve effective values from
 * multiple {@link Source}s.
 *
 * <p>This class does not permit null values.
 */
class LayerProxy implements Layer<String> {

  private static final Logger log = Logger.getLogger(LayerProxy.class.getName());

  private final HashMap<String, String> store = new HashMap<>();
  private final Source source;
  final Registry registry;
  private final int priority;

  // tracks how many times the source's data was reloaded
  // we sacrifice correctness for speed and avoid using an AtomicLong
  // as we want to roughly understand the number of operations
  // and if any operations took place
  @SuppressWarnings("NonAtomicVolatileUpdate")
  private volatile long counterDataReloaded = 0;

  @Nullable LayerProxy prev;
  @Nullable LayerProxy next;

  LayerProxy(Source source, Registry registry, int priority) {
    this(source, registry, priority, true, Duration.ZERO);
  }

  LayerProxy(Source source, Registry registry, int priority, boolean lazy, Duration maxEagerWait) {
    this.source = source;
    this.priority = priority;
    this.registry = registry;

    // ensure that the layer will receive any updates from the specified source
    this.source.register(this::onReload);

    if (!lazy) {
      // eagerly load values from the associated source
      this.ensureSourceWasRead(maxEagerWait);
    }
  }

  // TODO: move all of this into another class
  private final AtomicBoolean isReadingSource = new AtomicBoolean(false);

  private void ensureSourceWasRead(Duration maxWait) {
    if (this.counterDataReloaded != 0) {
      // if the counter is not zero, the source was already read
      // nothing to do
      return;
    }

    boolean firstToTriggerRead = this.isReadingSource.compareAndSet(false, true);
    if (firstToTriggerRead) {
      try {
        // ensure the source is read, but avoid waiting indefinitely
        CompletableFuture.runAsync(() -> this.onReload(this.source.read()))
            .get(maxWait.toNanos(), TimeUnit.NANOSECONDS);
      } catch (InterruptedException | ExecutionException e) {
        log.log(
            Level.SEVERE,
            e,
            () -> format("Something went wrong while reading from source: %s", this.source.id()));
      } catch (TimeoutException e) {
        log.log(
            Level.SEVERE,
            e,
            () -> format("Timed out while waiting to read from source: %s", this.source.id()));
      } finally {
        // reset the flag
        this.isReadingSource.compareAndSet(true, false);
      }
    }
  }

  /**
   * Delegates to {@link Source#id()}.
   *
   * @return an unique identifier
   */
  @Override
  public final String id() {
    return this.source.id();
  }

  @Override
  @Nullable
  public Layer<String> next() {
    return this.next;
  }

  @Override
  @Nullable
  public Layer<String> prev() {
    return this.prev;
  }

  /**
   * Determines the priority of this layer in the current registry.
   *
   * @return the priority, higher values being more important
   */
  @Override
  public int priority() {
    return this.priority;
  }

  @Override
  public Registry registry() {
    return this.registry;
  }

  /**
   * Retrieves the value associated with a key.
   *
   * @param key the key to retrieve
   * @return a value, or <code>null</code> if not found
   */
  @Override
  @Nullable
  public String get(String key) {
    if (this.counterDataReloaded == 0) {
      // notify the user that they have read values too soon
      log.warning(
          () ->
              format(
                  "Key %s read attempted before source %s was read.  Please ensure sources can be read"
                      + "promptly, or initialize them with lazy=false",
                  key, this.source.id()));
    }

    return this.store.get(key);
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
        this.registry.put(existingKey, null, this);
        continue;
      }

      // retrieve the (potentially) new value
      String maybeNewValue = freshValues.get(existingKey);

      // check if the value has changed
      if (!Objects.equals(this.store.get(existingKey), maybeNewValue)) {
        // if it has changed, update it
        entry.setValue(maybeNewValue);

        // and notify that registry that we have a new value
        this.registry.put(existingKey, maybeNewValue, this);

        // then remove it from the new map
        // so that we end up with a map containing only new entries
        freshValues.remove(existingKey);
      }
    }

    // finally, add the new entries to the store
    this.store.putAll(freshValues);

    // and notify the registry of any keys that this layer now defines
    freshValues.forEach((key, value) -> this.registry.put(key, value, this));

    // count the number of times that the data was reloaded from the source
    this.incrementDataReloadedCounter();
  }

  // we do not care to be exact; see the explanation linked to the incremented field
  @SuppressWarnings("NonAtomicVolatileUpdate")
  private void incrementDataReloadedCounter() {
    this.counterDataReloaded++;
  }
}
