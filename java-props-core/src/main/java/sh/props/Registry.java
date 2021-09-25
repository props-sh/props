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

import java.util.Collections;
import java.util.List;
import sh.props.annotations.Nullable;
import sh.props.interfaces.Layer;
import sh.props.interfaces.ValueLayerTuple;

public class Registry extends SyncStore {

  List<LayerProxy> layers = Collections.emptyList();

  /** Ensures a registry can only be constructed through a builder. */
  Registry() {}

  /**
   * Used during initialization to add the layers used by this registry.
   *
   * @param layers layers to define
   */
  void setLayers(List<LayerProxy> layers) {
    this.layers = layers;
  }

  /**
   * Retrieves the value for the specified key.
   *
   * @param key the key to retrieve
   * @param clz a class object used to pass a type reference
   * @param <T> a type that we'll cast the return to
   * @return the effective value, or <code>null</code> if not found
   */
  @Nullable
  public <T> T get(String key, Class<T> clz) {
    // finds the value and owning layer
    ValueLayerTuple<String> valueLayer = this.get(key);

    // no value found, the key does not exist in the registry
    if (valueLayer == null) {
      return null;
    }

    // casts the effective value
    // TODO: this won't work in production due to effectiveValue always being a string
    //       and will require props v1's implementation
    return clz.cast(valueLayer.value());
  }

  @Override
  public void sendUpdate(String key, @Nullable String value, @Nullable Layer layer) {
    // TODO: implement
  }
}
