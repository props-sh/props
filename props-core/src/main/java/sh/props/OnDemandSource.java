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

import static java.lang.String.format;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;

/** Standard implementation of a {@link Source} capable of loading keys on-demand. */
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public abstract class OnDemandSource extends Source {
  private static final Logger log = Logger.getLogger(OnDemandSource.class.getName());

  private final Set<String> keys = ConcurrentHashMap.newKeySet();
  private final Map<String, String> cache = new ConcurrentHashMap<>();

  /**
   * Loads the value for the given key.
   *
   * @param key a key that this source should retrieve
   * @return the current value represented by the specified key
   */
  @Nullable
  protected abstract String loadKey(String key);

  /**
   * Retrieves the list of keys which should be refreshed on-demand. Implementing subclasses are
   * strongly encouraged to only retrieve these keys from the underlying store, but ultimately this
   * is not enforced.
   *
   * @return a {@link Set} containing only the keys that were explicitly requested by a {@link
   *     sh.props.Registry}
   */
  protected Set<String> getRegisteredKeys() {
    return Collections.unmodifiableSet(keys);
  }

  /**
   * Retrieves all keys registered with {@link #registerKey(String)} from the underlying store.
   *
   * @return a map containing the most recent
   */
  @Override
  public Map<String, String> get() {
    // refresh all registered keys
    try {
      // and wait until the request is processed by all sources
      CompletableFuture.allOf(
              getRegisteredKeys().stream()
                  .map(this::retrieveKeyAsync)
                  .toArray(CompletableFuture[]::new))
          .join();
    } catch (CompletionException e) {
      log.log(Level.WARNING, e, () -> "Could not retrieve all keys, see previous log lines");
    }

    // and return the cached results
    return Collections.unmodifiableMap(cache);
  }

  /**
   * Registers the key for refreshing and loads its initial value.
   *
   * @param key the key of the Prop which was recently bound
   * @return a {@link CompletableFuture} that will eventually resolve to the key's current value
   */
  @Override
  public CompletableFuture<String> registerKey(String key) {
    // register the key
    keys.add(key);

    // and retrieve its value, updating the underlying Layer on completion
    return retrieveKeyAsync(key)
        .whenComplete(
            (ignored, err) -> {
              // if the value could not be retrieved
              if (err != null) {
                // log an error
                logKeyUpdateErr(key, err);
                return;
              }

              sendLayerUpdate(Collections.unmodifiableMap(this.cache));
            });
  }

  /**
   * Helper method that logs errors seen during key updates.
   *
   * @param key the referenced key
   * @param err the encountered error
   */
  private void logKeyUpdateErr(String key, Throwable err) {
    log.log(Level.WARNING, err, () -> format("Unexpected error retrieving %s from %s", key, this));
  }

  /**
   * Retrieves the value corresponding to the specified key, asynchronously.
   *
   * @param key the key to retrieve
   * @return a {@link CompletableFuture} that completes when the corresponding value is retrieved;
   *     if the value can be retrieved, it will also be cached
   */
  private CompletableFuture<String> retrieveKeyAsync(String key) {
    return CompletableFuture.supplyAsync(() -> loadKey(key)).whenComplete(cacheKeyValueResult(key));
  }

  /**
   * Returns a {@link BiConsumer} that can cache the resulting key,value pair. If a {@link
   * Throwable} is provided, it will be logged and the value will be ignored.
   *
   * @param key they key to which the value points
   * @return a {@link BiConsumer} that caches the result or logs the error
   */
  private BiConsumer<String, Throwable> cacheKeyValueResult(String key) {
    return (value, err) -> {
      // if the value could not be retrieved
      if (err != null) {
        logKeyUpdateErr(key, err);
        return;
      }

      // if a value was found
      if (value != null) {
        // cache it
        cache.put(key, value);
      } else {
        // otherwise, delete it
        cache.remove(key);
      }
    };
  }

  /**
   * Signals that this implementation loads keys when bound to and requested by a {@link
   * sh.props.Registry}.
   *
   * @return true
   */
  @Override
  public boolean loadOnDemand() {
    return true;
  }
}
