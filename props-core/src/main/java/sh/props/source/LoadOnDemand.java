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

package sh.props.source;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A contract that allows {@link Source}s to partially load values and avoid retrieving the whole
 * list from the underlying store.
 *
 * <p>This interface is highly useful in cases where retrieving all {@link
 * sh.props.interfaces.Prop}s eagerly is costly (e.g., pay per API call, or large number of props
 * that may not be needed).
 */
public interface LoadOnDemand {

  /**
   * Signals if the underlying source support on-demand loading.
   *
   * @return <code>true</code> if the {@link Source} supports on-demand loading and the calling code
   *     should wait for each key to be processed or <code>false</code>, if the {@link Source} will
   *     load its values in bulk
   */
  default boolean loadOnDemand() {
    return false;
  }

  /**
   * Returns a completed future, which should complete when a value for the specified key was
   * retrieved.
   *
   * <p>The default implementation assumes that the key was already loaded. Implementing subclasses
   * should override this method and return a {@link CompletableFuture} that completes when the
   * value was loaded from the underlying store.
   *
   * <p>To avoid having to do a full refresh, classes which override this method should load the
   * value that corresponds to the given key and call {@link Source#sendLayerUpdate(Map)}, providing
   * the most up-to-date map of keys and values. If the implementation does not allow for keeping a
   * cache, this method should simply register the key and call {@link Source#refresh()}, thus
   * reloading all registered keys.
   *
   * <p>This method is not responsible for refreshing or reloading the value for the given key, it
   * merely signals the implementing {@link Source} that it might need to retrieve a value for the
   * given key.
   *
   * @param key the key of the Prop which was recently bound
   * @return a {@link CompletableFuture} that completes when the value is loaded
   */
  default CompletableFuture<String> registerKey(String key) {
    return CompletableFuture.completedFuture(null);
  }
}
