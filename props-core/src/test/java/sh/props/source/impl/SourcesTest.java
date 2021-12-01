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

package sh.props.source.impl;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static sh.props.source.InMemory.UPDATE_REGISTRY_MANUALLY;
import static sh.props.source.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import sh.props.RegistryBuilder;
import sh.props.converter.Cast;
import sh.props.source.ClasspathPropertyFile;
import sh.props.source.Environment;
import sh.props.source.InMemory;
import sh.props.source.PropertyFile;
import sh.props.source.SystemProperties;
import sh.props.textfixtures.AwaitAssertionTest;
import sh.props.textfixtures.TestFileUtil;
import sh.props.typed.BooleanProp;

@SuppressWarnings("NullAway")
class SourcesTest extends AwaitAssertionTest {

  @Test
  void classpathPropertyFile() {
    // ARRANGE
    var source = new ClasspathPropertyFile("/source/standard-types.properties");
    var registry = new RegistryBuilder(source).build();

    // ACT
    Boolean value = registry.get("a.boolean", Cast.asBoolean());

    // ASSERT
    assertThat(value, equalTo(true));
  }

  @Test
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
    assertThat(value, equalTo(envVar.getValue()));
  }

  @Test
  void inMemoryUnset() {
    // ARRANGE
    var source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    var registry = new RegistryBuilder(source).build();

    // ACT
    Boolean value = registry.get("key", Cast.asBoolean());

    // ASSERT
    assertThat(value, nullValue());
  }

  @Test
  void inMemorySetAuto() {
    // ARRANGE
    var source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);
    var registry = new RegistryBuilder(source).build();

    // ACT
    source.put("key", "true");
    Boolean value = registry.get("key", Cast.asBoolean());

    // ASSERT
    assertThat(value, equalTo(true));
  }

  @Test
  void inMemorySetManual() {
    // ARRANGE
    var source = new InMemory(UPDATE_REGISTRY_MANUALLY);
    var registry = new RegistryBuilder(source).build();
    @SuppressWarnings("VariableDeclarationUsageDistance")
    BooleanProp prop = registry.bind(new BooleanProp("key"));

    // ACT
    source.put("key", "true");
    assertThat(registry.get("key"), nullValue());

    // ASSERT
    source.refresh();
    await().until(prop::get, equalTo(true));
  }

  @Test
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
    assertThat(value, equalTo(System.getProperty(sysPropKey)));
  }

  @Test
  void propertyFile() throws IOException {
    // ARRANGE
    Path propFile = TestFileUtil.createTempFilePath("input.properties");

    // load existing test properties
    InputStream testData = this.getClass().getResourceAsStream("/source/standard-types.properties");
    assertThat("Could not find test data, cannot proceed", testData, notNullValue());

    // copy the properties to a temp file
    Files.copy(testData, propFile);

    // load the test file
    var source = new PropertyFile(propFile);
    var registry = new RegistryBuilder(source).build();

    // ACT
    Boolean value = registry.get("a.boolean", Cast.asBoolean());

    // ASSERT
    assertThat(value, equalTo(true));
  }
}
