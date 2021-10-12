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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sh.props.Coordinated.PairSupplier;
import sh.props.converter.IntegerConverter;
import sh.props.source.impl.InMemory;
import sh.props.tuples.Pair;
import sh.props.tuples.Tuple;

@SuppressWarnings("NullAway")
class RegistryTest {

  @Test
  void updateValue() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put("key", "value");

    Registry registry = new RegistryBuilder().withSource(source).build();

    // ACT
    source.put("key", "value2");
    source.updateSubscribers();

    // ASSERT
    assertThat(registry.get("key", String.class), equalTo("value2"));
  }

  @Test
  void updateValueButNotSubscribers() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put("key", "value");

    Registry registry = new RegistryBuilder().withSource(source).build();

    // ACT
    source.put("key", "value2");

    // ASSERT
    assertThat(registry.get("key", String.class), equalTo("value"));
  }

  @Test
  void cannotCreateRegistryWithoutSources() {
    // ASSERT
    Assertions.assertThrows(
        IllegalStateException.class,
        () -> {
          // ACT
          new RegistryBuilder().build();
        });
  }

  @Test
  void propCanBeBoundAndItsValueIsSet() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put("key", "1");

    Registry registry = new RegistryBuilder().withSource(source).build();

    // ACT
    Prop<Integer> prop = new IntProp("key", null);
    registry.bind(prop);

    // ASSERT
    assertThat(prop.value(), equalTo(1));
  }

  @Test
  void propCanBeBoundAndUpdated() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put("key", "1");

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop = new IntProp("key", null);
    registry.bind(prop);

    // ACT
    source.put("key", "2");
    source.updateSubscribers();

    // ASSERT
    await().atMost(5, SECONDS).until(prop::value, equalTo(2));
  }

  @Test
  void autoUpdated() {
    // ARRANGE
    InMemory source = new InMemory(true);
    source.put("key", "value");

    Registry registry = new RegistryBuilder().withSource(source).build();

    // ACT
    source.put("key", "value2");

    // ASSERT
    assertThat(registry.get("key", String.class), equalTo("value2"));
  }

  @Test
  void manuallyUpdated() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put("key", "value");

    Registry registry = new RegistryBuilder().withSource(source).build();

    // ACT
    source.put("key", "value2");

    // ASSERT
    assertThat(registry.get("key", String.class), equalTo("value"));
  }

  @Test
  void propBoundAndReceivesAsyncUpdates() {
    // ARRANGE
    InMemory source = new InMemory(true);

    AtomicInteger localValue = new AtomicInteger(0);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop = new IntProp("key", null);
    registry.bind(prop);

    prop.subscribe(localValue::set, RegistryTest::ignoreErrors);

    // ACT
    source.put("key", "2");

    // ASSERT
    await().atMost(5, SECONDS).until(localValue::get, equalTo(2));
  }

  @Test
  void bindMultipleProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    AtomicInteger localValue1 = new AtomicInteger(0);
    Prop<Integer> prop1 = new IntProp("key", null);
    registry.bind(prop1);
    prop1.subscribe(localValue1::set, RegistryTest::ignoreErrors);

    AtomicInteger localValue2 = new AtomicInteger(0);
    Prop<Integer> prop2 = new IntProp("key", null);
    registry.bind(prop2);
    prop2.subscribe(localValue2::set, RegistryTest::ignoreErrors);

    // ACT
    source.put("key", "2");

    // ASSERT
    await().atMost(5, SECONDS).until(localValue1::get, equalTo(2));
    await().atMost(5, SECONDS).until(localValue2::get, equalTo(2));
  }

  @Test
  void coordinatePairOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    Prop<Integer> prop1 = new IntProp("key1", null);
    registry.bind(prop1);

    Prop<Integer> prop2 = new IntProp("key2", null);
    registry.bind(prop2);

    AtomicReference<Pair<Integer, Integer>> result = new AtomicReference<>();
    PairSupplier<Integer, Integer> supplier = Coordinated.coordinate(prop1, prop2);
    supplier.subscribe(result::set, RegistryTest::ignoreErrors);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");

    // ASSERT
    await().atMost(5, SECONDS).until(result::get, equalTo(Tuple.of(1, 2)));
  }

  private static void ignoreErrors(Throwable t) {
    // do nothing
  }

  private static class IntProp extends Prop<Integer> implements IntegerConverter {

    protected IntProp(String key, Integer defaultValue) {
      super(key, defaultValue, null, false, false);
    }
  }
}
