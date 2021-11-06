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

package sh.props.testhelpers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestFileUtil {

  /**
   * Creates a temporary directory that will be deleted on exit, and then returns a path in that
   * directory, given the desired name. The file is not created on disk.
   *
   * @param name the name of the temporary file
   * @return a path to a temporary file
   * @throws IOException if the temporary directory could not be created
   */
  public static Path createTempFilePath(String name) throws IOException {
    Path tmpDir = Files.createTempDirectory("props.sh");
    tmpDir.toFile().deleteOnExit();
    return tmpDir.resolve(name);
  }

  /**
   * Appends the given line to the specified file. A line separator will be appended automatically.
   *
   * @param line the line to append
   * @param file the file in which to append
   * @throws IOException in case the append operation failed
   */
  public static void appendLine(String line, Path file) throws IOException {
    String data = line + System.lineSeparator();
    Files.write(file, data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
  }
}
