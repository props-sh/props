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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import sh.props.AbstractProp;
import sh.props.Holder;
import sh.props.SubscribableProp;
import sh.props.annotations.Nullable;
import sh.props.exceptions.InvalidReadOpException;
import sh.props.interfaces.Prop;
import sh.props.tuples.Tuple;

public class AnotherQuintupleSyncdPropGroup<T, U, V, W, X>
    extends SubscribableProp<Tuple<T, U, V, W, X>> {
  private final AtomicReference<Holder<Tuple<T, U, V, W, X>>> cached =
      new AtomicReference<>(new Holder<>());
  private final String key;
  private final AbstractProp<T> first;
  private final AbstractProp<U> second;
  private final AbstractProp<V> third;
  private final AbstractProp<W> fourth;
  private final AbstractProp<X> fifth;

  /**
   * Constructs a synchronized quintuple of values.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param fourth the fourth prop
   * @param fifth the fifth prop
   */
  public AnotherQuintupleSyncdPropGroup(
      AbstractProp<T> first,
      AbstractProp<U> second,
      AbstractProp<V> third,
      AbstractProp<W> fourth,
      AbstractProp<X> fifth) {
    this.first = first;
    this.second = second;
    this.third = third;
    this.fourth = fourth;
    this.fifth = fifth;
    key =
        AbstractPropGroup.multiKey(
            first.key(), second.key(), third.key(), fourth.key(), fifth.key());

    // guarantee that holder.value is not null
    readValues();

    // register for prop updates
    first.subscribe(
        v -> updateValue(cached, t -> t.updateFirst(v), this::onValueUpdate), this::setError);
    second.subscribe(
        v -> updateValue(cached, t -> t.updateSecond(v), this::onValueUpdate), this::setError);
    third.subscribe(
        v -> updateValue(cached, t -> t.updateThird(v), this::onValueUpdate), this::setError);
    fourth.subscribe(
        v -> updateValue(cached, t -> t.updateFourth(v), this::onValueUpdate), this::setError);
    fifth.subscribe(
        v -> updateValue(cached, t -> t.updateFifth(v), this::onValueUpdate), this::setError);
  }

  /**
   * Ensures thrown exceptions are unchecked.
   *
   * @param t the exception to be thrown
   * @return the object cast as {@link RuntimeException} or a new exception, wrapping the passed
   *     throwable
   */
  private static RuntimeException ensureUnchecked(Throwable t) {
    if (t instanceof RuntimeException) {
      return (RuntimeException) t;
    }

    return new RuntimeException(t);
  }

  @SuppressWarnings("NullAway")
  private static <T, U, V, W, X> Holder<Tuple<T, U, V, W, X>> updateValue(
      AtomicReference<Holder<Tuple<T, U, V, W, X>>> ref,
      Function<Tuple<T, U, V, W, X>, Tuple<T, U, V, W, X>> transformer,
      BiConsumer<Tuple<T, U, V, W, X>, Long> subscriber) {
    var result = ref.updateAndGet(holder -> holder.value(transformer.apply(holder.value)));
    subscriber.accept(result.value, result.epoch);
    return result;
  }

  private Holder<Tuple<T, U, V, W, X>> setError(Throwable throwable) {
    var result = cached.updateAndGet(holder -> holder.error(throwable));
    this.onUpdateError(throwable, result.epoch);
    return result;
  }

  /** Reads the associated props. This method is synchronized to prevent concurrent runs. */
  private void readValues() {
    List<Throwable> errors = new ArrayList<>();

    // attempt to read all values
    T v1 = readVal(first, errors);
    U v2 = readVal(second, errors);
    V v3 = readVal(third, errors);
    W v4 = readVal(fourth, errors);
    X v5 = readVal(fifth, errors);

    if (errors.isEmpty()) {
      // if no errors, construct a result
      cached.updateAndGet(holder -> holder.value(Tuple.of(v1, v2, v3, v4, v5)));
    } else {
      // otherwise, collect all the logged exceptions
      var exc = new InvalidReadOpException("One or more errors");
      errors.forEach(exc::addSuppressed);

      // and set the errored state
      setError(exc);
    }
  }

  /**
   * Reads the prop's value or stores the thrown exception.
   *
   * @param prop the prop to load
   * @param errors the errors collector
   * @param <PropT> the type of the underlying prop
   * @return the read value, or null
   */
  @Nullable
  private <PropT> PropT readVal(AbstractProp<PropT> prop, List<Throwable> errors) {
    try {
      return prop.get();
    } catch (RuntimeException e) {
      errors.add(e);
      return null;
    }
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
  public Tuple<T, U, V, W, X> get() {
    var holder = cached.get();
    try {
      return holder.get();
    } catch (Throwable e) {
      throw ensureUnchecked(e);
    }
  }

  /**
   * Returns a key for this object. The key is generated using {@link
   * AbstractPropGroup#multiKey(String, String...)}.
   *
   * @return the key that identifies this prop group
   */
  @Override
  public String key() {
    return key;
  }
}