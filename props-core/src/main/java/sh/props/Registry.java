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
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;
import sh.props.converter.Cast;
import sh.props.converter.Converter;
import sh.props.interfaces.Prop;
import sh.props.tuples.Pair;

public class Registry implements Notifiable {
  private static final Logger log = Logger.getLogger(Registry.class.getName());

  final Datastore store;
  final List<Layer> layers = new ArrayList<>();
  final ConcurrentHashMap<String, HashSet<AbstractProp<?>>> notifications =
      new ConcurrentHashMap<>();

  /** Ensures a registry can only be constructed through a builder. */
  Registry() {
    this.store = new SyncStore(this);
  }

  @Override
  public void sendUpdate(String key, @Nullable String value, @Nullable Layer layer) {
    assertNotNull(key, "key");

    // check if we have any props to notify
    Collection<AbstractProp<?>> props = this.notifications.get(key);
    if (props == null || props.isEmpty()) {
      // nothing to do if the key is not registered or there aren't any props to notify
      return;
    }

    // alleviate the risk of blocking the main (update) thread
    // by offloading to an executor pool, since we don't control Prop subscribers
    ForkJoinPool.commonPool()
        .execute(
            () -> {
              for (AbstractProp<?> prop : props) {
                prop.setValue(value);
              }
            });
  }

  /**
   * Binds the specified {@link AbstractProp} to this registry. If the registry already contains a
   * value for this prop, it will call {@link AbstractProp#setValue(String)} to set it.
   *
   * <p>If any of the configured {@link sh.props.source.Source}s override {@link
   * sh.props.source.LoadOnDemand} and signal that they support on-demand loading of keys, their
   * corresponding {@link sh.props.source.LoadOnDemand#loadOnDemand()} method will be called, but
   * the implementation will not wait for a value to be retrieved. The logic of dealing with the
   * asynchronous nature of receiving each value is left to the implementing Source.
   *
   * <p>NOTE: In the default implementation, none of the classes extending {@link AbstractProp}
   * override {@link Object#equals(Object)} and {@link Object#hashCode()}. This ensures that
   * multiple props with the same {@link Prop#key()} to be bound to the same Registry.
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
   * @param <PropT> the class of the {@link Prop} with its upper bound ({@link AbstractProp})
   * @return the bound prop
   * @throws IllegalArgumentException if a previously bound prop is passed
   */
  public <T, PropT extends AbstractProp<T>> PropT bind(PropT prop) {
    final String key = prop.key();

    // notify all layers that the specified prop was bound
    // but do not wait for the key to be loaded
    for (Layer layer : layers) {
      @SuppressWarnings({"FutureReturnValueIgnored", "UnusedVariable"})
      var future = layer.registerKey(key);
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
   * <p>Since this method retrieves the value directly from the underlying {@link Datastore}, it is
   * the fastest way to observe a changed value.
   *
   * <p>It differs from any bound {@link Prop} objects in that they will have to wait for {@link
   * Registry#sendUpdate(String, String, Layer)} to asynchronously finish executing before observing
   * any changes.
   *
   * @param key the key to retrieve
   * @param converter the type converter used to cast the value to its appropriate type
   * @param <T> the return object's type
   * @return the effective value, or <code>null</code> if not found
   */
  @Nullable
  public <T> T get(String key, Converter<T> converter) {
    assertNotNull(key, "key");
    assertNotNull(converter, "converter");

    // notify layers that a key is being retrieved
    try {
      // and wait until the request is processed by all sources
      CompletableFuture.allOf(
              layers.stream()
                  .filter(Layer::loadOnDemand)
                  .map(layer -> layer.registerKey(key))
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

    // finds the value and owning layer
    var valueLayer = this.store.get(key);
    return getFromValueLayer(valueLayer, converter);
  }

  /**
   * Retrieves a value for the specified key, from the specified layer.
   *
   * @param key the key to look for
   * @param converter the type converter used to cast the value to its appropriate type
   * @param alias the alias of the {@link sh.props.source.Source} from which the value should be
   *     retrieved; if <code>null</code> is passed {@link #get(String, Converter)} will be called
   *     instead
   * @return a {@link Pair} containing the value and defining layer if found, or <code>null
   *     </code>
   */
  @Nullable
  public <T> T get(String key, Converter<T> converter, String alias) {
    assertNotNull(key, "key");
    assertNotNull(converter, "converter");

    // if no alias was provided, search all layers
    if (alias == null) {
      return get(key, converter);
    }

    // find the matching layer (by alias)
    var target = layers.stream().filter(l -> Objects.equals(l.alias, alias)).findFirst();
    if (target.isEmpty()) {
      // no matching layer was found
      return null;
    }

    String rawValue = target.get().get(key);
    if (rawValue == null) {
      // key was not found
      return null;
    }

    return converter.decode(rawValue);
  }

  /**
   * Returns the value, converted to the appropriate type, if a value exists in the specified layer.
   *
   * @param <T> the type of the returned type
   * @param valueLayer the value,layer pair from which the value should be extracted
   * @param converter the type converter used to cast the value to its appropriate type
   * @return a value cast to the appropriate type, or null if not found
   */
  @Nullable
  private <T> T getFromValueLayer(
      @Nullable Pair<String, Layer> valueLayer, Converter<T> converter) {
    // no value found, the key does not exist in the registry
    // or the value is null
    if (valueLayer == null || valueLayer.first == null) {
      return null;
    }

    // casts the effective value
    return converter.decode(valueLayer.first);
  }

  /**
   * Ensures that the specified param is not null.
   *
   * @param key the key to validate
   * @param <T> the type of the key
   * @return the specified key
   */
  private <T> T assertNotNull(T key, String param) {
    if (key == null) {
      throw new IllegalArgumentException(format("%s cannot be null", param));
    }

    return key;
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
