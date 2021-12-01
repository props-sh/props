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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static sh.props.sources.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import sh.props.exceptions.InvalidReadOpException;
import sh.props.sources.InMemory;
import sh.props.textfixtures.AwaitAssertionTest;
import sh.props.textfixtures.DummyConsumer;
import sh.props.textfixtures.TestErrorOnGetProp;
import sh.props.textfixtures.TestIntProp;
import sh.props.textfixtures.TestStringProp;

@SuppressWarnings("NullAway")
class RefactoredPropTest extends AwaitAssertionTest {

  @Test
  void valueIsReturnedFromRefactoredProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestStringProp("key1", null));
    var prop2 = registry.bind(new TestIntProp("key2", null));

    DummyConsumer<Integer> initialized = spy(new DummyConsumer<>());
    prop2.subscribe(initialized, (ignore) -> {});

    DummyConsumer<Integer> consumer = spy(new DummyConsumer<>());
    RefactoredProp<String, Integer> supplier =
        new RefactoredProp<>(prop1, prop2, Integer::parseInt);

    supplier.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");

    // wait until the second prop is initialized
    verify(initialized, timeout(1_000)).accept(2);

    // ASSERT
    verify(consumer, atLeastOnce()).accept(2);

    assertThat(prop1.get(), equalTo("1"));
    assertThat(prop2.get(), equalTo(2));
    assertThat(supplier.get(), equalTo(2));
  }

  @Test
  void valueIsReturnedFromOldProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestStringProp("key1", null));
    var prop2 = registry.bind(new TestIntProp("key2", null));

    DummyConsumer<Integer> consumer = spy(new DummyConsumer<>());
    RefactoredProp<String, Integer> supplier =
        new RefactoredProp<>(prop1, prop2, Integer::parseInt);

    supplier.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put("key1", "1");

    // ASSERT
    verify(consumer, timeout(1_000)).accept(1);

    assertThat(prop1.get(), equalTo("1"));
    assertThat(prop2.get(), equalTo(null));
    assertThat(supplier.get(), equalTo(1));
  }

  @RepeatedTest(value = 10)
  void exceptionIsPropagated() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestStringProp("key1", null));
    var prop2 = registry.bind(new TestErrorOnGetProp("key2", null));

    DummyConsumer<Throwable> errorReceived = spy(new DummyConsumer<>());
    prop2.subscribe((ignore) -> {}, errorReceived);

    DummyConsumer<Integer> consumer = spy(new DummyConsumer<>());
    RefactoredProp<String, Integer> supplier =
        new RefactoredProp<>(prop1, prop2, Integer::parseInt);

    supplier.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put("key1", "1");
    await().until(prop1::get, equalTo("1"));

    source.put("key2", "2");
    // wait until the second prop is initialized
    verify(errorReceived, timeout(1_000)).accept(any());

    // ASSERT
    // prop2 fails
    Assertions.assertThrows(InvalidReadOpException.class, prop2::get);
    // and the RefactoredProp cannot resolve due to an invalid prop2
    Assertions.assertThrows(InvalidReadOpException.class, supplier::get);
  }

  @RepeatedTest(value = 10)
  void exceptionIsPropagatedWhenRefactoredPropFails() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestStringProp("key1", null));
    var prop2 = registry.bind(new TestErrorOnGetProp("key2", null));

    DummyConsumer<Throwable> errorReceived = spy(new DummyConsumer<>());
    prop2.subscribe((ignore) -> {}, errorReceived);

    DummyConsumer<Integer> consumer = spy(new DummyConsumer<>());
    RefactoredProp<String, Integer> supplier =
        new RefactoredProp<>(prop1, prop2, Integer::parseInt);

    supplier.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put("key2", "2");
    // wait until the second prop is initialized
    verify(errorReceived, timeout(1_000)).accept(any());

    // ASSERT
    Assertions.assertThrows(InvalidReadOpException.class, prop2::get);
    // the RefactoredProp cannot resolve due to an invalid prop2
    Assertions.assertThrows(InvalidReadOpException.class, supplier::get);
  }

  @RepeatedTest(value = 10)
  void exceptionWtf() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Registry registry = new RegistryBuilder(source).build();

    var prop1 = registry.bind(new TestStringProp("key1", null));
    var prop2 = registry.bind(new TestErrorOnGetProp("key2", null));

    DummyConsumer<Throwable> errorReceived = spy(new DummyConsumer<>());
    prop2.subscribe((ignore) -> {}, errorReceived);

    DummyConsumer<Integer> consumer = spy(new DummyConsumer<>());
    RefactoredProp<String, Integer> supplier =
        new RefactoredProp<>(prop1, prop2, Integer::parseInt);

    supplier.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put("key2", "2");
    // wait until the second prop is initialized
    verify(errorReceived, timeout(1_000)).accept(any());

    source.put("key1", "1");

    // ASSERT
    // prop2 fails
    Assertions.assertThrows(InvalidReadOpException.class, prop2::get);
    // but the RefactoredProp resolves due to a valid prop1
    // TODO(mihaibojin): this is somewhat of a dubious side-effect of using AbstractPropGroup
    //                   as a store for RefactoredProp-s; need to rethink if this makes sense
    //                   (last prop to be updated decides the value and correctness) or if
    //                   given prop2, it should always take precedence regardless of prop1's
    //                   value/error.  On the other hand, maybe the logic should be: try prop2,
    //                   if missing or error, try prop1, and if missing or error, throw 1/2
    //                   exceptions
    await().until(supplier::get, equalTo(1));
  }
}
