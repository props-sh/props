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

package sh.props.source.refresh;

import static java.lang.String.format;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.source.Refreshable;

/**
 * Convenience implementation that triggers a {@link Refreshable#refresh()} and logs any runtime
 * exceptions.
 */
public class Trigger implements Runnable {

  private static final Logger log = Logger.getLogger(Trigger.class.getName());

  private final Refreshable source;
  private final ReentrantLock concurrencyLock = new ReentrantLock();

  /**
   * Class constructor.
   *
   * @param source the source that will be refreshed when this trigger executes
   */
  public Trigger(Refreshable source) {
    this.source = source;
  }

  @Override
  @SuppressWarnings("FutureReturnValueIgnored")
  public void run() {
    long t0 = System.nanoTime();

    // determine if we can run the update
    if (!this.concurrencyLock.tryLock()) {
      log.log(
          Level.WARNING, () -> "Another refresh is already in progress; skipping this interval");
      return;
    }

    try {
      CompletableFuture.runAsync(this.source::refresh)
          .whenComplete((unused, throwable) -> this.markDone(t0, throwable));
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e, () -> "Unexpected exception while refreshing the source");

      // in case that an error occurred, unlock anyway to give the operation a future chance
      this.markDone(t0, e);
    }
  }

  /**
   * Releases the concurrency lock and logs the total time it took to execute the refresh operation.
   *
   * @param startTime the original starting time, in nanoseconds
   * @param ex a possible exception thrown during the refresh operation
   */
  protected void markDone(long startTime, Throwable ex) {
    try {
      this.concurrencyLock.unlock();
    } finally {
      long durationNanos = System.nanoTime() - startTime;
      if (ex != null) {
        log.log(
            Level.WARNING,
            ex,
            () ->
                format(
                    "Unexpected exception while refreshing %s (%d ns)",
                    this.source, durationNanos));
      } else {
        log.log(Level.FINE, () -> format("Refreshed %s (%d ns)", this.source, durationNanos));
      }
    }
  }
}
