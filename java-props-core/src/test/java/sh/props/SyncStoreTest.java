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
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.props.source.impl.InMemory;

@SuppressWarnings("NullAway")
class SyncStoreTest {

  @BeforeEach
  void setUp() {}

  @AfterEach
  void tearDown() {}

  @Test
  void ensureMultiLayerOperations() {
    // ARRANGE
    SyncStore syncStore = spy(SyncStore.class);

    InMemory source1 = new InMemory();
    InMemory source2 = new InMemory();

    Registry registry =
        new RegistryBuilder().withSource(source1).withSource(source2).build(syncStore);

    // ACT/ASSERT

    // the value is not defined yet
    SyncStoreTest.assertValueIs(registry.get("key", String.class), null);

    // Layer 1 defines v1
    source1.put("key", "v1");
    source1.refresh();
    SyncStoreTest.assertValueIs(registry.get("key", String.class), "v1");

    // Layer 2 defines v2
    source2.put("key", "v2");
    source2.refresh();
    SyncStoreTest.assertValueIs(registry.get("key", String.class), "v2");

    // Layer 1 unsets v1
    source1.remove("key");
    source1.refresh();
    SyncStoreTest.assertValueIs(registry.get("key", String.class), "v2");

    // Layer 1 defines v3
    source1.put("key", "v3");
    source1.refresh();
    SyncStoreTest.assertValueIs(registry.get("key", String.class), "v2");

    // Layer 2 updates v4
    source2.put("key", "v4");
    source2.refresh();
    SyncStoreTest.assertValueIs(registry.get("key", String.class), "v4");

    // Layer 2 unsets v4
    source2.remove("key");
    source2.refresh();
    SyncStoreTest.assertValueIs(registry.get("key", String.class), "v3");

    // Layer 1 unsets v3
    source1.remove("key");
    source1.refresh();
    SyncStoreTest.assertValueIs(registry.get("key", String.class), null);
  }

  private static void assertValueIs(String result, String value) {
    if (value == null) {
      assertThat(result, nullValue());
      return;
    }

    assertThat(result, notNullValue());
    assertThat(result, equalTo(value));
  }
}
