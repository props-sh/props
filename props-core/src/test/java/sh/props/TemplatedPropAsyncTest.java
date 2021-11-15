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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static sh.props.source.impl.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import org.junit.jupiter.api.Test;
import sh.props.group.TemplatedProp;
import sh.props.source.impl.InMemory;
import sh.props.testhelpers.DummyConsumer;
import sh.props.testhelpers.TestIntProp;

@SuppressWarnings("NullAway")
class TemplatedPropAsyncTest {

  @Test
  void singlePropTemplate() {
    // ARRANGE
    final var expected = "I am expecting 1";

    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp("key1", null));
    var templatedProp = TemplatedProp.of("I am expecting %s", prop1);

    DummyConsumer<String> consumer = spy(new DummyConsumer<>());
    templatedProp.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put("key1", "1");

    // ASSERT
    verify(consumer, timeout(1_000)).accept(expected);
  }

  @Test
  void pairTemplate() {
    // ARRANGE
    final var expected = "I am expecting 1 and 2";

    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp("key1", null));
    var prop2 = registry.bind(new TestIntProp("key2", null));
    var templatedProp = TemplatedProp.of("I am expecting %s and %s", prop1, prop2);

    DummyConsumer<String> consumer = spy(new DummyConsumer<>());
    templatedProp.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");

    // ASSERT
    verify(consumer, timeout(3_000)).accept(expected);
  }

  @Test
  void tripleTemplate() {
    // ARRANGE
    final var expected = "I am expecting 1, 2, and 3";

    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp("key1", null));
    var prop2 = registry.bind(new TestIntProp("key2", null));
    var prop3 = registry.bind(new TestIntProp("key3", null));
    var templatedProp = TemplatedProp.of("I am expecting %s, %s, and %s", prop1, prop2, prop3);

    DummyConsumer<String> consumer = spy(new DummyConsumer<>());
    templatedProp.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");

    // ASSERT
    verify(consumer, timeout(1_000)).accept(expected);
  }

  @Test
  void quadTemplate() {
    // ARRANGE
    final var expected = "I am expecting 1, 2, 3, and 4";

    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp("key1", null));
    var prop2 = registry.bind(new TestIntProp("key2", null));
    var prop3 = registry.bind(new TestIntProp("key3", null));
    var prop4 = registry.bind(new TestIntProp("key4", null));
    var templatedProp =
        TemplatedProp.of("I am expecting %s, %s, %s, and %s", prop1, prop2, prop3, prop4);

    DummyConsumer<String> consumer = spy(new DummyConsumer<>());
    templatedProp.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");
    source.put("key4", "4");

    // ASSERT
    verify(consumer, timeout(1_000)).accept(expected);
  }

  @Test
  void tupleTemplate() {
    // ARRANGE
    final var expected = "I am expecting 1, 2, 3, 4, and 5";

    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp("key1", null));
    var prop2 = registry.bind(new TestIntProp("key2", null));
    var prop3 = registry.bind(new TestIntProp("key3", null));
    var prop4 = registry.bind(new TestIntProp("key4", null));
    var prop5 = registry.bind(new TestIntProp("key5", null));
    var templatedProp =
        TemplatedProp.of(
            "I am expecting %s, %s, %s, %s, and %s", prop1, prop2, prop3, prop4, prop5);

    DummyConsumer<String> consumer = spy(new DummyConsumer<>());
    templatedProp.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");
    source.put("key4", "4");
    source.put("key5", "5");

    // ASSERT
    verify(consumer, timeout(1_000)).accept(expected);
  }
}
