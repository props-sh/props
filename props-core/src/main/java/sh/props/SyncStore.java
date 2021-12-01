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

import static sh.props.util.Validate.assertNotNull;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import sh.props.annotations.Nullable;
import sh.props.tuples.Pair;
import sh.props.tuples.Tuple;

/**
 * Internal implementation for holding the effective values coming from all layers.
 *
 * <p>This class is subject to future change, if for example we decide to add a different algorithm
 * for deciding the effective value from a set of layers.
 */
class SyncStore implements Datastore {

  protected final ConcurrentHashMap<String, Pair<String, Layer>> effectiveValues =
      new ConcurrentHashMap<>();
  protected final Notifiable notifiable;

  public SyncStore(Notifiable notifiable) {
    this.notifiable = notifiable;
  }

  /**
   * When an owner relinquishes control, find the first lower priority layer that defines the key.
   *
   * @param key the key we're searching for
   * @param vl the starting value/layer pair
   * @return a {@link Pair} if found, or <code>null</code> if no layer defines the key
   */
  @Nullable
  private static Pair<String, Layer> findNextPotentialOwner(String key, Pair<String, Layer> vl) {
    Layer l = assertNotNull(vl.second, "layer");

    // search all layers with lower priority for the key
    while (l.prev() != null) {
      l = l.prev();
      String value = l.get(key);
      if (value != null) {
        return Tuple.of(value, l);
      }
    }

    // if no layer defines this key
    return null;
  }

  /**
   * Checks if the specified pair's value equals the deconstructed value, but NOT the layer.
   *
   * @param value the value to compare
   * @param layer the owning layer to compare
   * @return true if only the value is equal
   */
  private static boolean equalInValueButNotLayer(
      Pair<String, Layer> pair, String value, Layer layer) {
    return Objects.equals(value, pair.first) && layer != pair.second;
  }

  @Override
  public Notifiable notifier() {
    return this.notifiable;
  }

  /**
   * Retrieves a value for the specified key.
   *
   * @param key the key to look for
   * @return a {@link Pair} containing the value and defining layer if found, or <code>null
   *     </code>
   */
  @Override
  @Nullable
  public Pair<String, Layer> get(String key) {
    return this.effectiveValues.get(key);
  }

  /**
   * Updates the value for the specified key.
   *
   * @param key the key to update
   * @param value the value to set (<code>null</code> unmaps the key)
   * @param layer the originating layer
   * @return a {@link Pair} containing the value and new owning layer, or <code>null</code> if no
   *     other layer defines the key
   */
  @Override
  @Nullable
  public Pair<String, Layer> put(String key, @Nullable String value, Layer layer) {
    return this.effectiveValues.compute(
        key,
        (k, current) -> {
          if (current == null) {
            // if we had no mapping for this key

            // and if the new value is null
            if (value == null) {
              // do not map the key
              return null;
            }

            // otherwise, map the effective value and its owning layer
            return this.onValueChanged(key, Tuple.of(value, layer));
          }

          // if we already defined a mapping for this key

          // first check the attempted mapping's priority
          int oldPriority = assertNotNull(current.second, "layer").priority();
          int priority = layer.priority();
          // if the layer's priority is lower than the current owner
          if (priority < oldPriority) {
            // do nothing as this layer does not hold ownership over the key
            return current;
          }

          // from this point, we're guaranteed that the attempted mapping's priority
          // is at least equal to the current mapping's priority

          // if a higher priority layer unsets the value
          if (value == null && priority > oldPriority) {
            // this is a no-op
            // this could for example happen if a layer maps and unmaps a key
            // during the same update interval, sending only the unmap event
            return current;
          }

          // if the value has not changed (note, we do not allow null value mappings)
          if (value != null && current.equalTo(value, layer)) {
            // simply return the current value
            return current;
          }

          // if the layer mapping is updated but the value did not change
          if (value != null && equalInValueButNotLayer(current, value, layer)) {
            // we need to modify the layer/value mapping
            // but avoid sending an update to any objects following this key
            return Tuple.of(value, layer);
          }

          // if the owning layer unsets the value
          if (value == null) {
            // find a lower priority layer that defines this key
            return this.onValueChanged(key, findNextPotentialOwner(key, current));
          } else {
            // otherwise update the layer/value mapping
            return this.onValueChanged(key, Tuple.of(value, layer));
          }
        });
  }

  /**
   * Updates any subscribers of a value change event.
   *
   * @param key the key that was updated
   * @param vl the value,layer pair
   * @return the same value,layer pair
   */
  @Nullable
  private Pair<String, Layer> onValueChanged(String key, @Nullable Pair<String, Layer> vl) {
    if (vl == null) {
      this.notifier().sendUpdate(key, null, null);
      return null;
    }

    this.notifier().sendUpdate(key, vl.first, vl.second);
    return vl;
  }
}
