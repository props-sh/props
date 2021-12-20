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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;
import sh.props.converters.Cast;
import sh.props.converters.Converter;
import sh.props.tuples.Pair;

public class Registry {
  private static final Logger log = Logger.getLogger(Registry.class.getName());

  final RegistryStore store;
  final List<Layer> layers = new ArrayList<>();
  final ConcurrentHashMap<String, HashSet<BoundableProp<?>>> notifications =
      new ConcurrentHashMap<>();
  private final Scheduler scheduler;

  /** Ensures a registry can only be constructed through a builder. */
  Registry(Scheduler scheduler) {
    this.store = new RegistryStoreImpl(this);
    this.scheduler = scheduler;
  }

  /**
   * Sends a value update to any bound props.
   *
   * @param key the key representing the updated prop
   * @param valueLayer the value layer pair representing the update
   */
  void sendUpdate(String key, Pair<String, Layer> valueLayer) {
    Utilities.assertNotNull(key, "key");

    // check if we have any props to notify
    Collection<BoundableProp<?>> props = this.notifications.get(key);
    if (props == null || props.isEmpty()) {
      // nothing to do if the key is not registered or there aren't any props to notify
      return;
    }

    // alleviate the risk of blocking the main (update) thread
    // by offloading to an executor pool, since we don't control Prop subscribers
    scheduler.forkJoinExecute(() -> props.forEach(prop -> prop.setValue(valueLayer.first)));
  }

  /**
   * Binds the specified {@link BoundableProp} to this registry. If the registry already contains a
   * value for this prop, it will call {@link BoundableProp#setValue(String)} to set it.
   *
   * <p>If any of the configured {@link Source}s override {@link LoadOnDemand} and signal that they
   * support on-demand loading of keys, their corresponding {@link LoadOnDemand#loadOnDemand()}
   * method will be called, but the implementation will not wait for a value to be retrieved. The
   * logic of dealing with the asynchronous nature of receiving each value is left to the
   * implementing Source.
   *
   * <p>NOTE: In the default implementation, none of the classes extending {@link BoundableProp}
   * override {@link Object#equals(Object)} and {@link Object#hashCode()}. This ensures that
   * multiple props with the same {@link BoundableProp#key()} to be bound to the same Registry.
   *
   * <p>IMPORTANT: the update performance will decrease as the number of Prop objects increases.
   * Keep the implementation performant by reducing the number of Prop objects registered for the
   * same key!
   *
   * <p>NOTE 2: There is nothing stopping a caller from binding the same Prop object to multiple
   * Registries. This is an experimental and untested feature that may be developed in the future,
   * or depending on feedback might be disallowed. Mileage may vary, use at your own risk! ;)
   *
   * @param prop the prop object to bind
   * @param <T> the prop's type
   * @param <PropT> the prop's type with an upper bound of ({@link BoundableProp})
   * @return the bound prop
   * @throws IllegalArgumentException if a previously bound prop is passed
   */
  public <T, PropT extends BoundableProp<T>> PropT bind(PropT prop) {
    final String key = prop.key();

    // notify all layers that the specified prop was bound
    // but do not wait for the key to be loaded
    for (Layer layer : layers) {
      @SuppressWarnings({"FutureReturnValueIgnored", "UnusedVariable", "PMD.UnusedLocalVariable"})
      var future = layer.source.registerKey(key);
    }

    // and compute the prop's initial value
    this.notifications.compute(
        key,
        (s, current) -> {
          // initialize the list if not already present
          if (current == null) {
            current = new HashSet<>();
          }

          // bind the property and return the holder object
          if (!current.add(prop)) {
            throw new IllegalArgumentException(
                "Props can only be bound once to the same Registry object!");
          }

          // after the prop was bound, attempt to set its value from the registry
          Pair<String, Layer> vl = this.store.get(key);
          if (vl != null) {
            // we currently have a value; set it
            prop.setValue(vl.first);
          }

          return current;
        });

    // replace the prop's scheduler with this registry's scheduler
    if (prop.scheduler == null) {
      // only do this operation once (for the first registry that gets to bind this prop)
      prop.scheduler = this.scheduler;
    }

    // return the prop back to the caller
    return prop;
  }

