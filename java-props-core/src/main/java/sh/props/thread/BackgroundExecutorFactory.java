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

package sh.props.thread;

import static java.lang.String.format;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BackgroundExecutorFactory {

  public static final Duration DEFAULT_GRACE_PERIOD = Duration.ofSeconds(5);

  private static final Logger log = Logger.getLogger(BackgroundExecutorFactory.class.getName());

  /**
   * Creates {@link ScheduledExecutorService} based on daemon {@link Thread}s.
   *
   * <p>Any such executors will attempt graceful shutdown when instructed and will wait for any
   * current requests to complete, up to the default {@link #DEFAULT_GRACE_PERIOD}.
   *
   * @param threads the number of threads in the pool
   * @return an initialized scheduled executor
   */
  public static ScheduledExecutorService create(int threads) {
    return create(threads, DEFAULT_GRACE_PERIOD);
  }

  /**
   * Creates {@link ScheduledExecutorService} based on daemon {@link Thread}s.
   *
   * <p>Any such executors will attempt graceful shutdown when instructed and will wait for any
   * current requests to complete, up to the specified grace period (millisecond granularity).
   *
   * @param threads the number of threads in the pool
   * @param shutdownGracePeriod the maximum grace period before forcefully shutting down the
   *     executor
   * @return an initialized scheduled executor
   */
  public static ScheduledExecutorService create(int threads, Duration shutdownGracePeriod) {
    var executor = Executors.newScheduledThreadPool(threads, new DaemonThreadFactory());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(executor, shutdownGracePeriod)));
    log.info(
        () ->
            format(
                "Initialized %s with %d threads and a grace period of %d ms",
                executor, threads, shutdownGracePeriod.toMillis()));
    return executor;
  }

  /**
   * Gracefully terminate the specified {@link ScheduledExecutorService}, waiting for the specified
   * grace period (millisecond granularity).
   *
   * @param executor the executor to shutdown
   * @param gracePeriod the maximum grace period before forcing a shutdown
   */
  public static void shutdown(ExecutorService executor, Duration gracePeriod) {
    boolean ok = true;

    log.info(() -> format("Attempting to shut down %s", executor));
    long t0 = System.currentTimeMillis();
    executor.shutdown();
    try {
      // wait for running threads to complete
      ok = executor.awaitTermination(gracePeriod.toMillis(), TimeUnit.MILLISECONDS);
      log.info(
          () ->
              format(
                  "%s shut down after %d milliseconds", executor, System.currentTimeMillis() - t0));
    } catch (InterruptedException e) {
      log.warning(() -> format("Interrupted while waiting for shutdown %s", executor));
      ok = false;
      Thread.currentThread().interrupt();
    } finally {
      // if the executor could not be shutdown, attempt to forcefully shut it down
      if (!ok) {
        log.info(() -> format("Attempting a forceful shutdown of %s", executor));
        executor.shutdownNow();
      }
    }
  }
}
