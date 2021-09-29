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

public interface Refreshable {

  /**
   * How often should the source be refreshed.
   *
   * @return the duration between source refreshes
   */
  Duration refreshPeriod();

  /**
   * Determines if the underlying source should be eagerly or lazily initialized. The behaviour is
   * set to eager by default.
   *
   * @return true if the source should be eagerly initialized (e.g., the initial delay will be set
   *     to 0)
   */
  default boolean eagerInitialization() {
    return true;
  }

  /**
   * Determines if the object was scheduled for refreshes.
   *
   * @return <code>true</code> if already scheduled
   */
  boolean scheduled();
}
