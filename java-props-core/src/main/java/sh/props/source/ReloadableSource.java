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

package sh.props.source;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.interfaces.Source;
import sh.props.thread.BackgroundExecutorFactory;

public class ReloadableSource implements Source {

  private static final Logger log = Logger.getLogger(ReloadableSource.class.getName());

  private final List<Consumer<Map<String, String>>> downstream = new ArrayList<>();
  private final Source source;

  public ReloadableSource(Source source, Duration refreshInterval) {
    this(source, BackgroundExecutorFactory.create(1), refreshInterval, refreshInterval);
  }

  public ReloadableSource(
      Source source, ScheduledExecutorService executor, Duration refreshInterval) {
    this(source, executor, refreshInterval, refreshInterval);
  }

  /**
   * Creates a source that can be reloaded periodically.
   *
   * @param source the source to reload
   * @param executor where the reload operation is executed
   * @param initialDelay how long to wait before the first reload
   * @param reloadInterval minimum duration between subsequent reloads
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public ReloadableSource(
      Source source,
      ScheduledExecutorService executor,
      Duration initialDelay,
      Duration reloadInterval) {
    this.source = source;

    // schedule a periodic refresh
    executor.scheduleAtFixedRate(
        new RefreshRunnable(),
        initialDelay.toNanos(),
        reloadInterval.toNanos(),
        TimeUnit.NANOSECONDS);
  }

  /**
   * Inner class responsible for notifying downstream layers that this source's values have been
   * refreshed.
   */
  private class RefreshRunnable implements Runnable {

    @Override
    public void run() {
      try {
        Map<String, String> values = ReloadableSource.this.read();

        // notify all downstream layers of new values received from this source
        // this a simple implementation for the moment, since we expect
        // a small cardinality between sources and layers
        // and that layers process updates fast enough to not cause contention
        for (var downstream : ReloadableSource.this.downstream) {
          downstream.accept(values);
        }
      } catch (RuntimeException e) {
        ReloadableSource.log.log(
            Level.WARNING, e, () -> "Unexpected exception while refreshing the source");
      }
    }
  }

  @Override
  public void register(Consumer<Map<String, String>> downstream) {
    this.downstream.add(downstream);
  }

  /**
   * Delegates to the underlying {@link #source}.
   *
   * @return an unique identifier
   */
  @Override
  public String id() {
    return this.source.id();
  }

  /**
   * Delegates to the underlying {@link #source}.
   *
   * @return all properties defined by the underlying source
   */
  @Override
  public Map<String, String> read() {
    return this.source.read();
  }
}
