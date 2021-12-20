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
import static sh.props.sources.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sh.props.converters.Cast;
import sh.props.sources.InMemory;
import sh.props.textfixtures.AwaitAssertionTest;
import sh.props.textfixtures.TestIntProp;

class RegistryTest extends AwaitAssertionTest {
  private static final String KEY = "key";
  private static final String VALUE_1 = "value1";
  private static final String VALUE_2 = "value2";

  @Test
  void updateValue() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put(KEY, VALUE_1);

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    source.put(KEY, VALUE_2);
    source.refresh();

    // ASSERT
    assertThat(
        "Expecting the registry to have an updated value for the key",
        registry.get(KEY, Cast.asString()),
        equalTo(VALUE_2));
  }

  @Test
  void updateValueButNotSubscribers() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put(KEY, VALUE_1);

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    source.put(KEY, VALUE_2);

    // ASSERT
    assertThat(
        "Expecting the registry to not have updated the value",
        registry.get(KEY, Cast.asString()),
        equalTo(VALUE_1));
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
    source.put(KEY, "1");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = new TestIntProp(KEY, null);
    registry.bind(prop);

    // ASSERT
    assertThat(
        "Expecting a prop to get its original value, when bound to a Registry",
        prop.get(),
        equalTo(1));
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // PMD doesn't support awaitility
  void propCanBeBoundAndUpdated() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put(KEY, "1");

    Registry registry = new RegistryBuilder(source).build();

    var prop = new TestIntProp(KEY, null);
    registry.bind(prop);

    // ACT
    source.put(KEY, "2");
    source.refresh();

    // ASSERT
    // expecting a Prop to eventually receive a value update
    await().until(prop::get, equalTo(2));
  }

  @Test
  void autoUpdated() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, VALUE_1);

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    source.put(KEY, VALUE_2);

    // ASSERT
    assertThat(
        "Expecting the Registry to automatically update values",
        registry.get(KEY, Cast.asString()),
        equalTo(VALUE_2));
  }

  @Test
  void manuallyUpdated() {
    // ARRANGE
    InMemory source = new InMemory();
    source.put(KEY, VALUE_1);

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    source.put(KEY, VALUE_2);

    // ASSERT
    assertThat(
        "Expecting the Registry to not be aware of Source updates, before they are advertised",
        registry.get(KEY, Cast.asString()),
        equalTo(VALUE_1));
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // PMD doesn't support awaitility
  void propBoundAndReceivesAsyncUpdates() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop = new TestIntProp(KEY, null);
    registry.bind(prop);

    AtomicInteger localValue = new AtomicInteger(0);
    prop.subscribe(localValue::set, (ignored) -> {});

    // ACT
    source.put(KEY, "2");

    // ASSERT
    // expecting Prop subscribers to receive value updates
    await().until(localValue::get, equalTo(2));
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // PMD doesn't support awaitility
  void bindMultipleProps() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    AtomicInteger localValue1 = new AtomicInteger(0);
    var prop1 = new TestIntProp(KEY, null);
    registry.bind(prop1);
    prop1.subscribe(localValue1::set, (ignored) -> {});

    AtomicInteger localValue2 = new AtomicInteger(0);
    var prop2 = new TestIntProp(KEY, null);
    registry.bind(prop2);
    prop2.subscribe(localValue2::set, (ignored) -> {});

    // ACT
    source.put(KEY, "2");

    // ASSERT
    // expecting all bound Props to receive value updates
    await().until(localValue1::get, equalTo(2));
    await().until(localValue2::get, equalTo(2));
  }

  @Test
  void customPropCreatedWithBuilder() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop = registry.builder(Cast.asInteger()).defaultValue(1).build(KEY);

    // ACT / ASSERT
    assertThat("Expecting the prop to use its default value", prop.get(), equalTo(1));

    source.put(KEY, "2");
    // expecting the Prop to use its set value, after a value update
    await().until(prop::get, equalTo(2));
  }

  @Test
  @SuppressWarnings("NullAway") // explicitly sending nulls to emulate bad user input
  void getFromLayer() {
    // ARRANGE
    var source1 = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    var source2 = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    var registry =
        new RegistryBuilder().withSource(source1, "mem1").withSource(source2, "mem2").build();

    source1.put(KEY, "1");
    source2.put(KEY, "2");

    // ACT // ASSERT
    assertThat(
        "Expecting values to be returned from the effective (Layer) owner",
        registry.get(KEY, Cast.asInteger()),
        equalTo(2));
    assertThat(
        "Expecting values to be returned from the effective (Layer) owner",
        registry.get(KEY, Cast.asInteger(), null),
        equalTo(2));

    assertThat(
        "Expecting values to be returned from the desired Layer",
        registry.get(KEY, Cast.asInteger(), "mem1"),
        equalTo(1));
    assertThat(
        "Expecting values to be returned from the desired Layer",
        registry.get(KEY, Cast.asInteger(), "mem2"),
        equalTo(2));
  }

  @Test
  @SuppressWarnings("NullAway") // explicitly sending nulls to emulate bad user input
  void nullSafetyChecks() {
    // ARRANGE
    var source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    var registry = new RegistryBuilder().withSource(source, "mem").build();

    source.put(KEY, "1");

    // ACT // ASSERT
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> registry.get(null, Cast.asInteger(), "mem"),
        "key cannot be null");
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> registry.get(null, Cast.asInteger()),
        "key cannot be null");
  }
}
