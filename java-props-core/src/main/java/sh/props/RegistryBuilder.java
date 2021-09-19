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

package sh.props;

import java.util.ArrayList;
import java.util.List;
import sh.props.annotations.Nullable;

public class RegistryBuilder {

  List<Source> sources = new ArrayList<>();

  public RegistryBuilder source(Source source) {
    this.sources.add(source);
    return this;
  }

  /**
   * Builds a registry object, given the sources that were already added.
   *
   * @return a configured {@link Registry} object
   */
  public Registry build() {
    Registry registry = new Registry();
    int counter = 0;
    @Nullable Layer tail = null;

    for (Source source : this.sources) {
      Layer layer = new Layer(source, registry, counter++);
      layer.prev = tail;
      if (tail != null) {
        tail.next = layer;
      }
      tail = layer;
    }
    registry.topLayer = tail;

    return registry;
  }
}
