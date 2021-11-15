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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import sh.props.annotations.Nullable;
import sh.props.source.Source;

public class RegistryBuilder {

  ArrayDeque<Source> sources = new ArrayDeque<>();

  /**
   * Class constructor that accepts sources that the {@link Registry} will use for retrieving
   * values.
   *
   * @param sources the source or sources to add to the registry
   */
  public RegistryBuilder(Source... sources) {
    // be less restrictive here and avoid throwing an exception
    if (sources != null) {
      // if any sources are specified, add them
      for (Source source : sources) {
        this.sources.addFirst(source);
      }
    }
  }

  /**
   * Registers a source with the current registry.
   *
   * @param source the source to register
   * @return this builder object (fluent interface)
   */
  public RegistryBuilder withSource(Source source) {
    this.sources.addFirst(source);
    return this;
  }

  /**
   * Builds a registry object, given the sources that were already added.
   *
   * @return a configured {@link Registry} object
   */
  public Registry build() {
    Registry registry = new Registry();

    // validate sources
    if (this.sources.isEmpty()) {
      throw new IllegalStateException("Cannot construct registry without any sources");
    }

    int priority = this.sources.size();
    @Nullable Layer next = null;

    List<Layer> layers = new ArrayList<>(this.sources.size());
    for (Source source : this.sources) {
      // wrap each source in a layer
      Layer layer = new Layer(source, registry, priority--);

      // link the previous and current layer
      layer.next = next;
      if (next != null) {
        next.prev = layer;
      }
      next = layer;

      // store the layer in an array list
      layers.add(layer.initialize());
    }

    // add the layers to the registry
    registry.layers.addAll(layers);

    // and return a constructed registry object
    return registry;
  }
}
