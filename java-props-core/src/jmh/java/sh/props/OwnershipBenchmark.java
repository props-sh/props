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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import sh.props.source.InMemory;

@SuppressWarnings({"NullAway", "checkstyle:MissingJavadocMethod"})
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1, time = 5)
@Measurement(iterations = 1, time = 10)
@OutputTimeUnit(TimeUnit.SECONDS)
public class OwnershipBenchmark {

  @State(Scope.Benchmark)
  public static class ExecutionPlan {

    public Map<String, String> control;
    public Registry registry;
    public SyncStore tested;
    public InMemory source1;
    public LayerProxy l1;

    @Setup(Level.Invocation)
    public void setUp() {
      this.registry = mock(Registry.class);
      this.tested = spy(SyncStore.class);
      this.source1 = new InMemory();
      this.l1 = new LayerProxy(this.source1, this.registry, 1);

      // set an initial value
      this.source1.put("key", "v1");
      this.source1.refresh();
      this.tested.put("preset", "preset-value", this.l1);

      // control
      this.control = new HashMap<>();
      this.control.put("preset", "preset-value");
    }
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.SECONDS)
  public static void setUnset(Blackhole blackhole) {
    Registry registry = mock(Registry.class);
    SyncStore tested = spy(SyncStore.class);

    InMemory source1 = new InMemory();
    LayerProxy l1 = new LayerProxy(source1, registry, 1);

    source1.put("key", "v1");
    source1.refresh();
    blackhole.consume(tested.put("key", "v1", l1));

    source1.remove("key");
    source1.refresh();
    blackhole.consume(tested.put("key", null, l1));
  }

  @Benchmark
  public static void getUnused(ExecutionPlan plan) {
    plan.tested.get("preset");
  }

  @Benchmark
  public static void get(ExecutionPlan plan, Blackhole blackhole) {
    blackhole.consume(plan.tested.get("preset"));
  }

  @Benchmark
  public static void getFromHashMap(ExecutionPlan plan, Blackhole blackhole) {
    blackhole.consume(plan.control.get("preset"));
  }

  @Benchmark
  public static void getFromHashMapUnused(ExecutionPlan plan) {
    plan.control.get("preset");
  }
}
