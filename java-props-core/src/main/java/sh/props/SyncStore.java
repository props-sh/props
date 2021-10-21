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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import sh.props.annotations.Nullable;

class SyncStore implements Datastore {

  protected final ConcurrentHashMap<String, ValueLayerTuple> effectiveValues =
      new ConcurrentHashMap<>();
  protected final Notifiable notifiable;

  public SyncStore(Notifiable notifiable) {
    this.notifiable = notifiable;
  }

  private static void assertLayerIsValid(Layer search, ValueLayerTuple vl) {
    if (!Objects.equals(search.registry(), vl.layer().registry())) {
      throw new IllegalArgumentException(
          "Invalid layer passed (does not belong to current registry)");
    }
  }

  /**
   * When an owner relinquishes control, find the first lower priority layer that defines the key.
   *
   * @param key the key we're searching for
   * @param vl the starting value/layer pair
   * @return a {@link ValueLayerTuple} if found, or <code>null</code> if no layer defines the key
   */
  @Nullable
  private static ValueLayerTuple findNextPotentialOwner(String key, ValueLayerTuple vl) {
    Layer l = vl.layer();
    // search all layers with lower priority for the key
    while (l.prev() != null) {
      l = l.prev();
      String value = l.get(key);
      if (value != null) {
        return new ValueLayerTuple(value, l);
      }
    }

    // if no layer defines this key
    return null;
  }

  /**
   * Find a specific layer that defines the key.
   *
   * @param key the key to search for
   * @param vl the effective value/layer pair
   * @param search the layer to search for
   * @return a {@link ValueLayerTuple} if a layer defines the key, or <code>null</code> otherwise
   */
  @Nullable
  private static ValueLayerTuple findLayer(String key, ValueLayerTuple vl, Layer search) {
    Layer l = vl.layer();

    SyncStore.assertLayerIsValid(search, vl);

    if (Objects.equals(search.id(), l.id())) {
      // we're searching for this key's owner
      return vl;
    }

    // find the desired layer
    while (l != null && l.priority() != search.priority()) {
      // if we found a mapping
      String value = l.get(key);
      if (value != null) {
        // return it
        return new ValueLayerTuple(value, l);
      }

      if (l.priority() < search.priority()) {
        // go to next layer
        l = l.next();
      } else if (l.priority() > search.priority()) {
        // go to previous layer
        l = l.prev();
      }
    }

    // if no layer defines this key
    return null;
  }

  @Override
  public Notifiable notifier() {
    return this.notifiable;
  }

  /**
   * Retrieves a value for the specified key.
   *
   * @param key the key to look for
   * @return a {@link ValueLayerTuple} if found, or <code>null</code>
   */
  @Override
  @Nullable
  public ValueLayerTuple get(String key) {
    return this.effectiveValues.get(key);
  }

  /**
   * Retrieves a value for the specified key, from the specified layer.
   *
   * @param key the key to look for
   * @param layer the layer to retrieve the value from
   * @return a {@link ValueLayerTuple} if found, or <code>null</code>
   */
  @Nullable
  public ValueLayerTuple get(String key, Layer layer) {
    ValueLayerTuple effective = this.effectiveValues.get(key);
    if (effective == null) {
      // no mapping exists in any layer
      return null;
    }

    return findLayer(key, effective, layer);
  }

  /**
   * Updates the value for the specified key.
   *
   * @param key the key to update
   * @param value the value to set (<code>null</code> unmaps the key)
   * @param layer the originating layer
   * @return a {@link ValueLayerTuple} representing the new owner, or <code>null</code> if no other
   *     layer defines the key
   */
  @Override
  @Nullable
  public ValueLayerTuple put(String key, @Nullable String value, Layer layer) {
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
            return this.onValueChanged(key, new ValueLayerTuple(value, layer));
          }

          // if we already defined a mapping for this key

          // first check the attempted mapping's priority
          int oldPriority = current.layer().priority();
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
          if (value != null && current.equalInValueOnly(value, layer)) {
            // we need to modify the layer/value mapping
            // but avoid sending an update to any objects following this key
            return new ValueLayerTuple(value, layer);
          }

          // if the owning layer unsets the value
          if (value == null) {
            // find a lower priority layer that defines this key
            return this.onValueChanged(key, findNextPotentialOwner(key, current));
          } else {
            // otherwise update the layer/value mapping
            return this.onValueChanged(key, new ValueLayerTuple(value, layer));
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
  private ValueLayerTuple onValueChanged(String key, @Nullable ValueLayerTuple vl) {
    if (vl == null) {
      this.notifier().sendUpdate(key, null, null);
      return null;
    }

    this.notifier().sendUpdate(key, vl.value(), vl.layer());
    return vl;
  }
}
