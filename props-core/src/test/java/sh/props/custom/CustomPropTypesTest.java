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

package sh.props.custom;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static sh.props.sources.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import sh.props.Registry;
import sh.props.RegistryBuilder;
import sh.props.sources.InMemory;
import sh.props.textfixtures.TestFileUtil;

@SuppressWarnings("NullAway")
class CustomPropTypesTest {
  private static final String KEY = "key";

  @Test
  void booleanProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "true");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new BooleanProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(true));
  }

  @Test
  void chronoUnitProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "HALF_DAYS");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new ChronoUnitProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(ChronoUnit.HALF_DAYS));
  }

  @Test
  void dateProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "2007-12-03T10:15:30.010+01:00");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new DateProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(Date.from(Instant.ofEpochMilli(1_196_673_330_010L))));
  }

  @Test
  void doubleProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "3.14d");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new DoubleProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(3.14d));
  }

  @Test
  void durationProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "PT3H25M45S");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new DurationProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(Duration.ofSeconds(12345)));
  }

  @Test
  void floatProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "3.14f");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new FloatProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(3.14f));
  }

  @Test
  void instantProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "2007-12-03T10:15:30.010+01:00");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new InstantProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(Instant.ofEpochMilli(1_196_673_330_010L)));
  }

  @Test
  void integerProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "123");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new IntegerProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(123));
  }

  @Test
  void listOfDoublesProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "1d,2d");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new ListOfDoublesProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(List.of(1d, 2d)));
  }

  @Test
  void listOfFloatsProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "1f,2f");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new ListOfFloatsProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(List.of(1f, 2f)));
  }

  @Test
  void listOfIntegersProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "1,2,3");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new ListOfIntegersProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(List.of(1, 2, 3)));
  }

  @Test
  void listOfLongsProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "1L,2L,3L");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new ListOfLongsProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(List.of(1L, 2L, 3L)));
  }

  @Test
  void listOfLongsPropWithSpaces() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, " 1L, 2L ,3L ");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new ListOfLongsProp(KEY));

    // ASSERT
    assertThat(
        "Expecting leading/trailing spaces to be removed",
        prop.get(),
        equalTo(List.of(1L, 2L, 3L)));
  }

  @Test
  void listOfStringsProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "one,two");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new ListOfStringsProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(List.of("one", "two")));
  }

  @Test
  void longProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "123456789012L");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new LongProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(123456789012L));
  }

  @Test
  void numericDurationProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "123456789012L");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new NumericDurationProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(Duration.of(123456789012L, ChronoUnit.SECONDS)));
  }

  @Test
  void pathprop() throws IOException {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    Path tempFile = TestFileUtil.createTempFilePath("input.properties");
    source.put(KEY, tempFile.toString());

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new PathProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo(tempFile));
  }

  @Test
  void stringProp() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    source.put(KEY, "a string");

    Registry registry = new RegistryBuilder(source).build();

    // ACT
    var prop = registry.bind(new StringProp(KEY));

    // ASSERT
    assertThat(prop.get(), equalTo("a string"));
  }
}
