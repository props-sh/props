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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static sh.props.sources.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;
import static sh.props.textfixtures.ExpectedExceptionMatcher.hasExceptionMessage;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import sh.props.exceptions.ValueCannotBeReadException;
import sh.props.exceptions.ValueCannotBeSetException;
import sh.props.group.Group;
import sh.props.sources.InMemory;
import sh.props.textfixtures.TestErrorOnGetProp;
import sh.props.textfixtures.TestErrorOnSetProp;
import sh.props.tuples.Tuple;

@SuppressWarnings("NullAway") // NullAway does not support awaitility
class PropGroupExceptionTest {

  private static final String SPECIFIC_EXCEPTION_MESSAGE = "Expecting a specific error message";
  private static final String KEY_1 = "key1";
  private static final String KEY_2 = "key2";
  private static final String KEY_3 = "key3";

  @Test
  void pair() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestErrorOnGetProp(KEY_1, null));
    var prop2 = registry.bind(new TestErrorOnGetProp(KEY_2, null));

    AtomicReference<Throwable> capture = new AtomicReference<>();
    var supplier = Group.of(prop1, prop2);
    supplier.subscribe(ignore -> {}, capture::set);

    source.put(KEY_1, "1");
    source.put(KEY_2, "1");

    // ACT / ASSERT
    await().until(supplier::get, equalTo(Tuple.of(1, 1)));

    source.put(KEY_1, "2");
    await().until(capture::get, hasExceptionMessage(TestErrorOnGetProp.errorMessage(2)));
    assertThat(
        SPECIFIC_EXCEPTION_MESSAGE,
        capture.get().getMessage(),
        equalTo(TestErrorOnGetProp.errorMessage(2)));
    assertThrows(ValueCannotBeReadException.class, supplier::get);

    source.put(KEY_2, "3");
    await().until(capture::get, hasExceptionMessage(TestErrorOnGetProp.errorMessage(3)));
    assertThat(
        SPECIFIC_EXCEPTION_MESSAGE,
        capture.get().getMessage(),
        equalTo(TestErrorOnGetProp.errorMessage(3)));
    assertThrows(ValueCannotBeReadException.class, supplier::get);
  }

  @Test
  void pairSet() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestErrorOnSetProp(KEY_1, null));
    var prop2 = registry.bind(new TestErrorOnSetProp(KEY_2, null));

    AtomicReference<Throwable> capture = new AtomicReference<>();
    var supplier = Group.of(prop1, prop2);
    supplier.subscribe(ignore -> {}, capture::set);

    source.put(KEY_1, "1");
    source.put(KEY_2, "1");

    // ACT / ASSERT
    await().until(supplier::get, equalTo(Tuple.of(1, 1)));

    source.put(KEY_2, "2");
    await().until(capture::get, hasExceptionMessage(TestErrorOnSetProp.errorMessage(2)));
    assertThat(
        SPECIFIC_EXCEPTION_MESSAGE,
        capture.get().getMessage(),
        equalTo(TestErrorOnSetProp.errorMessage(2)));
    assertThrows(ValueCannotBeSetException.class, supplier::get);

    source.put(KEY_1, "3");
    await().until(capture::get, hasExceptionMessage(TestErrorOnSetProp.errorMessage(3)));
    assertThat(
        SPECIFIC_EXCEPTION_MESSAGE,
        capture.get().getMessage(),
        equalTo(TestErrorOnSetProp.errorMessage(3)));
    assertThrows(ValueCannotBeSetException.class, supplier::get);
  }

  @Test
  // breaking this test down to avoid multiple assertions, would not provide a net positive benefit
  @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
  void triple() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestErrorOnGetProp(KEY_1, null));
    var prop2 = registry.bind(new TestErrorOnGetProp(KEY_2, null));
    var prop3 = registry.bind(new TestErrorOnGetProp(KEY_3, null));

    AtomicReference<Throwable> capture = new AtomicReference<>();
    var supplier = Group.of(prop1, prop2, prop3);
    supplier.subscribe(ignore -> {}, capture::set);

    source.put(KEY_1, "1");
    source.put(KEY_2, "1");
    source.put(KEY_3, "1");

    // ACT / ASSERT
    await().until(supplier::get, equalTo(Tuple.of(1, 1, 1)));

    source.put(KEY_1, "2");
    await().until(capture::get, hasExceptionMessage(TestErrorOnGetProp.errorMessage(2)));
    assertThat(
        SPECIFIC_EXCEPTION_MESSAGE,
        capture.get().getMessage(),
        equalTo(TestErrorOnGetProp.errorMessage(2)));
    assertThrows(ValueCannotBeReadException.class, supplier::get);

    source.put(KEY_2, "3");
    await().until(capture::get, hasExceptionMessage(TestErrorOnGetProp.errorMessage(3)));
    assertThat(
        SPECIFIC_EXCEPTION_MESSAGE,
        capture.get().getMessage(),
        equalTo(TestErrorOnGetProp.errorMessage(3)));
    assertThrows(ValueCannotBeReadException.class, supplier::get);

    source.put(KEY_3, "4");
    await().until(capture::get, hasExceptionMessage(TestErrorOnGetProp.errorMessage(4)));
    assertThat(
        SPECIFIC_EXCEPTION_MESSAGE,
        capture.get().getMessage(),
        equalTo(TestErrorOnGetProp.errorMessage(4)));
    assertThrows(ValueCannotBeReadException.class, supplier::get);
  }
}
