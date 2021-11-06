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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import sh.props.source.Source;
import sh.props.testhelpers.TestFileUtil;

@SuppressWarnings("NullAway")
class SourceDeserializerTest {

  @Test
  void read() throws IOException {
    // ARRANGE
    // make a copy of file-based properties to a temporary file
    Path propFile = TestFileUtil.createTempFilePath("data-types.properties");
    InputStream testData = this.getClass().getResourceAsStream("/source/standard-types.properties");
    assertThat("Could not find test data, cannot proceed", testData, notNullValue());
    Files.copy(testData, propFile);

    // make a copy of the source configuration to a temporary file
    Path configFile = TestFileUtil.createTempFilePath("source-config.properties");
    InputStream configData =
        this.getClass().getResourceAsStream("/source/source-configuration.properties");
    assertThat("Could not find config data, cannot proceed", configData, notNullValue());
    Files.copy(configData, configFile);

    // append a file-based configuration
    TestFileUtil.appendLine("file=" + propFile, configFile);

    // ACT
    Source[] sources = SourceDeserializer.read(Files.newInputStream(configFile));
    Registry registry = new RegistryBuilder(sources).build();

    // ASSERT
    assertThat(registry.layers, hasSize(5));
  }
}
