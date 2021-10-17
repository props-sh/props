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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import sh.props.converter.IntegerConverter;
import sh.props.source.impl.InMemory;
import sh.props.tuples.Quad;
import sh.props.tuples.Tuple;

@SuppressWarnings({"NullAway", "checkstyle:VariableDeclarationUsageDistance"})
class CoordinatedAsyncTest {

  @Test
  @Disabled
  // TODO: fix the impl.
  void coordinatePairOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop1 = registry.bind(new IntProp("key1", null));
    Prop<Integer> prop2 = registry.bind(new IntProp("key2", null));

    var result = new AtomicReference<>();
    var supplier = Coordinated.coordinate(prop1, prop2);
    supplier.subscribe(result::set, CoordinatedAsyncTest::ignoreErrors);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");

    // ASSERT
    await().atMost(5, SECONDS).until(result::get, equalTo(Tuple.of(1, 2)));
  }

  @Test
  @Disabled
  // TODO: fix the impl.
  void coordinateTripleOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop1 = registry.bind(new IntProp("key1", null));
    Prop<Integer> prop2 = registry.bind(new IntProp("key2", null));
    Prop<Integer> prop3 = registry.bind(new IntProp("key3", null));

    var result = new AtomicReference<>();
    var supplier = Coordinated.coordinate(prop1, prop2, prop3);
    supplier.subscribe(result::set, CoordinatedAsyncTest::ignoreErrors);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");

    // ASSERT
    await().atMost(5, SECONDS).until(result::get, equalTo(Tuple.of(1, 2, 3)));
  }

  @Test
  void coordinateQuadOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop1 = registry.bind(new IntProp("key1", null));
    Prop<Integer> prop2 = registry.bind(new IntProp("key2", null));
    Prop<Integer> prop3 = registry.bind(new IntProp("key3", null));
    Prop<Integer> prop4 = registry.bind(new IntProp("key4", null));

    SpyConsumer consumer = spy(new SpyConsumer());
    var supplier = Coordinated.coordinate(prop1, prop2, prop3, prop4);
    supplier.subscribe(consumer, CoordinatedAsyncTest::ignoreErrors);

    var expected = Tuple.of(1, 2, 3, 4);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");
    source.put("key4", "4");

    // ASSERT
    verify(consumer, timeout(10_000).atLeastOnce()).accept(expected);

    var quad = CoordinatedAsyncTest.getLast(consumer);
    try {
      assertThat("Last notification should be a complete value", quad, equalTo(expected));
    } catch (AssertionError e) {
      for (int i = 0; i < consumer.collector.size(); i++) {
        System.out.println(consumer.collector.get(i));
      }
      throw e;
    }
  }

  private static Quad<Integer, Integer, Integer, Integer> getLast(SpyConsumer consumer) {
    synchronized (consumer) {
      Quad<Integer, Integer, Integer, Integer> result = consumer.collector.peek();
      System.out.printf("Returned result %s (index=%d)\n", result, consumer.collector.size() - 1);
      return result;
    }
    //    Quad<Integer, Integer, Integer, Integer> var = null;
    //    Quad<Integer, Integer, Integer, Integer> last = null;
    //    while ((var = consumer.collector.poll()) != null) {
    //      last = var;
    //      System.out.printf("%d: %s%s", System.nanoTime(), last, System.lineSeparator());
    //      System.out.flush();
    //    }
    //    return last;
  }

  private static class SpyConsumer implements Consumer<Quad<Integer, Integer, Integer, Integer>> {

    @SuppressWarnings("JdkObsolete")
    final Stack<Quad<Integer, Integer, Integer, Integer>> collector = new Stack<>();

    @Override
    public void accept(Quad<Integer, Integer, Integer, Integer> quad) {
      synchronized (this) {
        this.collector.add(quad);
        System.out.printf("Accepted value %s (index=%d)\n", quad, this.collector.size() - 1);
      }
    }
  }

  private static void ignoreErrors(Throwable t) {
    // do nothing
    t.printStackTrace();
  }

  private static class IntProp extends Prop<Integer> implements IntegerConverter {

    protected IntProp(String key, Integer defaultValue) {
      super(key, defaultValue, null, false, false);
    }
  }
}
