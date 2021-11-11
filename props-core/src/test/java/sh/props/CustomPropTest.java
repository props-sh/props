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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static sh.props.source.impl.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import sh.props.converter.Cast;
import sh.props.exceptions.InvalidReadOpException;
import sh.props.exceptions.InvalidUpdateOpException;
import sh.props.source.impl.InMemory;
import sh.props.testhelpers.TestErrorOnSetProp;

@SuppressWarnings("NullAway")
class CustomPropTest {

  @Test
  void secretProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop =
        new CustomPropBuilder<>(registry, Cast.asString())
            .defaultValue("A_SECRET")
            .secret(true)
            .build("key");

    // ACT / ASSERT
    assertThat(prop.get(), equalTo("A_SECRET"));
    assertThat(prop.toString(), not(containsString("A_SECRET")));
  }

  @Test
  void propWithDescription() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop =
        new CustomPropBuilder<>(registry, Cast.asString())
            .description("a_description")
            .build("key");

    // ACT / ASSERT
    assertThat(prop.description(), equalTo("a_description"));
  }

  @Test
  void requiredProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop = new CustomPropBuilder<>(registry, Cast.asString()).required(true).build("key");

    // ACT / ASSERT
    assertThrows(InvalidReadOpException.class, prop::get);

    source.put("key", "value");
    await().until(prop::get, equalTo("value"));
  }

  @Test
  void setError() {

    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    AtomicReference<Throwable> capture = new AtomicReference<>();

    var prop = registry.bind(new TestErrorOnSetProp("key", 0));
    prop.subscribe((ignore) -> {}, capture::set);

    // ACT / ASSERT
    assertThat(prop.get(), equalTo(0));

    source.put("key", "2");
    await().until(capture::get, instanceOf(InvalidUpdateOpException.class));
  }
}
