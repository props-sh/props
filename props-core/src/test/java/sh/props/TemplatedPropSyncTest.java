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

package sh.props;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static sh.props.sources.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import org.junit.jupiter.api.Test;
import sh.props.group.TemplatedProp;
import sh.props.sources.InMemory;
import sh.props.textfixtures.TestIntProp;

@SuppressWarnings("NullAway")
class TemplatedPropSyncTest {
  private static final String KEY_1 = "key1";
  private static final String KEY_2 = "key2";
  private static final String KEY_3 = "key3";
  private static final String KEY_4 = "key4";
  private static final String KEY_5 = "key5";

  @Test
  void singlePropTemplate() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY_1, "1");

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp(KEY_1, null));
    final var expected = "I am expecting 1";

    // ACT
    var templatedProp = TemplatedProp.of("I am expecting %s", prop1);

    // ASSERT
    assertThat(
        "Expecting the template to be correctly rendered when populated by a single prop",
        templatedProp.get(),
        equalTo(expected));
  }

  @Test
  void pairTemplate() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY_1, "1");
    source.put(KEY_2, "2");

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp(KEY_1, null));
    var prop2 = registry.bind(new TestIntProp(KEY_2, null));

    final var expected = "I am expecting 1 and 2";

    // ACT
    var templatedProp = TemplatedProp.of("I am expecting %s and %s", prop1, prop2);

    // ASSERT
    assertThat(
        "Expecting the template to be correctly rendered when populated by two props",
        templatedProp.get(),
        equalTo(expected));
  }

  @Test
  void tripleTemplate() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY_1, "1");
    source.put(KEY_2, "2");
    source.put(KEY_3, "3");

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp(KEY_1, null));
    var prop2 = registry.bind(new TestIntProp(KEY_2, null));
    var prop3 = registry.bind(new TestIntProp(KEY_3, null));

    final var expected = "I am expecting 1, 2, and 3";

    // ACT
    var templatedProp = TemplatedProp.of("I am expecting %s, %s, and %s", prop1, prop2, prop3);

    // ASSERT
    assertThat(
        "Expecting the template to be correctly rendered when populated by three props",
        templatedProp.get(),
        equalTo(expected));
  }

  @Test
  void quadTemplate() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY_1, "1");
    source.put(KEY_2, "2");
    source.put(KEY_3, "3");
    source.put(KEY_4, "4");

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp(KEY_1, null));
    var prop2 = registry.bind(new TestIntProp(KEY_2, null));
    var prop3 = registry.bind(new TestIntProp(KEY_3, null));
    var prop4 = registry.bind(new TestIntProp(KEY_4, null));

    final var expected = "I am expecting 1, 2, 3, and 4";

    // ACT
    var templatedProp =
        TemplatedProp.of("I am expecting %s, %s, %s, and %s", prop1, prop2, prop3, prop4);

    // ASSERT
    assertThat(
        "Expecting the template to be correctly rendered when populated by four props",
        templatedProp.get(),
        equalTo(expected));
  }

  @Test
  void tupleTemplate() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY_1, "1");
    source.put(KEY_2, "2");
    source.put(KEY_3, "3");
    source.put(KEY_4, "4");
    source.put(KEY_5, "5");

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp(KEY_1, null));
    var prop2 = registry.bind(new TestIntProp(KEY_2, null));
    var prop3 = registry.bind(new TestIntProp(KEY_3, null));
    var prop4 = registry.bind(new TestIntProp(KEY_4, null));
    var prop5 = registry.bind(new TestIntProp(KEY_5, null));

    final var expected = "I am expecting 1, 2, 3, 4, and 5";

    // ACT
    var templatedProp =
        TemplatedProp.of(
            "I am expecting %s, %s, %s, %s, and %s", prop1, prop2, prop3, prop4, prop5);

    // ASSERT
    assertThat(
        "Expecting the template to be correctly rendered when populated by five props",
        templatedProp.get(),
        equalTo(expected));
  }
}
