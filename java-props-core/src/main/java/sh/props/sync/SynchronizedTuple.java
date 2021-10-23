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

package sh.props.sync;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import sh.props.Prop;
import sh.props.SubscribableProp;
import sh.props.annotations.Nullable;
import sh.props.tuples.Tuple;

/**
 * Synchronizes five Props, retrieving them all at once. If any of the underlying Props fails to
 * update its value and calls {@link #onUpdateError(Throwable)}, the exception will be passed down
 * to any subscribers and the corresponding Prop will not be updated in the resulting {@link Tuple}.
 *
 * @param <T> the type of the first prop
 * @param <U> the type of the second prop
 * @param <V> the type of the third prop
 * @param <W> the type of the fourth prop
 * @param <X> the type of the fifth prop
 */
class SynchronizedTuple<T, U, V, W, X> extends SubscribableProp<Tuple<T, U, V, W, X>>
    implements Prop<Tuple<T, U, V, W, X>> {

  private final BlockingQueue<UnaryOperator<Tuple<T, U, V, W, X>>> ops =
      new LinkedBlockingQueue<>();
  private final ReentrantLock sendStage = new ReentrantLock();
  private final AtomicReference<Tuple<T, U, V, W, X>> value;

  /**
   * Constructs the provider.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param fourth the fourth prop
   * @param fifth the fifth prop
   */
  SynchronizedTuple(Prop<T> first, Prop<U> second, Prop<V> third, Prop<W> fourth, Prop<X> fifth) {

    // subscribe to all updates and errors
    first.subscribe(v -> this.apply(SynchronizedTuple::updateFirst, v), this::onUpdateError);
    second.subscribe(v -> this.apply(SynchronizedTuple::updateSecond, v), this::onUpdateError);
    third.subscribe(v -> this.apply(SynchronizedTuple::updateThird, v), this::onUpdateError);
    fourth.subscribe(v -> this.apply(SynchronizedTuple::updateFourth, v), this::onUpdateError);
    fifth.subscribe(v -> this.apply(SynchronizedTuple::updateFifth, v), this::onUpdateError);

    // retrieve the current state of the underlying props
    // it's important for this step to execute after we have subscribed to the underlying props
    // since any change operations will have been captured and will be applied on the underlying
    // atomic reference
    this.value =
        new AtomicReference<>(
            Tuple.of(first.get(), second.get(), third.get(), fourth.get(), fifth.get()));
  }

  /**
   * Stores the op and asynchronously sends the update to subscribers, if any are registered.
   *
   * @param f a function that generates the op to apply, given the passed value
   * @param v the value to apply
   * @param <K> the type of the value
   */
  private <K> void apply(Function<K, UnaryOperator<Tuple<T, U, V, W, X>>> f, K v) {
    this.ops.add(f.apply(v));
    this.onUpdatedValue();
  }

  /**
   * Custom implementation that Notifies all subscribers of the newest value of this prop. This
   * method does not accept a value but instead retrieves the latest value at the time of execution
   * (the update is processed using the default {@link ForkJoinPool}, only if another update is not
   * in-progress and also ensuring a single update event for a group of concurrent changes.
   */
  @Override
  protected void onUpdatedValue() {
    // apply the update ops
    this.processUpdates();

    // check if any consumers are registered
    if (this.valueSubscribers.isEmpty()) {
      // do not submit a ForkJoinTask if there are no subscribers to notify
      return;
    }

    ForkJoinPool.commonPool()
        .execute(
            () -> {
              // ensure we execute a single update concurrently
              this.sendStage.lock();

              try {
                // apply the update ops
                this.processUpdates();

                // send the same value to all consumers
                Tuple<T, U, V, W, X> value = this.get();
                this.valueSubscribers.forEach(c -> c.accept(value));
              } finally {
                // ensure the updated value was received by all consumers
                // before allowing a new update to be processed
                this.sendStage.unlock();
              }
            });
  }

  /**
   * Processes any operations on the associated value, applying the modifications before allowing
   * the encompassing logic to proceed.
   *
   * @return true if updates were processed
   */
  private boolean processUpdates() {
    boolean res = false;
    UnaryOperator<Tuple<T, U, V, W, X>> op;

    // iterate over all ops and apply them
    while ((op = this.ops.poll()) != null) {
      this.value.updateAndGet(op);
      res = true;
    }

    return res;
  }

  @Override
  @Nullable
  public Tuple<T, U, V, W, X> get() {
    return this.value.get();
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its first value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @param <V> the type of the third object in the value
   * @param <W> the type of the fourth object in the value
   * @param <X> the type of the fifth object in the value
   * @return a new object with the value updated
   */
  private static <T, U, V, W, X> UnaryOperator<Tuple<T, U, V, W, X>> updateFirst(T value) {
    return prev -> Tuple.of(value, prev.second, prev.third, prev.fourth, prev.fifth);
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its second value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @param <V> the type of the third object in the value
   * @param <W> the type of the fourth object in the value
   * @param <X> the type of the fifth object in the value
   * @return a new object with the value updated
   */
  private static <T, U, V, W, X> UnaryOperator<Tuple<T, U, V, W, X>> updateSecond(U value) {
    return prev -> Tuple.of(prev.first, value, prev.third, prev.fourth, prev.fifth);
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its third value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @param <V> the type of the third object in the value
   * @param <W> the type of the fourth object in the value
   * @param <X> the type of the fifth object in the value
   * @return a new object with the value updated
   */
  private static <T, U, V, W, X> UnaryOperator<Tuple<T, U, V, W, X>> updateThird(V value) {
    return prev -> Tuple.of(prev.first, prev.second, value, prev.fourth, prev.fifth);
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its fourth value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @param <V> the type of the third object in the value
   * @param <W> the type of the fourth object in the value
   * @param <X> the type of the fifth object in the value
   * @return a new object with the value updated
   */
  private static <T, U, V, W, X> UnaryOperator<Tuple<T, U, V, W, X>> updateFourth(W value) {
    return prev -> Tuple.of(prev.first, prev.second, prev.third, value, prev.fifth);
  }
  /**
   * Returns an operation that can be applied to the given object, modifying its fourth value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @param <V> the type of the third object in the value
   * @param <W> the type of the fourth object in the value
   * @param <X> the type of the fifth object in the value
   * @return a new object with the value updated
   */
  private static <T, U, V, W, X> UnaryOperator<Tuple<T, U, V, W, X>> updateFifth(X value) {
    return prev -> Tuple.of(prev.first, prev.second, prev.third, prev.fourth, value);
  }
}
