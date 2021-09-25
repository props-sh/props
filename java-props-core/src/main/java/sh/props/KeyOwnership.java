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
import sh.props.Layer.Pair;
import sh.props.annotations.Nullable;

public abstract class KeyOwnership {

  protected final ConcurrentHashMap<String, ValueLayer> effectiveValues = new ConcurrentHashMap<>();

  public abstract void sendUpdate(String key, @Nullable String value, @Nullable Layer layer);

  /**
   * Retrieves a value for the specified key.
   *
   * @param key the key to look for
   * @return a {@link ValueLayer} pair if found, or <code>null</code>
   */
  @Nullable
  public ValueLayer get(String key) {
    return this.effectiveValues.get(key);
  }

  /**
   * Retrieves a value for the specified key, from the specified layer.
   *
   * @param key the key to look for
   * @param layer the layer to retrieve the value from
   * @return a {@link ValueLayer} pair if found, or <code>null</code>
   */
  @Nullable
  public ValueLayer get(String key, Layer layer) {
    ValueLayer effective = this.effectiveValues.get(key);
    if (effective == null) {
      // no mapping exists in any layer
      return null;
    }

    return findLayer(key, effective, layer);
  }

  private static void assertLayerIsValid(Layer search, ValueLayer vl) {
    if (!Objects.equals(search.registry, vl.layer.registry)) {
      throw new IllegalArgumentException(
          "Invalid layer passed (does not belong to current registry)");
    }
  }

  /**
   * Updates the value for the specified key.
   *
   * @param key the key to update
   * @param value the value to set (<code>null</code> unmaps the key)
   * @param layer the originating layer
   * @return a {@link ValueLayer} pair representing the new owner, or <code>null</code> if no other
   *     layer defines the key
   */
  public ValueLayer put(String key, @Nullable String value, Layer layer) {
    return this.effectiveValues.compute(
        key,
        (k, current) -> {
          if (current == null) {
            // if we had no previous value for this key

            // and if the new value is null
            if (value == null) {
              // do not map the key
              return null;
            }

            // otherwise map the effective value and its owning layer
            try {
              return new ValueLayer(value, layer);
            } finally {
              // and send an update to any objects following this key
              this.sendUpdate(key, value, layer);
            }
          }

          // if nothing has changed
          if (current.equalTo(value, layer)) {
            // simply return the current value
            return current;
          }

          // if the layer's priority is lower than the current owner
          int oldPriority = current.layer.priority();
          int priority = layer.priority();
          if (priority < oldPriority) {
            // do nothing as this layer does not hold ownership over the key
            return current;
          }

          // if the value has not changed
          if (Objects.equals(current.value, value)) {
            // we need to modify the layer/value mapping
            // but not send an update to any objects following this key
            return new ValueLayer(value, layer);
          }

          ValueLayer ret;

          // but if the value has changed

          if (value == null && priority == oldPriority) {
            // it was either unset from the owning layer
            // in which case we need to find a lower priority layer that defines this key
            ret = findNextPotentialOwner(key, current);

          } else if (value == null) {
            // it was unset in a higher priority layer
            // in which case, this is a no-op
            return current;

          } else {
            // or it was legitimately updated
            // and we need to modify the layer/value mapping
            ret = new ValueLayer(value, layer);
          }

          try {
            return ret;
          } finally {
            // and send an update to any objects following this key
            this.sendUpdate(key, ret != null ? ret.value : null, layer);
          }
        });
  }

  /**
   * When an owner relinquishes control, find the first lower priority layer that defines the key.
   *
   * @param key the key we're searching for
   * @param vl the starting value/layer pair
   * @return a {@link ValueLayer} pair if found, or <code>null</code> if no layer defines the key
   */
  @Nullable
  private static ValueLayer findNextPotentialOwner(String key, ValueLayer vl) {
    Layer l = vl.layer;
    // search all layers with lower priority for the key
    while (l.prev != null) {
      l = l.prev;
      Pair pair = l.keyIsMapped(key);
      if (pair != null && pair.value != null) {
        return new ValueLayer(pair.value, l);
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
   * @return a {@link ValueLayer} pair if a layer defines the key, or <code>null</code> otherwise
   */
  @Nullable
  private static ValueLayer findLayer(String key, ValueLayer vl, Layer search) {
    Layer l = vl.layer;

    KeyOwnership.assertLayerIsValid(search, vl);

    if (Objects.equals(search.id(), l.id())) {
      // we're searching for this key's owner
      return vl;
    }

    // find the desired layer
    while (l != null && l.priority() != search.priority()) {
      // if we found a mapping
      Pair pair = l.keyIsMapped(key);
      if (pair != null) {
        // return it
        return new ValueLayer(pair.value, l);
      }

      if (l.priority() < search.priority()) {
        // go to next layer
        l = l.next;
      } else if (l.priority() > search.priority()) {
        // go to previous layer
        l = l.prev;
      }
    }

    // if no layer defines this key
    return null;
  }

  static class ValueLayer {

    // TODO: remove the annotation
    @Nullable final String value;
    final Layer layer;

    ValueLayer(@Nullable String value, Layer layer) {
      this.value = value;
      this.layer = layer;
    }

    boolean equalTo(@Nullable String value, Layer layer) {
      return Objects.equals(value, this.value) && Objects.equals(layer, this.layer);
    }
  }
}
