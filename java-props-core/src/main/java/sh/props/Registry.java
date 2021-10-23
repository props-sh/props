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
import java.util.concurrent.ForkJoinTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;

public class Registry implements Notifiable {

  private static final Logger log = Logger.getLogger(Registry.class.getName());

  final Datastore store;
  final List<Layer> layers = new ArrayList<>();
  final ConcurrentHashMap<String, HashSet<BaseProp<?>>> notifications = new ConcurrentHashMap<>();

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
  //
  private static void updateProps(
      Collection<BaseProp<?>> props, @Nullable String value, @Nullable Layer layer) {
    for (BaseProp<?> prop : props) {
      if (prop.setValue(value) && log.isLoggable(Level.FINE)) {
        log.fine(() -> format("%s received new value from %s", prop, layer));
      }
    }
  }

  @Override
  public void sendUpdate(String key, @Nullable String value, @Nullable Layer layer) {
    // check if we have any props to notify
    Collection<BaseProp<?>> props = this.notifications.get(key);
    if (props == null || props.isEmpty()) {
      // nothing to do if the key is not registered or there aren't any props to notify
      return;
    }

    // alleviate the risk of blocking the main (update) thread
    // by offloading to an executor pool, since we don't control Prop subscribers
    ForkJoinTask<?> task = ForkJoinTask.adapt(() -> Registry.updateProps(props, value, layer));
    ForkJoinPool.commonPool().execute(task);
  }

  /**
   * Binds the specified {@link BaseProp} to this registry. If the registry already has a value for
   * this prop, it will set it.
   *
   * <p>IMPORTANT: the update performance will decrease as the number of Prop objects increases.
   * Keep the implementation performant by reducing the number of Prop objects registered for the
   * same key.
   *
   * @param prop the prop object to bind
   * @return the bound prop
   */
  public <T> BaseProp<T> bind(BaseProp<T> prop) {
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
            ValueLayerTuple vl = this.store.get(prop.key());
            if (vl != null) {
              // we currently have a value; set it
              prop.setValue(vl.value());
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
   * @param clz a class object used to pass a type reference
   * @param <T> a type that we'll cast the return to
   * @return the effective value, or <code>null</code> if not found
   */
  @Nullable
  public <T> T get(String key, Class<T> clz) {
    // finds the value and owning layer
    ValueLayerTuple valueLayer = this.store.get(key);

    // no value found, the key does not exist in the registry
    if (valueLayer == null) {
      return null;
    }

    // casts the effective value
    // TODO: this won't work in production due to effectiveValue always being a string
    //       and will require props v1's implementation
    return clz.cast(valueLayer.value());
  }
}
