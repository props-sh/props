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

package sh.props.testhelpers;

import java.util.Map;
import java.util.Objects;
import sh.props.source.Source;
import sh.props.source.SourceFactory;

/** Creates a source used for tests, which only defines a key=value entry. */
public class TestSource extends Source {
  public static final String ID = "test-source";

  @Override
  public String id() {
    return "test-source";
  }

  @Override
  public Map<String, String> get() {
    return Map.of("key", "value");
  }

  /** Defines a source factory used in tests. */
  public static class Factory implements SourceFactory<TestSource> {

    @Override
    public TestSource create(String id) {
      if (!Objects.equals(TestSource.ID, id)) {
        throw new IllegalArgumentException("Invalid id: " + id);
      }

      return new TestSource();
    }
  }
}
