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

package sh.props.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/** Creates daemon {@link Thread}s. */
class DaemonThreadFactory implements ThreadFactory {

  private final ThreadFactory factory;

  /** Spawns threads using the default {@link ThreadFactory}. */
  public DaemonThreadFactory() {
    this(Executors.defaultThreadFactory());
  }

  /**
   * Allows a custom {@link ThreadFactory} to be specified.
   *
   * @param factory a custom thread factory
   */
  public DaemonThreadFactory(ThreadFactory factory) {
    this.factory = factory;
  }

  /** Creates a new daemon {@link Thread}. */
  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = this.factory.newThread(runnable);
    thread.setDaemon(true);
    return thread;
  }
}
