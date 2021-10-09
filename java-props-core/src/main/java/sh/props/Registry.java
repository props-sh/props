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
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;

public class Registry implements Notifiable {

  private static final Logger log = Logger.getLogger(Registry.class.getName());

  final Datastore store;
  final List<Layer> layers = new ArrayList<>();
  final ConcurrentHashMap<String, HashSet<Prop<?>>> notifications = new ConcurrentHashMap<>();

  /** Ensures a registry can only be constructed through a builder. */
  Registry() {
    this.store = new SyncStore(this);
  }

  @Override
  public void sendUpdate(String key, @Nullable String value, @Nullable Layer layer) {
    // check if we have any props to notify
    Collection<Prop<?>> props = this.notifications.get(key);
    if (props == null || props.isEmpty()) {
      // nothing to do if the key is not registered or there aren't any props to notify
      return;
    }

    // update all props
    for (Prop<?> prop : props) {
      // TODO: deal with validation errors (InvalidUpdateOpException)
      prop.setValue(value);
      if (log.isLoggable(Level.FINE)) {
        log.fine(() -> format("Prop %s updated by %s", prop.key(), layer));
      }
    }
  }

  /**
   * Binds the specified {@link Prop} to this registry. If the registry already has a value for this
   * prop, it will set it.
   *
   * @param prop the prop object to bind
   */
  public void bind(Prop<?> prop) {
    HashSet<Prop<?>> props =
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

    // log a warning message if more than one prop objects is bound for the same key
    // encourage good practices that lead to a performant implementation
    if (props.size() > 1 && log.isLoggable(Level.FINE)) {
      log.fine(
          () ->
              format(
                  "More than one Prop object bound for key %s; "
                      + "the performance of the updates will decrease linearly to the number of Prop objects "
                      + "bound per key. Consider reusing an existing object!",
                  prop.key()));
    }
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
