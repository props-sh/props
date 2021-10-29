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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import sh.props.converter.IntegerConverter;
import sh.props.group.Group;
import sh.props.interfaces.Prop;
import sh.props.source.impl.InMemory;
import sh.props.tuples.Pair;
import sh.props.tuples.Quad;
import sh.props.tuples.Triple;
import sh.props.tuples.Tuple;

@SuppressWarnings({"NullAway", "checkstyle:VariableDeclarationUsageDistance"})
public class CoordinatedAsyncTest {
  public static final int HOW_MANY_TIMES = 10000;

  private static void ignoreErrors(Throwable t) {
    // do nothing
    t.printStackTrace();
  }

  @RepeatedTest(value = HOW_MANY_TIMES)
  //  @Test
  void synchronizedPairOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop1 = registry.bind(new IntProp("key1", null));
    Prop<Integer> prop2 = registry.bind(new IntProp("key2", null));

    DummyConsumer<Pair<Integer, Integer>> consumer = spy(new DummyConsumer<>());
    var prop = Group.of(prop1, prop2);
    prop.subscribe(consumer, CoordinatedAsyncTest::ignoreErrors);

    var expected = Tuple.of(1, 2);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");

    // ASSERT
    await()
        .pollInterval(Duration.ofNanos(1000))
        .atMost(5, SECONDS)
        .until(consumer::get, hasItem(expected));

    var last = consumer.getLast();
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));

    Assertions.assertEquals(expected.first, last.first, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    Assertions.assertEquals(expected.second, last.second, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    assertThat("Last notification should be a complete value", last, equalTo(expected));
  }

  @RepeatedTest(value = HOW_MANY_TIMES)
  //  @Test
  void synchronizedTripleOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop1 = registry.bind(new IntProp("key1", null));
    Prop<Integer> prop2 = registry.bind(new IntProp("key2", null));
    Prop<Integer> prop3 = registry.bind(new IntProp("key3", null));

    DummyConsumer<Triple<Integer, Integer, Integer>> consumer = spy(new DummyConsumer<>());
    var prop = Group.of(prop1, prop2, prop3);
    prop.subscribe(consumer, CoordinatedAsyncTest::ignoreErrors);

    var expected = Tuple.of(1, 2, 3);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");

    // ASSERT
    await()
        .pollInterval(Duration.ofNanos(1000))
        .atMost(5, SECONDS)
        .until(consumer::get, hasItem(expected));

    var last = consumer.getLast();
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));

    Assertions.assertEquals(expected.first, last.first, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    Assertions.assertEquals(expected.second, last.second, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    Assertions.assertEquals(expected.third, last.third, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    assertThat("Last notification should be a complete value", last, equalTo(expected));
  }

  @RepeatedTest(value = HOW_MANY_TIMES)
  //  @Test
  void synchronizedQuadOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop1 = registry.bind(new IntProp("key1", null));
    Prop<Integer> prop2 = registry.bind(new IntProp("key2", null));
    Prop<Integer> prop3 = registry.bind(new IntProp("key3", null));
    Prop<Integer> prop4 = registry.bind(new IntProp("key4", null));

    DummyConsumer<Quad<Integer, Integer, Integer, Integer>> consumer = spy(new DummyConsumer<>());
    var prop = Group.of(prop1, prop2, prop3, prop4);
    prop.subscribe(consumer, CoordinatedAsyncTest::ignoreErrors);

    var expected = Tuple.of(1, 2, 3, 4);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");
    source.put("key4", "4");

    // ASSERT
    await()
        .pollInterval(Duration.ofNanos(1000))
        .atMost(5, SECONDS)
        .until(consumer::get, hasItem(expected));

    var last = consumer.getLast();
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));

    Assertions.assertEquals(expected.first, last.first, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    Assertions.assertEquals(expected.second, last.second, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    Assertions.assertEquals(expected.third, last.third, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    Assertions.assertEquals(expected.fourth, last.fourth, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    assertThat("Last notification should be a complete value", last, equalTo(expected));
  }

  @RepeatedTest(value = HOW_MANY_TIMES)
  //  @Test
  void synchronizedTupleOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop1 = registry.bind(new IntProp("key1", null));
    Prop<Integer> prop2 = registry.bind(new IntProp("key2", null));
    Prop<Integer> prop3 = registry.bind(new IntProp("key3", null));
    Prop<Integer> prop4 = registry.bind(new IntProp("key4", null));
    Prop<Integer> prop5 = registry.bind(new IntProp("key5", null));

    DummyConsumer<Tuple<Integer, Integer, Integer, Integer, Integer>> consumer =
        spy(new DummyConsumer<>());
    var prop = Group.of(prop1, prop2, prop3, prop4, prop5);
    prop.subscribe(consumer, CoordinatedAsyncTest::ignoreErrors);

    var expected = Tuple.of(1, 2, 3, 4, 5);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");
    source.put("key4", "4");
    source.put("key5", "5");

    // ASSERT
    await()
        .pollInterval(Duration.ofNanos(1000))
        .atMost(5, SECONDS)
        .until(consumer::get, hasItem(expected));

    var last = consumer.getLast();
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));

    Assertions.assertEquals(expected.first, last.first, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    Assertions.assertEquals(expected.second, last.second, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    Assertions.assertEquals(expected.third, last.third, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    Assertions.assertEquals(expected.fourth, last.fourth, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    Assertions.assertEquals(expected.fifth, last.fifth, () -> "Expected complete: " + last);
    System.out.println("compare: " + last + " " + System.identityHashCode(last));
    System.out.println("expected: " + expected + " " + System.identityHashCode(expected));
    assertThat("Last notification should be a complete value", last, equalTo(expected));
  }

  private static class DummyConsumer<T> implements Consumer<T> {
    private final LinkedHashSet<T> store = new LinkedHashSet<>();

    @Override
    public synchronized void accept(T t) {
      this.store.add(t);
    }

    public synchronized ArrayDeque<T> get() {
      return new ArrayDeque<>(this.store);
    }

    public synchronized T getLast() {
      System.out.println("before " + this.store.size());
      System.out.println(this.store);

      T val = null;
      try {
        Iterator<T> it = this.store.iterator();
        while (it.hasNext()) {
          val = it.next();
        }
        return val;

      } finally {
        System.out.println("after " + this.store.size());
        System.out.println(
            this.store.stream()
                .map(v -> v.toString() + " " + System.identityHashCode(v))
                .collect(Collectors.toList()));
        System.out.println("chosen: " + val + " " + System.identityHashCode(val));
      }
    }
  }

  private static class IntProp extends CustomProp<Integer> implements IntegerConverter {

    protected IntProp(String key, Integer defaultValue) {
      super(key, defaultValue, null, false, false);
    }
  }
}
