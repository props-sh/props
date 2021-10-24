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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;
import sh.props.converter.Cast;
import sh.props.converter.Converter;
import sh.props.interfaces.Prop;
import sh.props.tuples.WholePair;

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

  /**
   * Updates all registered props.
   *
   * @param props the props ot
   * @param value the value to set
   * @param layer the originating layer
   */
  private static void updateProps(
      Collection<AbstractProp<?>> props, @Nullable String value, @Nullable Layer layer) {
    for (AbstractProp<?> prop : props) {
      if (prop.setValue(value) && log.isLoggable(Level.FINE)) {
        log.fine(() -> format("%s received new value from %s", prop, layer));
      }
    }
  }

  @Override
  public void sendUpdate(String key, @Nullable String value, @Nullable Layer layer) {
    // check if we have any props to notify
    Collection<AbstractProp<?>> props = this.notifications.get(key);
    if (props == null || props.isEmpty()) {
      // nothing to do if the key is not registered or there aren't any props to notify
      return;
    }

    // alleviate the risk of blocking the main (update) thread
    // by offloading to an executor pool, since we don't control Prop subscribers
    ForkJoinPool.commonPool().execute(() -> Registry.updateProps(props, value, layer));
  }

  /**
   * Binds the specified {@link AbstractProp} to this registry. If the registry already has a value
   * for this prop, it will set it.
   *
   * <p>IMPORTANT: the update performance will decrease as the number of Prop objects increases.
   * Keep the implementation performant by reducing the number of Prop objects registered for the
   * same key.
   *
   * @param prop the prop object to bind
   * @param <T> the prop's type
   * @param <PropT> the class of the {@link Prop} with its upper bound ({@link AbstractProp})
   * @return the bound prop
   */
  public <T, PropT extends AbstractProp<T>> PropT bind(PropT prop) {
    this.notifications.compute(
        prop.key(),
        (s, current) -> {
          // initialize the list if not already present
          if (current == null) {
            current = new HashSet<>();
          }

          // bind the property and return the holder object
          if (current.add(prop)) {
            // if the prop was bound now attempt to set its value from the registry
            WholePair<String, Layer> vl = this.store.get(prop.key());
            if (vl != null) {
              // we currently have a value; set it
              prop.setValue(vl.first);
            }
          }

          return current;
        });
    return prop;
  }

  /**
   * Retrieves the value for the specified key.
   *
   * @param key the key to retrieve
   * @param converter the type converter used to cast the value to its appropriate type
   * @param <T> the return object's type
   * @return the effective value, or <code>null</code> if not found
   */
  @Nullable
  public <T> T get(String key, Converter<T> converter) {
    // finds the value and owning layer
    WholePair<String, Layer> valueLayer = this.store.get(key);

    // no value found, the key does not exist in the registry
    if (valueLayer == null) {
      return null;
    }

    // casts the effective value
    return converter.decode(valueLayer.first);
  }

  /**
   * Convenience method that retrieves the serialized value for the specified key.
   *
   * <p>This method is mostly useful in the following two cases:
   *
   * <ul>
   *   <li>- the property represented by the specified key is a <code>String</code>
   *   <li>- the calling code wants to deserialize the value using a different mechanism
   * </ul>
   *
   * @param key the key to retrieve
   * @return the effective value, or <code>null</code> if not found
   */
  @Nullable
  public String get(String key) {
    return this.get(key, Cast.asString());
  }
}
