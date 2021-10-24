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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;
import sh.props.converter.IntegerConverter;
import sh.props.source.impl.InMemory;
import sh.props.sync.Synchronize;
import sh.props.tuples.Tuple;

@SuppressWarnings("NullAway")
class CoordinatedSyncTest {

  @Test
  void coordinatePairOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    BaseProp<Integer> prop1 = registry.bind(new IntProp("key1", null));
    BaseProp<Integer> prop2 = registry.bind(new IntProp("key2", null));

    var supplier = Coordinated.coordinate(prop1, prop2);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");

    // ASSERT
    await().atMost(5, SECONDS).until(supplier::get, equalTo(Tuple.of(1, 2)));
  }

  @Test
  void coordinateTripleOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    BaseProp<Integer> prop1 = registry.bind(new IntProp("key1", null));
    BaseProp<Integer> prop2 = registry.bind(new IntProp("key2", null));
    BaseProp<Integer> prop3 = registry.bind(new IntProp("key3", null));

    @SuppressWarnings("VariableDeclarationUsageDistance")
    var supplier = Coordinated.coordinate(prop1, prop2, prop3);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");

    // ASSERT
    await().atMost(5, SECONDS).until(supplier::get, equalTo(Tuple.of(1, 2, 3)));
  }

  @Test
  void coordinateQuadOfProps() {
    // ARRANGE
    InMemory source = new InMemory(true);

    Registry registry = new RegistryBuilder().withSource(source).build();

    BaseProp<Integer> prop1 = registry.bind(new IntProp("key1", null));
    BaseProp<Integer> prop2 = registry.bind(new IntProp("key2", null));
    BaseProp<Integer> prop3 = registry.bind(new IntProp("key3", null));
    BaseProp<Integer> prop4 = registry.bind(new IntProp("key4", null));

    @SuppressWarnings("VariableDeclarationUsageDistance")
    var supplier = Synchronize.props(prop1, prop2, prop3, prop4);

    // ACT
    source.put("key1", "1");
    source.put("key2", "2");
    source.put("key3", "3");
    source.put("key4", "4");

    // ASSERT
    await().atMost(5, SECONDS).until(supplier::get, equalTo(Tuple.of(1, 2, 3, 4)));
  }

  private static class IntProp extends BaseProp<Integer> implements IntegerConverter {

    protected IntProp(String key, Integer defaultValue) {
      super(key, defaultValue, null, false, false);
    }
  }
}
