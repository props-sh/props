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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class Synchronizer {

  /**
   * Applies all ops on the specified value and then updates any subscribers, while locking to
   * ensure that all subscribers receive the same value, during a run.
   *
   * <p>The method locks since the update is processed using the default {@link ForkJoinPool},
   * introducing a risk that a subset of the specified subscribers (consumers) might receive one
   * value, while the others might be receiving a different value.
   */
  static <T> void applyOpsAndNotifySubscribers(
      BlockingQueue<UnaryOperator<T>> ops,
      AtomicReference<T> value,
      List<Consumer<T>> onUpdate,
      ReentrantLock lock) {
    // check if any consumers are registered
    if (onUpdate.isEmpty()) {
      // if there are no subscribers, ensure that the updates are applied before returning
      applyUpdatesOnValue(ops, value);

      // do not submit a ForkJoinTask if there are no subscribers to notify
      return;
    }

    ForkJoinPool.commonPool()
        .execute(
            () -> {
              // ensure we execute a single update concurrently
              lock.lock();

              try {
                // apply the update ops
                applyUpdatesOnValue(ops, value);

                // send the same value to all consumers
                T val = value.get();
                onUpdate.forEach(c -> c.accept(val));
              } finally {
                // ensure the updated value was received by all consumers
                // before allowing a new update to be processed
                lock.unlock();
              }
            });
  }

  /** Processes any operations, applying them to the associated value. */
  static <T> void applyUpdatesOnValue(
      BlockingQueue<UnaryOperator<T>> ops, AtomicReference<T> value) {
    // iterate over all ops and apply them
    UnaryOperator<T> op;
    while ((op = ops.poll()) != null) {
      value.updateAndGet(op);
    }
  }
}
