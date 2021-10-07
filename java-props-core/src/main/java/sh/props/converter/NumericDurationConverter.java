/*
 * MIT License
 *
 * Copyright (c) 2020 Mihai Bojin
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

package sh.props.converter;

import static java.util.Objects.isNull;
import static sh.props.converter.ConverterUtils.safeParseNumber;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import sh.props.annotations.Nullable;

/** Converter that casts the input {@link Number} to an {@link Duration} value. */
public interface NumericDurationConverter extends Converter<Duration> {

  @Override
  @Nullable
  default Duration decode(String value) {
    Number number = safeParseNumber(value);
    if (isNull(number)) {
      return null;
    }

    return Duration.of(number.longValue(), this.unit());
  }

  /**
   * Determines the {@link Duration} unit to use.
   *
   * @return a {@link ChronoUnit} representing the unit of the underlying value
   */
  ChronoUnit unit();
}
