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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import sh.props.annotations.Nullable;
import sh.props.interfaces.Datastore;
import sh.props.source.AbstractSource;
import sh.props.source.refresh.FileWatchSvc;
import sh.props.source.refresh.FileWatchableSource;
import sh.props.source.refresh.Refreshable;
import sh.props.source.refresh.RefreshableSource;
import sh.props.source.refresh.Scheduler;

public class RegistryBuilder {

  ArrayDeque<AbstractSource> sources = new ArrayDeque<>();

  /**
   * Registers a source with the current registry. The source will only be read once (eagerly) and
   * never refreshed.
   *
   * <p>Use one of {@link #withSource(FileWatchableSource)} or {@link
   * #withSource(RefreshableSource)} if you'd like to refresh the source's data.
   *
   * @param source the source to register
   * @return this builder object (fluent interface)
   */
  public RegistryBuilder withSource(AbstractSource source) {
    this.sources.addFirst(source);
    return this;
  }

  /**
   * Registers a source which will be periodically refreshed.
   *
   * <p>If the source was not already {@link Refreshable#scheduled()}, it will be
   * on the default {@link Scheduler#instance()}.
   *
   * @param source the source to auto-refresh
   * @return this builder object (fluent interface)
   */
  public RegistryBuilder withSource(RefreshableSource source) {
    this.sources.addFirst(source);

    if (!source.scheduled()) {
      // schedule the source for periodic refreshing
      Scheduler.instance().schedule(source);
    }

    return this;
  }

  /**
   * Registers a source with the current registry.
   *
   * <p>If the source was not already {@link Refreshable#scheduled()}, it will be
   * registered to the default {@link FileWatchSvc#instance()}.
   *
   * @param source the source to register
   * @return this builder object (fluent interface)
   */
  public RegistryBuilder withSource(FileWatchableSource source) throws IOException {
    this.sources.addFirst(source);

    if (!source.scheduled()) {
      // schedule the source for on disk updates
      FileWatchSvc.instance().register(source);
    }

    return this;
  }

  /**
   * Builds a registry object, given the sources that were already added.
   *
   * @return a configured {@link Registry} object
   */
  public Registry build(Datastore store) {
    Registry registry = new Registry(store);

    int priority = this.sources.size();
    @Nullable Layer next = null;

    List<Layer> layers = new ArrayList<>(this.sources.size());
    for (AbstractSource source : this.sources) {
      // wrap each source in a layer
      Layer layer = new Layer(source, registry, priority--);

      // link the previous and current layer
      layer.next = next;
      if (next != null) {
        next.prev = layer;
      }
      next = layer;

      // store the layer in an array list
      layers.add(layer);
    }

    // add the layers to the registry
    registry.setLayers(layers);
    
    // and return a constructed registry object
    return registry;
  }
}
