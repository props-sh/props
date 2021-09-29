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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Abstract class that permits subscribers to register for updates.
 *
 * <p>This functionality is useful for triggering a single {@link Source#get()}, which can be an
 * expensive operation, and notifying multiple layers.
 */
public abstract class AbstractSource implements Source, Subscribable {

  private final List<Consumer<Map<String, String>>> subscribers = new ArrayList<>();

  /**
   * Registers a new downstream subscriber.
   *
   * @param subscriber a subscriber that accepts any updates this source may be sending
   */
  @Override
  public void register(Consumer<Map<String, String>> subscriber) {
    this.subscribers.add(subscriber);
  }

  /**
   * Retrieves a stream of subscribers to this source's updates.
   *
   * @return the subscribers to be notified
   */
  @Override
  public Stream<Consumer<Map<String, String>>> subscribers() {
    return this.subscribers.stream();
  }

  /** Triggers a {@link #get()} call and sends all values to the registered downstream consumers. */
  @Override
  public void updateSubscribers() {
    Map<String, String> data = Collections.unmodifiableMap(this.get());
    this.subscribers().forEach(d -> d.accept(data));
  }

  @Override
  public String toString() {
    return format("Source(%s)", this.id());
  }
}
