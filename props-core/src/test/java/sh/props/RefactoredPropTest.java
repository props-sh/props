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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sh.props.converter.IntegerConverter;
import sh.props.converter.StringConverter;
import sh.props.exceptions.InvalidReadOpException;
import sh.props.source.impl.InMemory;

@SuppressWarnings("NullAway")
class RefactoredPropTest {

  @Test
  void valueIsReturnedFromRefactoredProp() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    var prop1 = registry.bind(new StringProp("key1", null));
    var prop2 = registry.bind(new IntProp("key2", null));

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
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    var prop1 = registry.bind(new StringProp("key1", null));
    var prop2 = registry.bind(new IntProp("key2", null));

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

  @Test
  void exceptionIsPropagated() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    var prop1 = registry.bind(new StringProp("key1", null));
    var prop2 = registry.bind(new IntPropErr("key2", null));

    DummyConsumer<Throwable> initialized = spy(new DummyConsumer<>());
    prop2.subscribe((ignore) -> {}, initialized);

    DummyConsumer<Integer> consumer = spy(new DummyConsumer<>());
    RefactoredProp<String, Integer> supplier =
        new RefactoredProp<>(prop1, prop2, Integer::parseInt);

    supplier.subscribe(consumer, (ignore) -> {});

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");

    // wait until the second prop is initialized
    verify(initialized, timeout(1_000)).accept(any());

    // ASSERT
    assertThat(prop1.get(), equalTo("1"));
    Assertions.assertThrows(InvalidReadOpException.class, prop2::get);
    Assertions.assertThrows(InvalidReadOpException.class, supplier::get);
  }

  private static class DummyConsumer<T> implements Consumer<T> {
    @Override
    public void accept(T t) {
      // do nothing
    }
  }

  private static class IntProp extends CustomProp<Integer> implements IntegerConverter {

    protected IntProp(String key, Integer defaultValue) {
      super(key, defaultValue, null, false, false);
    }
  }

  private static class IntPropErr extends CustomProp<Integer> implements IntegerConverter {

    protected IntPropErr(String key, Integer defaultValue) {
      super(key, defaultValue, null, false, false);
    }

    @Override
    protected void validateBeforeGet(Integer value) {
      if (value != null && value > 1) {
        throw new InvalidReadOpException("unretrievable value");
      }
    }
  }

  private static class StringProp extends CustomProp<String> implements StringConverter {

    protected StringProp(String key, String defaultValue) {
      super(key, defaultValue, null, false, false);
    }
  }
}
