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

package sh.props.sources;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static sh.props.sources.InMemory.UPDATE_REGISTRY_MANUALLY;
import static sh.props.sources.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import sh.props.RegistryBuilder;
import sh.props.converters.Cast;
import sh.props.custom.BooleanProp;
import sh.props.textfixtures.AwaitAssertionTest;
import sh.props.textfixtures.TestFileUtil;

class SourcesTest extends AwaitAssertionTest {
  private static final String KEY = "key";
  private static final String A_BOOLEAN = "a.boolean";

  @Test
  void classpathPropertyFile() {
    // ARRANGE
    var source = new ClasspathPropertyFile("/source/standard-types.properties");
    var registry = new RegistryBuilder(source).build();

    // ACT
    Boolean value = registry.get(A_BOOLEAN, Cast.asBoolean());

    // ASSERT
    assertThat(
        "Expecting a value from a classpath-based property file source", value, equalTo(true));
  }

  @Test
  @SuppressWarnings("NullAway") // NullAway does not support JUnit assumptions
  void environment() {
    // ARRANGE
    var source = new Environment();
    var registry = new RegistryBuilder(source).build();

    var maybeEnvVar = System.getenv().entrySet().stream().findFirst();
    Assumptions.assumeTrue(
        maybeEnvVar.isPresent(), () -> "At least one env var is needed for this test to run");

    var envVar = maybeEnvVar.get();

    // ACT
    String value = registry.get(envVar.getKey());

    // ASSERT
    assertThat("Expecting a value from an Environment source", value, equalTo(envVar.getValue()));
  }

  @Test
  void inMemoryUnset() {
    // ARRANGE
    var source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    var registry = new RegistryBuilder(source).build();

    // ACT
    Boolean value = registry.get(KEY, Cast.asBoolean());

    // ASSERT
    assertThat("Expecting a value from an in-memory source", value, nullValue());
  }

  @Test
  void inMemorySetAuto() {
    // ARRANGE
    var source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    var registry = new RegistryBuilder(source).build();

    // ACT
    source.put(KEY, "true");
    Boolean value = registry.get(KEY, Cast.asBoolean());

    // ASSERT
    assertThat(
        "Expecting a value from an in-memory source, which is auto-updated", value, equalTo(true));
  }

  @Test
  void inMemorySetManual() {
    // ARRANGE
    var source = new InMemory(UPDATE_REGISTRY_MANUALLY);
    var registry = new RegistryBuilder(source).build();
    @SuppressWarnings("VariableDeclarationUsageDistance")
    BooleanProp prop = registry.bind(new BooleanProp(KEY));

    // ACT
    source.put(KEY, "true");
    assertThat(
        "Expecting a value to not be set before the source is refreshed",
        registry.get(KEY),
        nullValue());

    // ASSERT
    source.refresh();
    // expecting the prop to eventually receive the update
    await().until(prop::get, equalTo(true));
  }

  @Test
  @SuppressWarnings("NullAway") // NullAway does not support JUnit assumptions
  void systemProperties() {
    // ARRANGE
    var source = new SystemProperties();
    var maybeSystemPropKey = System.getProperties().stringPropertyNames().stream().findFirst();

    Assumptions.assumeTrue(
        maybeSystemPropKey.isPresent(),
        () -> "At least one system property is needed for this test to run");
    var sysPropKey = maybeSystemPropKey.get();

    // ACT
    var registry = new RegistryBuilder(source).build();

    // ASSERT
    String value = registry.get(sysPropKey);
    assertThat(
        "Expecting a value from a System source", value, equalTo(System.getProperty(sysPropKey)));
  }

  @Test
  void propertyFile() throws IOException {
    // ARRANGE
    Path propFile = TestFileUtil.createTempFilePath("input.properties");

    // load existing test properties and copy them to a temporary file
    try (InputStream testData =
        this.getClass().getResourceAsStream("/source/standard-types.properties")) {
      assertThat("Could not find test data, cannot proceed", testData, notNullValue());
      Files.copy(testData, propFile);
    }

    // load the test file
    var source = new PropertyFile(propFile);
    var registry = new RegistryBuilder(source).build();

    // ACT
    Boolean value = registry.get(A_BOOLEAN, Cast.asBoolean());

    // ASSERT
    assertThat("Expecting a value from a property file source", value, equalTo(true));
  }
}
