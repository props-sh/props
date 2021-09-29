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

import java.nio.file.Path;
import sh.props.source.AbstractSource;

/** A special type of source which is backed by a file, on disk. */
public abstract class FileWatchableSource extends AbstractSource
    implements FileWatchable, Schedulable {

  private final Path file;
  private boolean wasScheduled = false;

  protected FileWatchableSource(Path file) {
    this.file = file;
  }

  /**
   * Returns the location, on disk, of the file backing this source.
   *
   * @return a non-null {@link Path} pointing to a file on disk
   */
  @Override
  public Path file() {
    return this.file;
  }

  /**
   * Determines if the object was scheduled for refreshes.
   *
   * @return <code>true</code> if already scheduled
   */
  @Override
  public boolean scheduled() {
    return this.wasScheduled;
  }

  /** Mark this source as having been scheduled for periodic execution. */
  @Override
  public void setScheduled() {
    this.wasScheduled = true;
  }
}
