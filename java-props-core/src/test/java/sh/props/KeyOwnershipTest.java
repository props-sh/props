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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.props.KeyOwnership.ValueLayer;
import sh.props.source.impl.InMemory;

@SuppressWarnings("NullAway")
class KeyOwnershipTest {

  @BeforeEach
  void setUp() {}

  @AfterEach
  void tearDown() {}

  @Test
  void ensureMultiLayerOperations() {
    // ARRANGE
    KeyOwnership tested = spy(KeyOwnership.class);

    Registry registry = mock(Registry.class);

    InMemory source1 = new InMemory();
    Layer l1 = new Layer(source1, registry, 1);

    InMemory source2 = new InMemory();
    Layer l2 = new Layer(source2, registry, 2);
    l1.next = l2;
    l2.prev = l1;

    // ACT/ASSERT

    // the value is not defined yet
    KeyOwnershipTest.assertValueIs(tested.get("key"), null);
    ValueLayer res;

    // Layer 1 defines v1
    source1.put("key", "v1");
    source1.update();
    res = tested.put("key", "v1", l1);
    KeyOwnershipTest.assertValueIs(res, "v1");
    KeyOwnershipTest.assertValueIs(tested.get("key"), "v1");

    // Layer 2 defines v2
    source2.put("key", "v2");
    source2.update();
    res = tested.put("key", "v2", l2);
    KeyOwnershipTest.assertValueIs(res, "v2");
    KeyOwnershipTest.assertValueIs(tested.get("key"), "v2");

    // Layer 1 unsets v1
    source1.remove("key");
    source1.update();
    res = tested.put("key", null, l1);
    KeyOwnershipTest.assertValueIs(res, "v2");
    KeyOwnershipTest.assertValueIs(tested.get("key"), "v2");

    // Layer 1 defines v3
    source1.put("key", "v3");
    source1.update();
    res = tested.put("key", "v3", l1);
    KeyOwnershipTest.assertValueIs(res, "v2");
    KeyOwnershipTest.assertValueIs(tested.get("key"), "v2");

    // Layer 2 updates v4
    source2.put("key", "v4");
    source2.update();
    res = tested.put("key", "v4", l2);
    KeyOwnershipTest.assertValueIs(res, "v4");
    KeyOwnershipTest.assertValueIs(tested.get("key"), "v4");

    // Layer 2 unsets v4
    source2.remove("key");
    source2.update();
    res = tested.put("key", null, l2);
    KeyOwnershipTest.assertValueIs(res, "v3");
    KeyOwnershipTest.assertValueIs(tested.get("key"), "v3");

    // Layer 1 unsets v3
    source1.remove("key");
    source1.update();
    res = tested.put("key", null, l1);
    KeyOwnershipTest.assertValueIs(res, null);
    KeyOwnershipTest.assertValueIs(tested.get("key"), null);
  }

  private static void assertValueIs(ValueLayer result, String value) {
    if (value == null) {
      assertThat(result, nullValue());
      return;
    }

    assertThat(result, notNullValue());
    assertThat(result.value, equalTo(value));
  }
}
