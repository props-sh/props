/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Mihai Bojin
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static sh.props.sources.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;
import static sh.props.textfixtures.ExpectedExceptionMatcher.hasExceptionMessage;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sh.props.exceptions.ValueCannotBeReadException;
import sh.props.exceptions.ValueCannotBeSetException;
import sh.props.sources.InMemory;
import sh.props.textfixtures.AwaitAssertionTest;
import sh.props.textfixtures.DummyConsumer;
import sh.props.textfixtures.TestErrorOnGetProp;
import sh.props.textfixtures.TestErrorOnSetProp;
import sh.props.textfixtures.TestIntProp;
import sh.props.textfixtures.TestStringProp;

class RefactoredPropTest extends AwaitAssertionTest {
  private static final String KEY_1 = "key1";
  private static final String KEY_2 = "key2";

  @Test
  void valueIsReturnedFromRefactoredProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestStringProp(KEY_1, null));
    var prop2 = registry.bind(new TestIntProp(KEY_2, null));

    DummyConsumer<Integer> initialized = spy(new DummyConsumer<>());
    prop2.subscribe(initialized, (ignore) -> {});

    DummyConsumer<Integer> consumer = spy(new DummyConsumer<>());
    var refactoredProp = new RefactoredProp<>(prop1, prop2, Integer::parseInt);

    refactoredProp.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put(KEY_1, "1");
    source.put(KEY_2, "2");

    // wait until the second prop is initialized
    verify(initialized, timeout(1_000)).accept(2);

    // ASSERT
    verify(consumer, timeout(1_000)).accept(2);

    assertThat("Expecting original prop value to be set", prop1.get(), equalTo("1"));
    assertThat("Expecting refactored prop value to be set", prop2.get(), equalTo(2));
    assertThat("Expecting the refactored value to be returned", refactoredProp.get(), equalTo(2));
  }

  @Test
  void valueIsReturnedFromOldProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestStringProp(KEY_1, null));
    var prop2 = registry.bind(new TestIntProp(KEY_2, null));

    DummyConsumer<Integer> consumer = spy(new DummyConsumer<>());
    var refactoredProp = new RefactoredProp<>(prop1, prop2, Integer::parseInt);

    refactoredProp.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put(KEY_1, "1");

    // ASSERT
    verify(consumer, timeout(1_000)).accept(1);

    assertThat("Expecting original prop value to be set", prop1.get(), equalTo("1"));
    assertThat("Expecting refactored prop value to not be set", prop2.get(), equalTo(null));
    assertThat("Expecting the original value to be returned", refactoredProp.get(), equalTo(1));
  }

  @Test
  void exceptionIsPropagated() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestStringProp(KEY_1, null));
    var prop2 = registry.bind(new TestErrorOnGetProp(KEY_2, null));

    DummyConsumer<Throwable> errorReceived = spy(new DummyConsumer<>());
    prop2.subscribe((ignore) -> {}, errorReceived);

    DummyConsumer<Integer> consumer = spy(new DummyConsumer<>());
    RefactoredProp<String, Integer> supplier =
        new RefactoredProp<>(prop1, prop2, Integer::parseInt);

    supplier.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put(KEY_1, "1");
    await().until(prop1::get, equalTo("1"));

    source.put(KEY_2, "2");
    // wait until the second prop is initialized
    verify(errorReceived, timeout(1_000)).accept(any());

    // ASSERT
    // prop2 fails
    Assertions.assertThrows(ValueCannotBeReadException.class, prop2::get);
    // and the RefactoredProp cannot resolve due to an invalid prop2
    Assertions.assertThrows(ValueCannotBeReadException.class, supplier::get);
  }

  @Test
  void exceptionIsPropagatedWhenRefactoredPropFails() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestStringProp(KEY_1, null));
    var prop2 = registry.bind(new TestErrorOnGetProp(KEY_2, null));

    DummyConsumer<Throwable> errorReceived = spy(new DummyConsumer<>());
    prop2.subscribe((ignore) -> {}, errorReceived);

    DummyConsumer<Integer> consumer = spy(new DummyConsumer<>());
    var refactoredProp = new RefactoredProp<>(prop1, prop2, Integer::parseInt);

    refactoredProp.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put(KEY_2, "2");
    // wait until the second prop is initialized
    verify(errorReceived, timeout(1_000)).accept(any());

    // ASSERT
    Assertions.assertThrows(ValueCannotBeReadException.class, prop2::get);
    // the RefactoredProp cannot resolve due to an invalid prop2
    Assertions.assertThrows(ValueCannotBeReadException.class, refactoredProp::get);
  }

  @Test
  void exceptionAlwaysThrownByRefactoredProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestStringProp(KEY_1, null));
    var prop2 = registry.bind(new TestErrorOnGetProp(KEY_2, null));

    DummyConsumer<Throwable> errorReceived = spy(new DummyConsumer<>());
    prop2.subscribe((ignore) -> {}, errorReceived);

    DummyConsumer<Integer> consumer = spy(new DummyConsumer<>());
    RefactoredProp<String, Integer> refactoredProp =
        new RefactoredProp<>(prop1, prop2, Integer::parseInt);

    refactoredProp.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put(KEY_2, "2");
    // wait until the second prop is initialized
    verify(errorReceived, timeout(1_000)).accept(any());

    source.put(KEY_1, "1");

    // ASSERT
    Assertions.assertThrows(ValueCannotBeReadException.class, prop2::get);
    Assertions.assertThrows(ValueCannotBeReadException.class, refactoredProp::get);
  }

  @Test
  void bothExceptionsObserved() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestErrorOnSetProp(KEY_1, null));
    var prop2 = registry.bind(new TestErrorOnGetProp(KEY_2, null));

    AtomicReference<Throwable> errorCapture1 = new AtomicReference<>();
    prop1.subscribe(ignored -> {}, errorCapture1::set);

    AtomicReference<Throwable> errorCapture2 = new AtomicReference<>();
    prop2.subscribe(ignored -> {}, errorCapture2::set);

    AtomicReference<Throwable> errorCapture = new AtomicReference<>();
    DummyConsumer<Integer> consumer = spy(new DummyConsumer<>());
    var refactoredProp = new RefactoredProp<>(prop1, prop2, Function.identity());
    refactoredProp.subscribe(consumer, errorCapture::set);

    // ACT
    // initialize both keys in error state
    source.put(KEY_1, "2");
    source.put(KEY_2, "3");

    // ASSERT
    await().until(errorCapture::get, hasExceptionMessage(TestErrorOnGetProp.errorMessage(3)));
    Assertions.assertThrows(ValueCannotBeReadException.class, refactoredProp::get);

    await().until(errorCapture1::get, hasExceptionMessage(TestErrorOnSetProp.errorMessage(2)));
    Assertions.assertThrows(ValueCannotBeSetException.class, prop1::get);

    await().until(errorCapture2::get, hasExceptionMessage(TestErrorOnGetProp.errorMessage(3)));
    Assertions.assertThrows(ValueCannotBeReadException.class, prop2::get);
  }
}
