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
import static org.hamcrest.MatcherAssert.assertThat;
import static sh.props.source.impl.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sh.props.converter.Cast;
import sh.props.source.impl.InMemory;
import sh.props.testhelpers.AwaitAssertionTest;
import sh.props.testhelpers.TestIntProp;

@SuppressWarnings("NullAway")
class RegistryTest extends AwaitAssertionTest {

  @Test
  void updateValue() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put("key", "value");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    source.put("key", "value2");
    source.updateSubscribers();

    // ASSERT
    assertThat(registry.get("key", Cast.asString()), equalTo("value2"));
  }

  @Test
  void updateValueButNotSubscribers() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put("key", "value");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    source.put("key", "value2");

    // ASSERT
    assertThat(registry.get("key", Cast.asString()), equalTo("value"));
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

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = new TestIntProp("key", null);
    registry.bind(prop);

    // ASSERT
    assertThat(prop.get(), equalTo(1));
  }

  @Test
  void propCanBeBoundAndUpdated() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put("key", "1");

    Registry registry = new RegistryBuilder(source).build();

    var prop = new TestIntProp("key", null);
    registry.bind(prop);

    // ACT
    source.put("key", "2");
    source.updateSubscribers();

    // ASSERT
    await().until(prop::get, equalTo(2));
  }

  @Test
  void autoUpdated() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put("key", "value");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    source.put("key", "value2");

    // ASSERT
    assertThat(registry.get("key", Cast.asString()), equalTo("value2"));
  }

  @Test
  void manuallyUpdated() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put("key", "value");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    source.put("key", "value2");

    // ASSERT
    assertThat(registry.get("key", Cast.asString()), equalTo("value"));
  }

  @Test
  void propBoundAndReceivesAsyncUpdates() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    AtomicInteger localValue = new AtomicInteger(0);

    Registry registry = new RegistryBuilder(source).build();

    var prop = new TestIntProp("key", null);
    registry.bind(prop);

    prop.subscribe(localValue::set, (ignored) -> {});

    // ACT
    source.put("key", "2");

    // ASSERT
    await().until(localValue::get, equalTo(2));
  }

  @Test
  void bindMultipleProps() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    AtomicInteger localValue1 = new AtomicInteger(0);
    var prop1 = new TestIntProp("key", null);
    registry.bind(prop1);
    prop1.subscribe(localValue1::set, (ignored) -> {});

    AtomicInteger localValue2 = new AtomicInteger(0);
    var prop2 = new TestIntProp("key", null);
    registry.bind(prop2);
    prop2.subscribe(localValue2::set, (ignored) -> {});

    // ACT
    source.put("key", "2");

    // ASSERT
    await().until(localValue1::get, equalTo(2));
    await().until(localValue2::get, equalTo(2));
  }

  @Test
  void customPropCreatedWithBuilder() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop = registry.builder(Cast.asInteger()).defaultValue(1).build("key");

    // ACT / ASSERT
    assertThat(prop.get(), equalTo(1));

    source.put("key", "2");
    await().until(prop::get, equalTo(2));
  }
}
