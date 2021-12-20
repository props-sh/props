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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static sh.props.sources.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import sh.props.converters.Cast;
import sh.props.exceptions.ValueCannotBeReadException;
import sh.props.exceptions.ValueCannotBeSetException;
import sh.props.sources.InMemory;
import sh.props.textfixtures.TestErrorOnSetProp;

@SuppressWarnings({"NullAway", "PMD.JUnitTestContainsTooManyAsserts"})
class CustomPropTest {
  private static final String KEY = "key";

  @Test
  void secretProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop =
        new CustomPropBuilder<>(registry, Cast.asString())
            .defaultValue("A_SECRET")
            .secret(true)
            .build(KEY);

    // ACT / ASSERT
    assertThat("Expecting value to resolve", prop.get(), equalTo("A_SECRET"));
    assertThat(
        "Expecting the secret to not be exposed", prop.toString(), not(containsString("A_SECRET")));
  }

  @Test
  void propWithDescription() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop =
        new CustomPropBuilder<>(registry, Cast.asString()).description("a_description").build(KEY);

    // ACT / ASSERT
    assertThat("Expecting a description", prop.description(), equalTo("a_description"));
  }

  @Test
  void requiredProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop = new CustomPropBuilder<>(registry, Cast.asString()).required(true).build(KEY);

    // ACT / ASSERT
    assertThrows(ValueCannotBeReadException.class, prop::get);

    source.put(KEY, "value");
    await().until(prop::get, equalTo("value"));
  }

  @Test
  void setError() {

    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    AtomicReference<Throwable> capture = new AtomicReference<>();

    var prop = registry.bind(new TestErrorOnSetProp(KEY, 0));
    prop.subscribe((ignore) -> {}, capture::set);

    // ACT / ASSERT
    assertThat("Expecting a non-error state", prop.get(), equalTo(0));

    source.put(KEY, "2");
    await().until(capture::get, instanceOf(ValueCannotBeSetException.class));
  }
}
