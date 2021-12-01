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

import static sh.props.Registry.assertNotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import sh.props.annotations.Nullable;
import sh.props.source.Source;
import sh.props.tuples.Pair;
import sh.props.tuples.Tuple;

public class RegistryBuilder {

  ArrayDeque<Pair<Source, String>> sources = new ArrayDeque<>();

  /**
   * Class constructor that accepts sources that the {@link Registry} will use for retrieving
   * values.
   *
   * <p>This is a shorthand for quickly creating a Registry in cases where the client code does not
   * care about retrieving values directly from a Source, in a reproducible way (same alias in
   * subsequent JVM runs). If the implementation does require that feature, it can use {@link
   * #withSource(Source, String)} to also provide an alias for each source.
   *
   * @param sources the source or sources to add to the registry
   */
  public RegistryBuilder(Source... sources) {
    // be less restrictive here and avoid throwing an exception
    if (sources != null) {
      // if any sources are specified, add them
      for (Source source : sources) {
        withSource(source);
      }
    }
  }

  /**
   * Registers a source with the current registry, without specifying an alias.
   *
   * @param source the source to register
   * @return this builder object (fluent interface)
   */
  public RegistryBuilder withSource(Source source) {
    this.sources.addFirst(Tuple.of(source, null));
    return this;
  }

  /**
   * Registers a source with the current registry.
   *
   * @param source the source to register
   * @param alias a reference that can be used to retrieve values directly from this source; if
   *     <code>null</code> is provided, it will result in the source being identified by its {@link
   *     Source#id()}
   * @return this builder object (fluent interface)
   */
  public RegistryBuilder withSource(Source source, @Nullable String alias) {
    this.sources.addFirst(Tuple.of(source, alias));
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
    for (var pair : this.sources) {
      Source source = assertNotNull(pair.first, "source");
      String alias = pair.second;

      // wrap each source in a layer
      Layer layer = new Layer(source, alias, registry, priority--);

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
