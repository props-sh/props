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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import sh.props.annotations.Nullable;
import sh.props.interfaces.Layer;
import sh.props.source.PathBackedSource;
import sh.props.source.RefreshableSource;
import sh.props.source.Source;
import sh.props.source.refresh.FileWatchSvc;
import sh.props.source.refresh.Scheduler;

public class LayerTwo implements Layer<String>, Consumer<Map<String, String>> {

  private final Source source;
  private final Registry registry;
  private final HashMap<String, String> store = new HashMap<>();

  @Nullable LayerTwo prev;
  @Nullable LayerTwo next;

  private final int priority;

  /**
   * Class constructor.
   *
   * @param source the source that provides data for this layer
   * @param registry a reference to the associated registry
   */
  protected LayerTwo(Source source, Registry registry, int priority) {
    this.source = source;
    this.registry = registry;
    this.priority = priority;

    // if the passed source is refreshable, register it for receiving updates
    if (this.source instanceof RefreshableSource) {
      ((RefreshableSource) source).register(this);
    }
  }

  /**
   * Initializes the source, ensuring that it is read at least once, and that any scheduled
   * operations or watch services are tracking the source.
   */
  public void initialize() throws IOException {
    if (this.source.initialized()) {
      // the source is already initialized, nothing to do
      return;
    }

    // path-backed sources
    if (this.source instanceof PathBackedSource) {
      FileWatchSvc.instance().register((PathBackedSource) this.source);
      return;
    }

    // periodically refreshable sources
    if (this.source instanceof RefreshableSource) {
      // schedule it to be refreshed
      Scheduler.instance().schedule((RefreshableSource) this.source);
      return;
    }

    // for any other source types, ensure the data is read at least once
    this.accept(this.source.get());
  }

  @Nullable
  @Override
  public Layer<String> next() {
    return this.next;
  }

  /**
   * Delegates to {@link Source#id()}.
   *
   * @return the id of the underlying source
   */
  @Override
  public String id() {
    return this.source.id();
  }

  @Nullable
  @Override
  public Layer<String> prev() {
    return this.prev;
  }

  @Nullable
  @Override
  public String get(String key) {
    return this.store.get(key);
  }

  @Override
  public int priority() {
    return this.priority;
  }

  @Override
  public Registry registry() {
    return this.registry;
  }

  @Override
  public void accept(Map<String, String> data) {
    // TODO: interim implementation, refactor!
    this.store.clear();
    this.store.putAll(data);
  }
}
