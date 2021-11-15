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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;
import static sh.props.source.impl.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import org.junit.jupiter.api.RepeatedTest;
import sh.props.group.Group;
import sh.props.source.impl.InMemory;
import sh.props.testhelpers.AwaitAssertionTest;
import sh.props.testhelpers.StoreAllValuesConsumer;
import sh.props.testhelpers.TestIntProp;
import sh.props.tuples.Pair;
import sh.props.tuples.Quad;
import sh.props.tuples.Triple;
import sh.props.tuples.Tuple;

@SuppressWarnings({"NullAway", "checkstyle:VariableDeclarationUsageDistance"})
public class CoordinatedAsyncTest extends AwaitAssertionTest {
  public static final int HOW_MANY_TIMES = 1000;

  @RepeatedTest(value = HOW_MANY_TIMES)
  void synchronizedPairOfProps() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp("key1", null));
    var prop2 = registry.bind(new TestIntProp("key2", null));

    StoreAllValuesConsumer<Pair<Integer, Integer>> consumer = spy(new StoreAllValuesConsumer<>());
    var prop = Group.of(prop1, prop2);
    prop.subscribe(consumer, (ignored) -> {});

    var expected = Tuple.of(1, 2);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");

    // ASSERT
    await().until(consumer::get, hasItem(expected));

    var last = consumer.getLast();
    assertThat("Last notification should be a complete value", last, equalTo(expected));
  }

  @RepeatedTest(value = HOW_MANY_TIMES)
  void synchronizedTripleOfProps() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp("key1", null));
    var prop2 = registry.bind(new TestIntProp("key2", null));
    var prop3 = registry.bind(new TestIntProp("key3", null));

    StoreAllValuesConsumer<Triple<Integer, Integer, Integer>> consumer =
        spy(new StoreAllValuesConsumer<>());
    var prop = Group.of(prop1, prop2, prop3);
    prop.subscribe(consumer, (ignored) -> {});

    var expected = Tuple.of(1, 2, 3);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");

    // ASSERT
    await().until(consumer::get, hasItem(expected));

    var last = consumer.getLast();
    assertThat("Last notification should be a complete value", last, equalTo(expected));
  }

  @RepeatedTest(value = HOW_MANY_TIMES)
  void synchronizedQuadOfProps() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp("key1", null));
    var prop2 = registry.bind(new TestIntProp("key2", null));
    var prop3 = registry.bind(new TestIntProp("key3", null));
    var prop4 = registry.bind(new TestIntProp("key4", null));

    StoreAllValuesConsumer<Quad<Integer, Integer, Integer, Integer>> consumer =
        spy(new StoreAllValuesConsumer<>());
    var prop = Group.of(prop1, prop2, prop3, prop4);
    prop.subscribe(consumer, (ignored) -> {});

    var expected = Tuple.of(1, 2, 3, 4);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");
    source.put("key4", "4");

    // ASSERT
    await().until(consumer::get, hasItem(expected));

    var last = consumer.getLast();
    assertThat("Last notification should be a complete value", last, equalTo(expected));
  }

  @RepeatedTest(value = HOW_MANY_TIMES)
  void synchronizedTupleOfProps() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestIntProp("key1", null));
    var prop2 = registry.bind(new TestIntProp("key2", null));
    var prop3 = registry.bind(new TestIntProp("key3", null));
    var prop4 = registry.bind(new TestIntProp("key4", null));
    var prop5 = registry.bind(new TestIntProp("key5", null));

    StoreAllValuesConsumer<Tuple<Integer, Integer, Integer, Integer, Integer>> consumer =
        spy(new StoreAllValuesConsumer<>());
    var prop = Group.of(prop1, prop2, prop3, prop4, prop5);
    prop.subscribe(consumer, (ignored) -> {});

    var expected = Tuple.of(1, 2, 3, 4, 5);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");
    source.put("key4", "4");
    source.put("key5", "5");

    // ASSERT
    await().until(consumer::get, hasItem(expected));

    var last = consumer.getLast();
    assertThat("Last notification should be a complete value", last, equalTo(expected));
  }
}
