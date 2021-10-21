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

import java.util.function.Consumer;
import java.util.function.Supplier;
import sh.props.tuples.Pair;
import sh.props.tuples.Triple;
import sh.props.tuples.Tuple;

/** Helper class used to coordinate retrieving groups of {@link Prop}s. */
@Deprecated
public final class Coordinated {

  /**
   * Coordinates a pair of values. The returned type implements {@link Subscribable}, allowing the
   * user to receive events when any of the values are updated.
   *
   * @param first the first prop
   * @param second the second prop
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @return a coordinated pair of props, which can be retrieved together
   */
  public static <T, U> PairSupplier<T, U> coordinate(Prop<T> first, Prop<U> second) {
    return new PairSupplierImpl<>(first, second, new SubscriberProxy<>());
  }

  /**
   * Coordinates a triple of values. The returned type implements {@link Subscribable}, allowing the
   * user to receive events when any of the values are updated.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   * @return a coordinated triple of props, which can be retrieved together
   */
  public static <T, U, V> TripleSupplier<T, U, V> coordinate(
      Prop<T> first, Prop<U> second, Prop<V> third) {
    return new TripleSupplierImpl<>(first, second, third, new SubscriberProxy<>());
  }

  // Pair

  /**
   * A coordinated pair of props.
   *
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   */
  public interface PairSupplier<T, U> extends Supplier<Pair<T, U>>, Subscribable<Pair<T, U>> {}

  private static class PairSupplierImpl<T, U> implements PairSupplier<T, U> {

    private final Prop<T> first;
    private final Prop<U> second;
    private final SubscriberProxy<Pair<T, U>> subscribers;

    /**
     * Constructs the provider.
     *
     * @param first the first prop
     * @param second the second prop
     * @param subscribers the subscribers that get notified when any of the values change
     */
    PairSupplierImpl(Prop<T> first, Prop<U> second, SubscriberProxy<Pair<T, U>> subscribers) {
      // initialize
      this.subscribers = subscribers;
      this.first = first;
      this.second = second;

      // subscribe to updates
      first.subscribe(
          value -> this.subscribers.sendUpdate(Tuple.of(value, this.second.get())),
          this.subscribers::handleError);
      second.subscribe(
          value -> this.subscribers.sendUpdate(Tuple.of(this.first.get(), value)),
          this.subscribers::handleError);
    }

    @Override
    public Pair<T, U> get() {
      return Tuple.of(this.first.get(), this.second.get());
    }

    /**
     * Subscribes to value updates and errors.
     *
     * @param onUpdate called when any value is updated
     * @param onError called when an update fails
     */
    @Override
    public void subscribe(Consumer<Pair<T, U>> onUpdate, Consumer<Throwable> onError) {
      this.subscribers.subscribe(onUpdate, onError);
    }
  }

  // Triple

  /**
   * A coordinated pair of props.
   *
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   */
  public interface TripleSupplier<T, U, V>
      extends Supplier<Triple<T, U, V>>, Subscribable<Triple<T, U, V>> {}

  /**
   * Internal implementation class.
   *
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   */
  private static class TripleSupplierImpl<T, U, V> implements TripleSupplier<T, U, V> {

    private final Prop<T> first;
    private final Prop<U> second;
    private final Prop<V> third;
    private final SubscriberProxy<Triple<T, U, V>> subscribers;

    /**
     * Constructs the provider.
     *
     * @param first the first prop
     * @param second the second prop
     * @param third the third prop
     * @param subscribers the subscribers that get notified when any of the values change
     */
    TripleSupplierImpl(
        Prop<T> first,
        Prop<U> second,
        Prop<V> third,
        SubscriberProxy<Triple<T, U, V>> subscribers) {
      // initialize
      this.subscribers = subscribers;
      this.first = first;
      this.second = second;
      this.third = third;

      // subscribe to updates
      first.subscribe(
          value -> {
            this.subscribers.sendUpdate(this.get());
          },
          this.subscribers::handleError);
      second.subscribe(
          value -> {
            this.subscribers.sendUpdate(this.get());
          },
          this.subscribers::handleError);
      third.subscribe(
          value -> {
            this.subscribers.sendUpdate(this.get());
          },
          this.subscribers::handleError);
    }

    @Override
    public Triple<T, U, V> get() {
      return Tuple.of(this.first.get(), this.second.get(), this.third.get());
    }

    /**
     * Subscribes to value updates and errors.
     *
     * @param onUpdate called when any value is updated
     * @param onError called when an update fails
     */
    @Override
    public void subscribe(Consumer<Triple<T, U, V>> onUpdate, Consumer<Throwable> onError) {
      this.subscribers.subscribe(onUpdate, onError);
    }

    @Override
    public String toString() {
      return "TripleSupplier{" + this.get() + '}';
    }
  }

  /** Private constructor, preventing instantiation. */
  private Coordinated() {
    // intentionally left blank
  }
}
