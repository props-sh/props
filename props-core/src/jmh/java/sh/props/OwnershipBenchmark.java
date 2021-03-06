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
import sh.props.converters.Cast;
import sh.props.sources.InMemory;

@SuppressWarnings({
  "NullAway",
  "checkstyle:MissingJavadocMethod",
  "PMD.JUnit4TestShouldUseBeforeAnnotation"
})
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1, time = 5)
@Measurement(iterations = 1, time = 10)
@OutputTimeUnit(TimeUnit.SECONDS)
public class OwnershipBenchmark {
  private static final String KEY = "key";
  private static final String PRESET = "preset";

  @Benchmark
  public static void setUnset(ExecutionPlan plan) {
    // set
    plan.source1.put(KEY, "v1");
    plan.source1.refresh();

    // unset
    plan.source1.remove(KEY);
    plan.source1.refresh();
  }

  @Benchmark
  public static void setUnsetSameUpdate(ExecutionPlan plan) {
    // set
    plan.source1.put(KEY, "v1");

    // unset
    plan.source1.remove(KEY);

    // update
    plan.source1.refresh();
  }

  @Benchmark
  public static void set(ExecutionPlan plan) {
    // set
    plan.source1.put(KEY, "v1");
    plan.source1.refresh();
  }

  @Benchmark
  public static void noopUpdate(ExecutionPlan plan) {
    plan.source1.refresh();
  }

  @Benchmark
  public static void get(ExecutionPlan plan, Blackhole blackhole) {
    blackhole.consume(plan.registry.get(PRESET, Cast.asString()));
  }

  @Benchmark
  public static void getFromHashMap(ExecutionPlan plan, Blackhole blackhole) {
    blackhole.consume(plan.control.get(PRESET));
  }

  @State(Scope.Benchmark)
  public static class ExecutionPlan {

    public Map<String, String> control;
    public Registry registry;
    public InMemory source1;

    @Setup(Level.Invocation)
    public void setUp() {
      this.source1 = new InMemory();
      this.registry = new RegistryBuilder(this.source1).build();

      // set initial values
      this.source1.put(KEY, "v1");
      this.source1.put(PRESET, "preset-value");
      this.source1.refresh();

      // control
      this.control = new HashMap<>();
      this.control.put(PRESET, "preset-value");
    }
  }
}
