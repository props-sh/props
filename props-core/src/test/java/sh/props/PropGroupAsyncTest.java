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

package sh.props;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;
import static sh.props.sources.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import org.junit.jupiter.api.RepeatedTest;
import sh.props.group.Group;
import sh.props.sources.InMemory;
import sh.props.textfixtures.AwaitAssertionTest;
import sh.props.textfixtures.StoreAllUniqueValuesConsumer;
import sh.props.textfixtures.TestIntProp;
import sh.props.tuples.Pair;
import sh.props.tuples.Quad;
import sh.props.tuples.Quintuple;
import sh.props.tuples.Triple;
import sh.props.tuples.Tuple;

@SuppressWarnings({"NullAway", "checkstyle:VariableDeclarationUsageDistance"})
class PropGroupAsyncTest extends AwaitAssertionTest {
  private static final int HOW_MANY_TIMES = 10;
  private static final String KEY_1 = "key1";
  private static final String KEY_2 = "key2";
  private static final String KEY_3 = "key3";
  private static final String KEY_4 = "key4";
  private static final String KEY_5 = "key5";

  @RepeatedTest(value = HOW_MANY_TIMES)
  void pair() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp(KEY_1, null));
    var prop2 = registry.bind(new TestIntProp(KEY_2, null));

    StoreAllUniqueValuesConsumer<Pair<Integer, Integer>> consumer =
        spy(new StoreAllUniqueValuesConsumer<>());
    var prop = Group.of(prop1, prop2);
    prop.subscribe(consumer, (ignored) -> {});

    var expected = Tuple.of(1, 2);

    // ACT
    source.put(KEY_1, "1");
    source.put(KEY_2, "2");

    // ASSERT
    await().until(consumer::get, hasItem(expected));

    var last = consumer.getLast();
    assertThat("Last notification should be a complete pair", last, equalTo(expected));
  }

  @RepeatedTest(value = HOW_MANY_TIMES)
  void triple() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp(KEY_1, null));
    var prop2 = registry.bind(new TestIntProp(KEY_2, null));
    var prop3 = registry.bind(new TestIntProp(KEY_3, null));

    StoreAllUniqueValuesConsumer<Triple<Integer, Integer, Integer>> consumer =
        spy(new StoreAllUniqueValuesConsumer<>());
    var prop = Group.of(prop1, prop2, prop3);
    prop.subscribe(consumer, (ignored) -> {});

    var expected = Tuple.of(1, 2, 3);

    // ACT
    source.put(KEY_1, "1");
    source.put(KEY_2, "2");
    source.put(KEY_3, "3");

    // ASSERT
    await().until(consumer::get, hasItem(expected));

    var last = consumer.getLast();
    assertThat("Last notification should be a complete triple", last, equalTo(expected));
  }

  @RepeatedTest(value = HOW_MANY_TIMES)
  void quad() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp(KEY_1, null));
    var prop2 = registry.bind(new TestIntProp(KEY_2, null));
    var prop3 = registry.bind(new TestIntProp(KEY_3, null));
    var prop4 = registry.bind(new TestIntProp(KEY_4, null));

    StoreAllUniqueValuesConsumer<Quad<Integer, Integer, Integer, Integer>> consumer =
        spy(new StoreAllUniqueValuesConsumer<>());
    var prop = Group.of(prop1, prop2, prop3, prop4);
    prop.subscribe(consumer, (ignored) -> {});

    var expected = Tuple.of(1, 2, 3, 4);

    // ACT
    source.put(KEY_1, "1");
    source.put(KEY_2, "2");
    source.put(KEY_3, "3");
    source.put(KEY_4, "4");

    // ASSERT
    await().until(consumer::get, hasItem(expected));

    var last = consumer.getLast();
    assertThat("Last notification should be a complete quad", last, equalTo(expected));
  }

  @RepeatedTest(value = HOW_MANY_TIMES)
  void quintuple() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp(KEY_1, null));
    var prop2 = registry.bind(new TestIntProp(KEY_2, null));
    var prop3 = registry.bind(new TestIntProp(KEY_3, null));
    var prop4 = registry.bind(new TestIntProp(KEY_4, null));
    var prop5 = registry.bind(new TestIntProp(KEY_5, null));

    StoreAllUniqueValuesConsumer<Quintuple<Integer, Integer, Integer, Integer, Integer>> consumer =
        spy(new StoreAllUniqueValuesConsumer<>());
    var prop = Group.of(prop1, prop2, prop3, prop4, prop5);
    prop.subscribe(consumer, (ignored) -> {});

    var expected = Tuple.of(1, 2, 3, 4, 5);

    // ACT
    source.put(KEY_1, "1");
    source.put(KEY_2, "2");
    source.put(KEY_3, "3");
    source.put(KEY_4, "4");
    source.put(KEY_5, "5");

    // ASSERT
    await().until(consumer::get, hasItem(expected));

    var last = consumer.getLast();
    assertThat("Last notification should be a complete quintuple", last, equalTo(expected));
  }
}
