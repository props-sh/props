/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Mihai Bojin
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

package sh.props.textfixtures;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import sh.props.annotations.Nullable;

/**
 * Test-only implementation that accepts values and deduplicates them, also keeping track of the
 * order in which it received the values.
 */
public class StoreAllUniqueValuesConsumer<T> implements Consumer<T> {

  private final Set<T> store = new LinkedHashSet<>();

  @Override
  public synchronized void accept(T t) {
    this.store.add(t);
  }

  public synchronized ArrayDeque<T> get() {
    return new ArrayDeque<>(this.store);
  }

  /**
   * Retrieves the last value accepted by this consumer.
   *
   * @return the last accepted value, or null if none were accepted
   */
  @Nullable
  public synchronized T getLast() {
    return this.store.stream().skip(this.store.size() - 1).findFirst().orElse(null);
  }
}
