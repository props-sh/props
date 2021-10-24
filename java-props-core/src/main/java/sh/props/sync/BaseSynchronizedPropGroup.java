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

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import sh.props.SubscribableProp;
import sh.props.annotations.Nullable;

public class BaseSynchronizedPropGroup<TUPLE> extends SubscribableProp<TUPLE> {

  protected final AtomicReference<TUPLE> value;
  private final BlockingQueue<UnaryOperator<TUPLE>> ops = new LinkedBlockingQueue<>();
  private final ReentrantLock sendStage = new ReentrantLock();
  private final AtomicReference<TUPLE> lastSentValue = new AtomicReference<>();

  public BaseSynchronizedPropGroup(AtomicReference<TUPLE> value) {
    this.value = value;
  }

  /**
   * Applies all ops on the specified value and then updates any subscribers, while locking to
   * ensure that all subscribers receive the same value, during a run.
   *
   * <p>The method locks since the update is processed asynchronously using the default {@link
   * ForkJoinPool}. This removes the risk that a subset of the specified subscribers (consumers)
   * might receive one value, while the others might be receiving a different value.
   *
   * <p>Since the underlying Props are also updated asynchronously, this implementation might send
   * more than one event, eventually converging to the final value representing all of the
   * underlying props.
   */
  private void applyOpsAndNotifySubscribers() {
    // check if any consumers are registered
    if (this.updateHandlers.isEmpty()) {
      // if there are no subscribers, ensure that the updates are applied before returning
      this.applyUpdatesOnValue();

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
                TUPLE val = this.applyUpdatesOnValue();

                // check if the last value we sent is different to the one we're trying to send
                if (Objects.equals(this.lastSentValue.get(), val)) {
                  // avoid sending duplicate events
                  return;
                }

                // send the same value to all consumers
                this.updateHandlers.forEach(c -> c.accept(val));

                // update the last sent value
                this.lastSentValue.set(val);
              } finally {
                // ensure the updated value was received by all consumers
                // before allowing a new update to be processed
                this.sendStage.unlock();
              }
            });
  }

  /**
   * Processes any operations, applying them to the associated value.
   *
   * @return the final value after applying all/any of the updates
   */
  @Nullable
  private TUPLE applyUpdatesOnValue() {
    // get the initial value
    TUPLE result = this.value.get();

    // iterate over all ops and apply them
    UnaryOperator<TUPLE> op;
    while ((op = this.ops.poll()) != null) {
      result = this.value.updateAndGet(op);
    }

    return result;
  }

  /**
   * Stores the op and asynchronously sends the update to subscribers, if any are registered.
   *
   * @param transformation a function that generates the op to apply, given the passed value
   * @param onValue the value to apply via a transformation
   */
  protected <V> void apply(Function<V, UnaryOperator<TUPLE>> transformation, V onValue) {
    this.ops.add(transformation.apply(onValue));
    this.applyOpsAndNotifySubscribers();
  }

  @Override
  @Nullable
  public TUPLE get() {
    return this.value.get();
  }
}
