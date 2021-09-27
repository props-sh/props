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
import java.util.Map;
import java.util.function.Consumer;

/** Interface denoting sources that can be refreshed. */
public interface RefreshableSource extends Source {

  Duration DEFAULT_REFRESH_PERIOD = Duration.ofSeconds(5);

  /**
   * Registers a consumer to be called when the source is updated.
   *
   * @param downstream a consumer that accepts any updates this source may be sending
   */
  void register(Consumer<Map<String, String>> downstream);

  /** Triggers an update of this source's key,value pairs. */
  Map<String, String> refresh();

  /**
   * The duration between refreshes.
   *
   * @return a duration object specifying how often the source should be refreshed
   */
  Duration refreshPeriod();

  /**
   * The initial delay before refreshing the source for the first time.
   *
   * @return a duration object; defaults to the value of {@link #refreshPeriod()}
   */
  default Duration initialRefreshDelay() {
    return this.refreshPeriod();
  }
}