  /**
   * Convenience method that retrieves the string representation of the value associated with the
   * given key. If the underlying value is not actually a {@link String}, this method will
   * effectively call {@link Converter#encode(Object)}, which by default is implemented as {@link
   * Object#toString()}.
   *
   * @param key the key to retrieve
   * @return the effective value, or <code>null</code> if not found
   */
  @Nullable
  public String get(String key) {
    return this.get(key, Cast.asString());
  }

  /**
   * Convenience method that retrieves the value associated with the given key.
   *
   * <p>Since this method retrieves the value directly from the underlying {@link RegistryStore}, it
   * is the fastest way to observe a changed value.
   *
   * <p>It differs from any bound {@link BoundableProp} objects in that they will have to wait for
   * {@link Registry#sendUpdate(String, Pair)} to asynchronously finish executing before observing
   * any changes.
   *
   * @param key the key to retrieve
   * @param converter the type converter used to cast the value to its appropriate type
   * @param <T> the return object's type
   * @return the effective value, or <code>null</code> if not found
   */
  @Nullable
  public <T> T get(String key, Converter<T> converter) {
    Utilities.assertNotNull(key, "key");
    Utilities.assertNotNull(converter, "converter");

    waitForOnDemandValuesToBePopulated(key);

    // finds the value and owning layer
    var valueLayer = this.store.get(key);
    return maybeConvertValue(() -> valueLayer != null ? valueLayer.first : null, converter);
  }

  /**
   * Retrieves a value for the specified key, from the specified layer.
   *
   * @param key the key to look for
   * @param converter the type converter used to cast the value to its appropriate type
   * @param alias the alias of the {@link Source} from which the value should be retrieved; if
   *     <code>null</code> is passed {@link #get(String, Converter)} will be called instead
   * @param <T> the type of the returned type
   * @return a {@link Pair} containing the value and defining layer if found, or <code>null
   *     </code>
   */
  @Nullable
  public <T> T get(String key, Converter<T> converter, String alias) {
    Utilities.assertNotNull(key, "key");
    Utilities.assertNotNull(converter, "converter");

    // if no alias was provided, search all layers
    if (alias == null) {
      return get(key, converter);
    }

    // find the matching layer (by alias)
    var target = layers.stream().filter(l -> Objects.equals(l.alias, alias)).findFirst();

    // return the value corresponding to the desired key from the Layer
    // converted to the appropriate type
    return maybeConvertValue(() -> target.map(l -> l.get(key)).orElse(null), converter);
  }

  /**
   * Ensures all layers representing {@link OnDemandSource}s have a chance of retrieving the value
   * for the specified key.
   *
   * @param key the key to retrieve
   */
  private void waitForOnDemandValuesToBePopulated(String key) {
    try {
      // wait until the request is processed by all sources
      CompletableFuture.allOf(
              layers.stream()
                  .filter(layer -> layer.source.loadOnDemand())
                  .map(layer -> layer.source.registerKey(key))
                  .toArray(CompletableFuture[]::new))
          .join();
    } catch (CompletionException e) {
      log.log(
          Level.WARNING,
          e,
          () ->
              format(
                  "At least one layer failed to retrieve a value for %s; the returned value may be incorrect",
                  key));
    }
  }

  /**
   * Returns the value, converted to the appropriate type, if a value exists.
   *
   * @param <T> the type of the returned type
   * @param valueSupplier supplies a value to be converted
   * @param converter the type converter used to cast the value to its appropriate type
   * @return a value cast to the appropriate type, or null if not found
   */
  @Nullable
  private <T> T maybeConvertValue(Supplier<String> valueSupplier, Converter<T> converter) {
    // no value found in the registry
    String rawValue = valueSupplier.get();
    if (rawValue == null) {
      return null;
    }

    return converter.decode(rawValue);
  }

  /**
   * Initializes a {@link CustomPropBuilder} which can be used to initialize custom Props, without
   * the user needing to extend {@link CustomProp}.
   *
   * @param converter the type converter used to cast the created custom Props
   * @param <T> the type of the Props that will be built by the returned builder
   * @return a builder object
   */
  public <T> CustomPropBuilder<T> builder(Converter<T> converter) {
    return new CustomPropBuilder<>(this, converter);
  }
}
