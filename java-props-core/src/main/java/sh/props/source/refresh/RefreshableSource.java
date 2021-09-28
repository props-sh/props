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

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import sh.props.source.AbstractSource;

public abstract class RefreshableSource extends AbstractSource {

  public static Duration DEFAULT_REFRESH_PERIOD = Duration.ofSeconds(5);

  protected final Duration initialDelay;
  protected final Duration refreshPeriod;

  /**
   * Initializes a source that will be refreshed on the specified period and with the desired
   * initial delay.
   *
   * @param initialDelay the initial delay before refreshing for the first time
   * @param refreshPeriod the duration between source refreshes
   */
  RefreshableSource(Duration initialDelay, Duration refreshPeriod) {
    this.initialDelay = initialDelay;
    this.refreshPeriod = refreshPeriod;
  }

  public Duration initialDelay() {
    return this.initialDelay;
  }

  public Duration refreshPeriod() {
    return this.refreshPeriod;
  }

  /**
   * Static factory method that refreshes the source on the {@link #DEFAULT_REFRESH_PERIOD}.
   *
   * @param delegate the source to wrap
   * @return a source that is now refreshable
   */
  public static RefreshableSource of(AbstractSource delegate) {
    return of(delegate, DEFAULT_REFRESH_PERIOD, DEFAULT_REFRESH_PERIOD);
  }

  /**
   * Static factory method that refreshes the source on the specified {@link #refreshPeriod}, while
   * also waiting for that period to pass before the first refresh.
   *
   * @param delegate the source to wrap
   * @return a source that is now refreshable
   */
  public static RefreshableSource of(AbstractSource delegate, Duration refreshPeriod) {
    return of(delegate, refreshPeriod, refreshPeriod);
  }

  /**
   * Static factory method.
   *
   * @param delegate source to wrap as refreshable
   * @param initialDelay the initial delay before refreshing for the first time
   * @param refreshPeriod the duration between source refreshes
   * @return a constructed source
   */
  public static RefreshableSource of(
      AbstractSource delegate, Duration initialDelay, Duration refreshPeriod) {
    return new RefreshableSource(initialDelay, refreshPeriod) {
      @Override
      public String id() {
        return delegate.id();
      }

      @Override
      public Map<String, String> get() {
        return delegate.get();
      }

      @Override
      public void register(Consumer<Map<String, String>> consumer) {
        delegate.register(consumer);
      }

      @Override
      public boolean initialized() {
        return delegate.initialized();
      }

      @Override
      public String toString() {
        return delegate.toString();
      }
    };
  }
}
