/*
 * MIT License
 *
 * Copyright (c) 2021 - 2021 Mihai Bojin
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

import java.util.Collections;
import java.util.Map;
import sh.props.annotations.Nullable;
import sh.props.source.OnDemandSource;

public class TestOnDemandSource extends OnDemandSource {

  private final Map<String, String> backingData;
  private final long sleepMillis;

  public TestOnDemandSource(Map<String, String> backingData, long sleepMillis) {
    this.backingData = Collections.unmodifiableMap(backingData);
    this.sleepMillis = sleepMillis;
  }

  @Nullable
  @Override
  protected String loadKey(String key) {
    try {
      // simulate a real-world scenario
      Thread.sleep(sleepMillis);
    } catch (InterruptedException e) {
      // nothing to do
    }

    // retrieve the key
    return backingData.get(key);
  }

  @Override
  public String id() {
    return "ON_DEMAND";
  }
}