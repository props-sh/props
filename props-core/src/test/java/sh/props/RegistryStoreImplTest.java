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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.props.converters.Cast;
import sh.props.sources.InMemory;

@SuppressWarnings("NullAway")
class RegistryStoreImplTest {

  private static void assertValueIs(String result, String value) {
    if (value == null) {
      assertThat(result, nullValue());
      return;
    }

    assertThat(result, notNullValue());
    assertThat(result, equalTo(value));
  }

  @BeforeEach
  void setUp() {}

  @AfterEach
  void tearDown() {}

  @Test
  void ensureMultiLayerOperations() {
    // ARRANGE
    InMemory source1 = new InMemory();
    InMemory source2 = new InMemory();

    Registry registry = new RegistryBuilder(source1, source2).build();

    // ACT/ASSERT

    // the value is not defined yet
    RegistryStoreImplTest.assertValueIs(registry.get("key", Cast.asString()), null);

    // Layer 1 defines v1
    source1.put("key", "v1");
    source1.refresh();
    RegistryStoreImplTest.assertValueIs(registry.get("key", Cast.asString()), "v1");

    // Layer 2 defines v2
    source2.put("key", "v2");
    source2.refresh();
    RegistryStoreImplTest.assertValueIs(registry.get("key", Cast.asString()), "v2");

    // Layer 1 unsets v1
    source1.remove("key");
    source1.refresh();
    RegistryStoreImplTest.assertValueIs(registry.get("key", Cast.asString()), "v2");

    // Layer 1 defines v3
    source1.put("key", "v3");
    source1.refresh();
    RegistryStoreImplTest.assertValueIs(registry.get("key", Cast.asString()), "v2");

    // Layer 2 updates v4
    source2.put("key", "v4");
    source2.refresh();
    RegistryStoreImplTest.assertValueIs(registry.get("key", Cast.asString()), "v4");

    // Layer 2 unsets v4
    source2.remove("key");
    source2.refresh();
    RegistryStoreImplTest.assertValueIs(registry.get("key", Cast.asString()), "v3");

    // Layer 1 unsets v3
    source1.remove("key");
    source1.refresh();
    RegistryStoreImplTest.assertValueIs(registry.get("key", Cast.asString()), null);
  }
}
