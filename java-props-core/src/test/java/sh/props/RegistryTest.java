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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sh.props.converter.IntegerConverter;
import sh.props.source.impl.InMemory;

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
    assertThat(prop.value(), equalTo(2));
  }

  private static class IntProp extends Prop<Integer> implements IntegerConverter {

    protected IntProp(String key, Integer defaultValue) {
      super(key, defaultValue, null, false, false);
    }
  }
}
