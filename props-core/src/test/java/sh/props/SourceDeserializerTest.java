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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static sh.props.sources.InMemory.UPDATE_REGISTRY_ON_EVERY_WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sh.props.converters.Cast;
import sh.props.sources.InMemory;
import sh.props.textfixtures.TestFileUtil;
import sh.props.textfixtures.TestSource;

class SourceDeserializerTest {

  public static final String A_DURATION = "a.duration";
  static Path propFile;
  static Source[] sources;

  @BeforeAll
  static void beforeAll() throws IOException {
    // ARRANGE

    // make a copy of file-based properties to a temporary file
    propFile = TestFileUtil.createTempFilePath("-types.properties");
    try (InputStream testData =
        SourceDeserializerTest.class.getResourceAsStream("/source/extended-types.properties")) {
      assertThat("Could not find test data, cannot proceed", testData, notNullValue());
      Files.copy(testData, propFile);
    }

    // make a copy of the source configuration to a temporary file
    Path configFile = TestFileUtil.createTempFilePath("source-configuration.properties");
    try (InputStream configData =
        SourceDeserializerTest.class.getResourceAsStream(
            "/source/source-configuration.properties")) {
      assertThat("Could not find config data, cannot proceed", configData, notNullValue());
      Files.copy(configData, configFile);
    }

    // append a file-based configuration
    TestFileUtil.appendLine("file=" + propFile, configFile);

    // store the sources to be used by each test
    var deserializer = new SourceDeserializer.Builder().withDefaults().build();
    sources = deserializer.read(Files.newInputStream(configFile));
  }

  @Test
  void readFiles() {
    // ACT
    Registry registry = new RegistryBuilder(sources).build();

    // ASSERT
    assertThat("Expecting four layers", registry.layers, hasSize(4));
    assertThat(
        "Expecting the prop to be defined via standard-types.properties",
        registry.get("a.long", Cast.asLong()),
        equalTo(1L));
    assertThat(
        "Expecting the prop to be overwritten via extended-types.properties",
        registry.get(A_DURATION, Cast.asDuration()),
        equalTo(Duration.ofDays(2)));
  }

  @Test
  @SuppressWarnings("NullAway") // NullAway does not support JUnit assumptions
  void readEnvironment() {
    // ARRANGE
    var maybeEnvVar = System.getenv().entrySet().stream().findFirst();
    Assumptions.assumeTrue(
        maybeEnvVar.isPresent(), () -> "At least one env var is needed for this test to run");

    var envVar = maybeEnvVar.get();

    // ACT
    Registry registry = new RegistryBuilder(sources).build();

    // ASSERT
    assertThat("Expecting four layers", registry.layers, hasSize(4));
    assertThat(
        "Expecting the registry to correctly load values from the Environment",
        registry.get(envVar.getKey()),
        equalTo(envVar.getValue()));
  }

  @Test
  void readInMemory() {
    // ARRANGE
    InMemory source = new InMemory(UPDATE_REGISTRY_ON_EVERY_WRITE);

    // ACT
    Registry registry = new RegistryBuilder(sources).withSource(source).build();

    // ASSERT
    assertThat("Expecting five layers", registry.layers, hasSize(5));

    assertThat(
        "Expecting a value from extended-types.properties",
        registry.get(A_DURATION, Cast.asDuration()),
        equalTo(Duration.ofDays(2)));

    source.put(A_DURATION, "P3D");
    assertThat(
        "Expecting a value from memory",
        registry.get(A_DURATION, Cast.asDuration()),
        equalTo(Duration.ofDays(3)));
  }

  @Test
  @SuppressWarnings("NullAway") // NullAway does not support JUnit assumptions
  void readSystemProperties() {
    // ARRANGE
    var maybeSystemPropKey = System.getProperties().stringPropertyNames().stream().findFirst();
    Assumptions.assumeTrue(
        maybeSystemPropKey.isPresent(),
        () -> "At least one system property is needed for this test to run");
    var sysPropKey = maybeSystemPropKey.get();

    // ACT
    Registry registry = new RegistryBuilder(sources).build();

    // ASSERT
    assertThat("Expecting four layers", registry.layers, hasSize(4));
    assertThat(
        "Expecting the Registry to correctly load values form the System",
        registry.get(sysPropKey),
        equalTo(System.getProperty(sysPropKey)));
  }

  @Test
  void readFromConfigList() {
    // ARRANGE
    var deserializer = new SourceDeserializer.Builder().withDefaults().build();
    Source[] sources =
        deserializer.read(
            List.of("classpath=/source/standard-types.properties", "file=" + propFile));

    // ACT
    Registry registry = new RegistryBuilder(sources).build();

    // ASSERT
    assertThat(
        "Expecting the prop to be defined via standard-types.properties",
        registry.get("a.long", Cast.asLong()),
        equalTo(1L));
    assertThat(
        "Expecting the prop to be overwritten via extended-types.properties",
        registry.get(A_DURATION, Cast.asDuration()),
        equalTo(Duration.ofDays(2)));
  }

  @Test
  void cannotDeserializeWithUnknownSource() {
    // ARRANGE
    var deserializer = new SourceDeserializer.Builder().withDefaults().build();

    // ACT / ASSERT
    assertThrows(
        IllegalStateException.class,
        () -> deserializer.read(List.of("classpath=/source/standard-types.properties", "UNKNOWN")));
  }

  @Test
  void customSourceImplementation() {
    // ARRANGE
    var deserializer =
        new SourceDeserializer.Builder()
            .withDefaults()
            .withSource("test-source", new TestSource.Factory())
            .build();

    Source[] sources =
        deserializer.read(List.of("classpath=/source/standard-types.properties", "test-source"));

    // ACT
    Registry registry = new RegistryBuilder(sources).build();

    // ASSERT
    assertThat(
        "Expecting the prop to be defined via standard-types.properties",
        registry.get("a.long", Cast.asLong()),
        equalTo(1L));
    assertThat("Expecting a prop from the test-source", registry.get("key"), equalTo("value"));
  }
}
