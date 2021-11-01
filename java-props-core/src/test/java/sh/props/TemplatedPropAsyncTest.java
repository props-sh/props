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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import sh.props.converter.IntegerConverter;
import sh.props.group.Group;
import sh.props.source.impl.InMemory;

@SuppressWarnings("NullAway")
class TemplatedPropAsyncTest {

  @Test
  void singlePropTemplate() {
    // ARRANGE
    final var expected = "I am expecting 1";

    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    var prop1 = registry.bind(new IntProp("key1", null));
    var templatedProp = prop1.renderTemplate("I am expecting %s");

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

    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    var prop1 = registry.bind(new IntProp("key1", null));
    var prop2 = registry.bind(new IntProp("key2", null));
    var group = Group.of(prop1, prop2);
    var templatedProp = group.renderTemplate("I am expecting %s and %s");

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

    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    var prop1 = registry.bind(new IntProp("key1", null));
    var prop2 = registry.bind(new IntProp("key2", null));
    var prop3 = registry.bind(new IntProp("key3", null));
    var group = Group.of(prop1, prop2, prop3);
    var templatedProp = group.renderTemplate("I am expecting %s, %s, and %s");

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

    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    var prop1 = registry.bind(new IntProp("key1", null));
    var prop2 = registry.bind(new IntProp("key2", null));
    var prop3 = registry.bind(new IntProp("key3", null));
    var prop4 = registry.bind(new IntProp("key4", null));
    var group = Group.of(prop1, prop2, prop3, prop4);

    // ACT
    var templatedProp = group.renderTemplate("I am expecting %s, %s, %s, and %s");

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

    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    var prop1 = registry.bind(new IntProp("key1", null));
    var prop2 = registry.bind(new IntProp("key2", null));
    var prop3 = registry.bind(new IntProp("key3", null));
    var prop4 = registry.bind(new IntProp("key4", null));
    var prop5 = registry.bind(new IntProp("key5", null));
    var group = Group.of(prop1, prop2, prop3, prop4, prop5);
    var templatedProp = group.renderTemplate("I am expecting %s, %s, %s, %s, and %s");

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

  private static class DummyConsumer<T> implements Consumer<T> {

    @Override
    public void accept(T t) {
      // no-op
    }
  }

  private static class IntProp extends CustomProp<Integer> implements IntegerConverter {

    protected IntProp(String key, Integer defaultValue) {
      super(key, defaultValue, null, false, false);
    }
  }
}
