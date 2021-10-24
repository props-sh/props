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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sh.props.converter.IntegerConverter;
import sh.props.source.impl.InMemory;
import sh.props.sync.Synchronize;
import sh.props.tuples.Pair;
import sh.props.tuples.Quad;
import sh.props.tuples.Triple;
import sh.props.tuples.Tuple;

@SuppressWarnings({"NullAway", "checkstyle:VariableDeclarationUsageDistance"})
class CoordinatedAsyncTest {

  private static <T> T getLast(List<T> lst) {
    if (lst.isEmpty()) {
      return null;
    }

    return lst.get(lst.size() - 1);
  }

  private static void ignoreErrors(Throwable t) {
    // do nothing
    t.printStackTrace();
  }

  @Test
  void synchronizedPairOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop1 = registry.bind(new IntProp("key1", null));
    Prop<Integer> prop2 = registry.bind(new IntProp("key2", null));

    Consumer<Pair<Integer, Integer>> consumer = spy(new DummyConsumer<>());
    var prop = Synchronize.props(prop1, prop2);
    prop.subscribe(consumer, CoordinatedAsyncTest::ignoreErrors);

    var expected = Tuple.of(1, 2);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");

    // ASSERT
    await()
        .pollInterval(Duration.ofNanos(1000))
        .atMost(5, SECONDS)
        .until(prop::get, equalTo(expected));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Pair<Integer, Integer>> captor = ArgumentCaptor.forClass(Pair.class);
    verify(consumer, atLeastOnce()).accept(captor.capture());

    var result = CoordinatedAsyncTest.getLast(captor.getAllValues());
    assertThat("Last notification should be a complete value", result, equalTo(expected));
  }

  @Test
  void synchronizedTripleOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop1 = registry.bind(new IntProp("key1", null));
    Prop<Integer> prop2 = registry.bind(new IntProp("key2", null));
    Prop<Integer> prop3 = registry.bind(new IntProp("key3", null));

    Consumer<Triple<Integer, Integer, Integer>> consumer = spy(new DummyConsumer<>());
    var prop = Synchronize.props(prop1, prop2, prop3);
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
        .until(prop::get, equalTo(expected));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Triple<Integer, Integer, Integer>> captor =
        ArgumentCaptor.forClass(Triple.class);
    verify(consumer, atLeastOnce()).accept(captor.capture());

    var result = CoordinatedAsyncTest.getLast(captor.getAllValues());
    assertThat("Last notification should be a complete value", result, equalTo(expected));
  }

  @Test
  void synchronizedQuadOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop1 = registry.bind(new IntProp("key1", null));
    Prop<Integer> prop2 = registry.bind(new IntProp("key2", null));
    Prop<Integer> prop3 = registry.bind(new IntProp("key3", null));
    Prop<Integer> prop4 = registry.bind(new IntProp("key4", null));

    Consumer<Quad<Integer, Integer, Integer, Integer>> consumer = spy(new DummyConsumer<>());
    var prop = Synchronize.props(prop1, prop2, prop3, prop4);
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
        .until(prop::get, equalTo(expected));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Quad<Integer, Integer, Integer, Integer>> captor =
        ArgumentCaptor.forClass(Quad.class);
    verify(consumer, atLeastOnce()).accept(captor.capture());

    var result = CoordinatedAsyncTest.getLast(captor.getAllValues());
    assertThat("Last notification should be a complete value", result, equalTo(expected));
  }

  @Test
  void synchronizedTupleOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop1 = registry.bind(new IntProp("key1", null));
    Prop<Integer> prop2 = registry.bind(new IntProp("key2", null));
    Prop<Integer> prop3 = registry.bind(new IntProp("key3", null));
    Prop<Integer> prop4 = registry.bind(new IntProp("key4", null));
    Prop<Integer> prop5 = registry.bind(new IntProp("key5", null));

    Consumer<Tuple<Integer, Integer, Integer, Integer, Integer>> consumer =
        spy(new DummyConsumer<>());
    var prop = Synchronize.props(prop1, prop2, prop3, prop4, prop5);
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
        .until(prop::get, equalTo(expected));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Tuple<Integer, Integer, Integer, Integer, Integer>> captor =
        ArgumentCaptor.forClass(Tuple.class);
    verify(consumer, atLeastOnce()).accept(captor.capture());

    var result = CoordinatedAsyncTest.getLast(captor.getAllValues());
    assertThat("Last notification should be a complete value", result, equalTo(expected));
  }

  private static class DummyConsumer<T> implements Consumer<T> {
    @Override
    public void accept(T t) {
      // no-op
    }
  }

  private static class IntProp extends BaseProp<Integer> implements IntegerConverter {

    protected IntProp(String key, Integer defaultValue) {
      super(key, defaultValue, null, false, false);
    }
  }
}
