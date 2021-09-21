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

public abstract class LayerOwnership {

  protected final ConcurrentHashMap<String, Layer> owners = new ConcurrentHashMap<>();

  // registers ownership of a layer over a key
  void bindKey(String key, Layer layer) {
    // finds the current owner
    Layer owner = this.owners.get(key);

    // determines if ownership change is required
    if (owner == null || owner.priority() < layer.priority()) {
      // change the current owner
      this.owners.put(key, layer);
      // TODO: notify subscribers of update
    }
  }

  // unregisters ownership of a layer over a key
  void unbindKey(String key, String oldValue, Layer layer) {
    // finds the current owner
    Layer owner = this.owners.get(key);

    // determines if ownership change is required
    if (owner != null && owner.equals(layer)) {
      // change the current owner
      while (layer.prev != null) {
        layer = layer.prev;

        Pair result = layer.keyIsMapped(key);
        if (result != null) {
          // update the owner
          this.owners.put(key, layer);

          // determine if the value has changed
          if (!Objects.equals(result.value, oldValue)) {
            // TODO: notify subscribers of update
          }
          return;
        }
      }

      // we could not replace the owner
      // it means this key is not defined in any layer
      this.owners.remove(key);
      // TODO: notify subscribers of update
    }

    // nothing to do, ownership did not change
  }

  void updateKey(String key, Layer layer) {
    // finds the current owner
    Layer owner = this.owners.get(key);

    // determines if the specified layer owns the key
    if (Objects.equals(owner, layer)) {
      // TODO: notify subscribers of update
    }
  }
}
