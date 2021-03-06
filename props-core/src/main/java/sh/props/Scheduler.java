/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Mihai Bojin
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

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.time.Duration;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

public class Scheduler {

  protected final ScheduledExecutorService executor;
  protected final ForkJoinPool forkJoinPool;

  /**
   * Class constructor.
   *
   * <p>Creates a new scheduled executor using {@link BackgroundExecutorFactory#create()}. This
   * results in instantiating an executor service with a number of threads as returned by the {@link
   * Runtime#availableProcessors()}, and uses the {@link ForkJoinPool}.
   */
  public Scheduler() {
    this(BackgroundExecutorFactory.create(), ForkJoinPool.commonPool());
  }

  /**
   * Class constructor.
   *
   * @param executor the {@link java.util.concurrent.ScheduledExecutorService} used to schedule any
   *     refresh operations.
   * @param forkJoinPool the {@link ForkJoinPool} to use when submitting tasks
   */
  public Scheduler(ScheduledExecutorService executor, ForkJoinPool forkJoinPool) {
    this.executor = executor;
    this.forkJoinPool = forkJoinPool;
  }

  /**
   * Returns the default scheduler, ensuring it is initialized on first-use.
   *
   * @return the default {@link Scheduler}
   */
  public static Scheduler instance() {
    return Initializer.DEFAULT;
  }

  /**
   * Schedules an {@link Source} for periodic data refreshes.
   *
   * @param source the source to refresh
   * @param initialDelay the initial delay before the first refresh is executed
   * @param refreshPeriod the refresh period
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public void refresh(Source source, Duration initialDelay, Duration refreshPeriod) {

    // schedule the source for periodic data refreshes
    this.executor.scheduleAtFixedRate(
        new Trigger(source), initialDelay.toNanos(), refreshPeriod.toNanos(), NANOSECONDS);
  }

  /**
   * Schedules an {@link Source} for periodic data refreshes. It eagerly scheduled the first data
   * load by setting the initial interval to zero.
   *
   * @param source the source to refresh
   * @param refreshPeriod the refresh period
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public void refreshEagerly(Source source, Duration refreshPeriod) {
    this.refresh(source, Duration.ZERO, refreshPeriod);
  }

  /**
   * Executes the specified {@link Runnable} using the {@link #executor}.
   *
   * @param task the task to run
   */
  public void execute(Runnable task) {
    executor.execute(task);
  }

  /**
   * Executes the specified {@link Runnable} using the {@link #forkJoinPool}.
   *
   * @param task the task to run
   */
  public void forkJoinExecute(Runnable task) {
    forkJoinPool.execute(task);
  }

  /** Static holder for the default instance, ensuring lazy initialization. */
  private static final class Initializer {

    private static final Scheduler DEFAULT = new Scheduler();
  }
}
