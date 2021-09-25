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

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import sh.props.source.RefreshableSource;
import sh.props.source.refresh.util.BackgroundExecutorFactory;

public class Scheduler {

  protected final ScheduledExecutorService executor;

  /**
   * Class constructor.
   *
   * <p>Creates a new scheduled executor using {@link BackgroundExecutorFactory#create(int)}, with a
   * single thread.
   */
  public Scheduler() {
    this(BackgroundExecutorFactory.create(1));
  }

  /**
   * Class constructor.
   *
   * @param executor The executor used to schedule any refresh operations.
   */
  public Scheduler(ScheduledExecutorService executor) {
    this.executor = executor;
  }

  /**
   * Schedules a {@link RefreshableSource} for periodic data refreshes. The first execution will be
   * scheduled after the specified period. If you want to immediately execute a refresh use the
   * {@link #refreshSourceAfter(RefreshableSource, Duration, Duration)} method, specifying <code>
   * initialDelay=0</code> .
   *
   * @param source the source to reload
   * @param period minimum duration between subsequent refreshes
   */
  public void refreshSourceAfter(RefreshableSource source, Duration period) {
    this.refreshSourceAfter(source, period, period);
  }

  /**
   * Schedules a {@link RefreshableSource} for periodic data refreshes.
   *
   * @param source the source to reload
   * @param initialDelay how long to wait before the first refresh
   * @param period minimum duration between subsequent refreshes
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public void refreshSourceAfter(RefreshableSource source, Duration initialDelay, Duration period) {
    this.executor.scheduleAtFixedRate(
        new Trigger(source), initialDelay.toNanos(), period.toNanos(), NANOSECONDS);
  }
}
