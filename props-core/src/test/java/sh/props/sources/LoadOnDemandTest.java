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

package sh.props.sources;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static sh.props.converters.Cast.asString;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import sh.props.RegistryBuilder;
import sh.props.textfixtures.AwaitAssertionTest;
import sh.props.textfixtures.TestOnDemandSource;

@SuppressWarnings({"NullAway", "VariableDeclarationUsageDistance"})
class LoadOnDemandTest extends AwaitAssertionTest {

  @RepeatedTest(10)
  void testLoadPropOnDemandSuperFast() {
    internalOnDemandSourceTest(10L);
  }

  @RepeatedTest(10)
  void testLoadPropOnDemandFast() {
    internalOnDemandSourceTest(50L);
  }

  @Test
  void testLoadPropOnDemandSlow() {
    internalOnDemandSourceTest(500L);
  }

  private void internalOnDemandSourceTest(long sleepMillis) {
    // ARRANGE
    Map<String, String> data = new HashMap<>();
    data.put("key1", "value1");
    data.put("key2", "value2");

    var source = new TestOnDemandSource(data, sleepMillis);

    var registry = new RegistryBuilder(source).build();
    var prop = registry.builder(asString()).build("key1");

    // ACT / ASSERT
    assertThat(registry.get("key0"), nullValue());
    assertThat(registry.get("key1"), equalTo("value1"));
    assertThat(registry.get("key2"), equalTo("value2"));

    data.put("key1", "value3");
    source.refresh();
    await().until(prop::get, equalTo("value3"));

    data.remove("key1");
    source.refresh();
    await().until(prop::get, nullValue());
  }
}
