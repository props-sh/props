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

package sh.props.group;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import sh.props.Holder;
import sh.props.Prop;
import sh.props.annotations.Nullable;
import sh.props.tuples.Tuple;

@SuppressWarnings({"PMD.AvoidFieldNameMatchingMethodName"})
abstract class AbstractPropGroup<TupleT> extends Prop<TupleT> {
  protected final AtomicReference<Holder<TupleT>> holderRef;
  private final String key;

  /**
   * Accepts the reference to the holder of (value,error,epoch) triples.
   *
   * @param holderRef the holder ref
   */
  protected AbstractPropGroup(AtomicReference<Holder<TupleT>> holderRef, String key) {
    this.holderRef = holderRef;
    this.key = key;
  }

  /**
   * Helper function that concatenates the passed strings to generate a composite key. Each key part
   * is separated by the '∪' (union / U+222A) character.
   *
   * @param first a string representing the key's prefix
   * @param other optionally, any other keys to add to the key
   * @return a composite key made up by concatenating all the parts
   */
  protected static String multiKey(String first, @Nullable String... other) {
    if (other == null || other.length == 0) {
      // return only the first key, if no other parts were specified
      return first;
    }

    StringBuilder sb = new StringBuilder(first);
    for (String part : other) {
      sb.append("∪").append(part);
    }
    return sb.toString();
  }

  /**
   * Retrieves a {@link Tuple} of values. If any errors were encountered via the underlying {@link
   * Prop}s, this method will throw an exception. This method will continue to throw an exception
   * until all errors are cleared.
   *
   * @return a tuple, containing the current values
   * @throws RuntimeException in case any errors were set by any of the underlying props
   */
  @Override
  @SuppressWarnings("NullAway")
  public TupleT get() {
    return holderRef.get().value();
  }

  /**
   * Returns a key for this object. The key is generated using {@link #multiKey(String, String...)}.
   *
   * @return the key that identifies this prop group
   */
  @Override
  public String key() {
    return key;
  }

  /**
   * Helper method that applies updates to the underlying holder.
   *
   * @param ref an {@link AtomicReference} to the holder of values/errors
   * @param transformer the transforming {@link Function} that converts a holder's value from the
   *     previous state to the new state
   * @param subscriber a subscribing {@link java.util.function.BiConsumer} which accepts the updated
   *     (value, epoch) pair
   * @return the updated holder object
   */
  @SuppressWarnings("NullAway")
  protected Holder<TupleT> setValueState(
      AtomicReference<Holder<TupleT>> ref,
      Function<TupleT, TupleT> transformer,
      BiConsumer<TupleT, Long> subscriber) {
    var result = ref.updateAndGet(holder -> holder.value(transformer.apply(holder.value)));
    subscriber.accept(result.value, result.epoch);
    return result;
  }

  /**
   * Replaces a {@link Holder}'s value with an error state.
   *
   * @param throwable the error to set
   * @return the updated holder
   */
  protected Holder<TupleT> setErrorState(Throwable throwable) {
    var result = holderRef.updateAndGet(holder -> holder.error(throwable));
    this.onUpdateError(throwable, result.epoch);
    return result;
  }

  /**
   * Helper method that teads the prop's value or stores the thrown exception.
   *
   * @param prop the prop to load
   * @param errors the errors collector
   * @param <T> the type of the underlying prop
   * @return the read value, or null
   */
  @Nullable
  protected <T> T readVal(Prop<T> prop, List<Throwable> errors) {
    try {
      return prop.get();
    } catch (RuntimeException e) {
      errors.add(e);
      return null;
    }
  }
}
