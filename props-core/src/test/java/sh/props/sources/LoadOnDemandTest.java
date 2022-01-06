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
  private static final String KEY_1 = "key1";
  private static final String KEY_2 = "key2";
  private static final String VALUE_1 = "value1";
  private static final String VALUE_2 = "value2";
  private static final String VALUE_3 = "value3";

  @RepeatedTest(10)
  @SuppressWarnings(
      "PMD.JUnitTestsShouldIncludeAssert") // PMD doesn't detect assertions in private static helper
  void testLoadPropOnDemandSuperFast() {
    internalOnDemandSourceTest(10L);
  }

  @RepeatedTest(10)
  @SuppressWarnings(
      "PMD.JUnitTestsShouldIncludeAssert") // PMD doesn't detect assertions in private static helper
  void testLoadPropOnDemandFast() {
    internalOnDemandSourceTest(50L);
  }

  @Test
  @SuppressWarnings(
      "PMD.JUnitTestsShouldIncludeAssert") // PMD doesn't detect assertions in private static helper
  void testLoadPropOnDemandSlow() {
    internalOnDemandSourceTest(500L);
  }

  private void internalOnDemandSourceTest(long sleepMillis) {
    // ARRANGE
    Map<String, String> data = new HashMap<>();
    data.put(KEY_1, VALUE_1);
    data.put(KEY_2, VALUE_2);

    var source = new TestOnDemandSource(data, sleepMillis);

    var registry = new RegistryBuilder(source).build();
    var prop = registry.builder(asString()).build(KEY_1);

    // ACT / ASSERT
    assertThat("Expecting no value for undefined keys", registry.get("UNSET_KEY"), nullValue());
    assertThat("Expecting key to be set", registry.get(KEY_1), equalTo(VALUE_1));
    assertThat("Expecting key to be set", registry.get(KEY_2), equalTo(VALUE_2));

    data.put(KEY_1, VALUE_3);
    source.refresh();
    // expecting value to be updated
    await().until(prop::get, equalTo(VALUE_3));

    data.remove(KEY_1);
    source.refresh();
    // expecting value to be updated
    await().until(prop::get, nullValue());
  }
}
