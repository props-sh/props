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
import java.util.function.UnaryOperator;
import sh.props.Prop;
import sh.props.SubscribableProp;
import sh.props.annotations.Nullable;
import sh.props.tuples.Quad;
import sh.props.tuples.Tuple;

/**
 * Quadruple supplier implementation
 *
 * @param <T> the type of the first prop
 * @param <U> the type of the second prop
 * @param <V> the type of the third prop
 */
class QuadSupplierImpl<T, U, V, W> extends SubscribableProp<Quad<T, U, V, W>>
    implements QuadSupplier<T, U, V, W> {

  private final BlockingQueue<UnaryOperator<Quad<T, U, V, W>>> ops = new LinkedBlockingQueue<>();
  private final ReentrantLock sendStage = new ReentrantLock();
  private final AtomicReference<Quad<T, U, V, W>> value;

  /**
   * Constructs the provider.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param fourth the fourth prop
   */
  QuadSupplierImpl(Prop<T> first, Prop<U> second, Prop<V> third, Prop<W> fourth) {
    // subscribe to all updates and errors
    first.subscribe(
        v -> {
          this.ops.add(Quad.applyFirst(v));
          this.onUpdatedValue();
        },
        this::onUpdateError);
    second.subscribe(
        v -> {
          this.ops.add(Quad.applySecond(v));
          this.onUpdatedValue();
        },
        this::onUpdateError);
    third.subscribe(
        v -> {
          this.ops.add(Quad.applyThird(v));
          this.onUpdatedValue();
        },
        this::onUpdateError);
    fourth.subscribe(
        v -> {
          this.ops.add(Quad.applyFourth(v));
          this.onUpdatedValue();
        },
        this::onUpdateError);

    // retrieve a current state of the underlying props
    this.value =
        new AtomicReference<>(Tuple.of(first.get(), second.get(), third.get(), fourth.get()));
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
                Quad<T, U, V, W> value = this.get();
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
    UnaryOperator<Quad<T, U, V, W>> op;

    // iterate over all ops and apply them
    while ((op = this.ops.poll()) != null) {
      this.value.updateAndGet(op);
      res = true;
    }

    return res;
  }

  @Override
  @Nullable
  public Quad<T, U, V, W> get() {
    return this.value.get();
  }
}
