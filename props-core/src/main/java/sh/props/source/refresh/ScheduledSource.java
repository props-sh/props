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

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import sh.props.source.Schedulable;
import sh.props.source.Source;

public class ScheduledSource extends Source implements Schedulable {

  private final Source delegate;
  private volatile boolean scheduled;

  public ScheduledSource(Source delegate, boolean scheduled) {
    this.delegate = delegate;
    this.scheduled = scheduled;
  }

  @Override
  public String id() {
    return this.delegate.id();
  }

  @Override
  public Map<String, String> get() {
    return this.delegate.get();
  }

  @Override
  public boolean scheduled() {
    return this.scheduled;
  }

  @Override
  public void setScheduled() {
    this.scheduled = true;
  }

  @Override
  public void register(Consumer<Map<String, String>> subscriber) {
    this.delegate.register(subscriber);
  }

  @Override
  public Stream<Consumer<Map<String, String>>> subscribers() {
    return this.delegate.subscribers();
  }

  @Override
  public void updateSubscribers() {
    this.delegate.updateSubscribers();
  }
}
