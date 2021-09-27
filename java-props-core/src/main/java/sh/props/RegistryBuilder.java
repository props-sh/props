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
import sh.props.source.PathBackedSource;
import sh.props.source.RefreshableSource;
import sh.props.source.Source;
import sh.props.source.refresh.FileWatchSvc;
import sh.props.source.refresh.Scheduler;

public class RegistryBuilder {

  ArrayDeque<Source> sources = new ArrayDeque<>();
  @Nullable FileWatchSvc fileWatchSvc = null;
  @Nullable Scheduler scheduler = null;

  /**
   * Call this method if the registry should watch for file changes.
   *
   * @return this builder object (fluent interface)
   * @throws IOException if the {@link FileWatchSvc} could not be initialized
   */
  public RegistryBuilder withFileWatchService() throws IOException {
    // the synchronized block is probably superfluous,
    // but nevertheless we want to prevent users from accidentally creating
    // more than one file watching service and its corresponding executor
    synchronized (this) {
      if (this.fileWatchSvc == null) {
        // initialize if necessary
        this.fileWatchSvc = new FileWatchSvc();
      }
    }

    return this;
  }

  /** Call this method if the registry should be allowed to periodically refresh its sources. */
  public RegistryBuilder withScheduler() {
    // the synchronized block is probably superfluous,
    // but nevertheless we want to prevent users from accidentally creating
    // more than one scheduler since we want to keep the number of threads to a minimum
    synchronized (this) {
      if (this.scheduler == null) {
        // initialize a scheduler if necessary
        this.scheduler = new Scheduler();
      }
    }

    return this;
  }

  /**
   * Registers a source with the current registry. The source will only be read once (eagerly) and
   * never refreshed.
   *
   * <p>Use one of {@link #withSource(PathBackedSource)} or {@link #withSource(RefreshableSource)}
   * if you'd like to refresh the source's data.
   *
   * @param source the source to register
   * @return this builder object (fluent interface)
   */
  public RegistryBuilder withSource(Source source) {
    this.sources.addFirst(source);
    return this;
  }

  /**
   * Registers a source which will be periodically refreshed.
   *
   * <p>If {@link #withScheduler()} was not already called, it will be automatically called.
   *
   * @param source the source to auto-refresh
   * @return this builder object (fluent interface)
   */
  public RegistryBuilder withSource(RefreshableSource source) {
    this.sources.addFirst(source);

    // initialize a scheduler if necessary
    if (this.scheduler == null) {
      this.withScheduler();
    }

    if (this.scheduler == null) {
      throw new IllegalStateException(
          "Something went seriously wrong and the scheduler could not be initialized");
    }

    // schedule the source for periodic refreshing
    this.scheduler.schedule(source);

    return this;
  }

  /**
   * Registers a source with the current registry. If a {@link FileWatchSvc} is initialized, the
   * source will be refreshed on any disk events (file created, modified, or deleted).
   *
   * @param source the source to register
   * @return this builder object (fluent interface)
   */
  public RegistryBuilder withSource(PathBackedSource source) throws IOException {
    this.sources.addFirst(source);

    // ensure a file watch service is initialized
    if (this.fileWatchSvc == null) {
      this.withFileWatchService();
    }

    if (this.fileWatchSvc == null) {
      throw new IllegalStateException(
          "Something went seriously wrong and the file watch service could not be initialized");
    }

    // schedule the source for on disk updates
    this.fileWatchSvc.register(source);

    return this;
  }

  /**
   * Builds a registry object, given the sources that were already added.
   *
   * @return a configured {@link Registry} object
   */
  public Registry build(Datastore<String> store) {
    Registry registry = new Registry(store);

    int priority = this.sources.size();
    @Nullable LayerProxy next = null;

    List<LayerProxy> layers = new ArrayList<>(this.sources.size());
    for (Source source : this.sources) {
      // wrap each source in a layer
      LayerProxy layer = new LayerProxy(source, registry, priority--);

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

    // ensure the file watch service is running
    if (this.fileWatchSvc != null) {
      this.fileWatchSvc.schedule();
    }

    // and return a constructed registry object
    return registry;
  }
}
