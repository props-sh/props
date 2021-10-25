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

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/** Helper class that makes specifying a helper sound more natural. */
public class Cast {

  /**
   * Returns <code>Converter&lt;Boolean&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<Boolean> asBoolean() {
    return new BooleanConverter() {};
  }

  /**
   * Returns <code>Converter&lt;ChronoUnit&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<ChronoUnit> asChronoUnit() {
    return new ChronoUnitConverter() {};
  }

  /**
   * Returns <code>Converter&lt;Date&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<Date> asDate() {
    return new DateConverter() {};
  }

  /**
   * Returns <code>Converter&lt;Double&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<Double> asDouble() {
    return new DoubleConverter() {};
  }

  /**
   * Returns <code>Converter&lt;Duration&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<Duration> asDuration() {
    return new DurationConverter() {};
  }

  /**
   * Returns <code>Converter&lt;Float&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<Float> asFloat() {
    return new FloatConverter() {};
  }

  /**
   * Returns <code>Converter&lt;Instant&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<Instant> asInstant() {
    return new InstantConverter() {};
  }

  /**
   * Returns <code>Converter&lt;Integer&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<Integer> asInteger() {
    return new IntegerConverter() {};
  }

  /**
   * Returns <code>Converter&lt;List&lt;Double&gt;&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<List<Double>> asListOfDouble() {
    return new ListOfDoubleConverter() {};
  }

  /**
   * Returns <code>Converter&lt;List&lt;Float&gt;&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<List<Float>> asListOfFloat() {
    return new ListOfFloatConverter() {};
  }

  /**
   * Returns <code>Converter&lt;List&lt;Integer&gt;&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<List<Integer>> asListOfInteger() {
    return new ListOfIntegerConverter() {};
  }

  /**
   * Returns <code>Converter&lt;List&lt;Long&gt;&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<List<Long>> asListOfLong() {
    return new ListOfLongConverter() {};
  }

  /**
   * Returns <code>Converter&lt;List&lt;String&gt;&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<List<String>> asListOfString() {
    return new ListOfStringConverter() {};
  }

  /**
   * Returns <code>Converter&lt;Long&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<Long> asLong() {
    return new LongConverter() {};
  }

  /**
   * Returns <code>Converter&lt;NumericDuration&gt;</code>, where the unit is {@link
   * ChronoUnit#SECONDS}.
   *
   * @return a converter object
   */
  public static Converter<Duration> asNumericDuration() {
    return asNumericDuration(ChronoUnit.SECONDS);
  }

  /**
   * Returns <code>Converter&lt;NumericDuration&gt;</code>.
   *
   * @param unit specifies the {@link ChronoUnit} in which the numeric value is measured
   * @return a converter object
   */
  public static Converter<Duration> asNumericDuration(ChronoUnit unit) {
    return (NumericDurationConverter) () -> unit;
  }

  /**
   * Returns <code>Converter&lt;Path&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<Path> asPath() {
    return asPath(true);
  }

  /**
   * Returns <code>Converter&lt;Path&gt;</code>.
   *
   * @param expandHomeDirectory if true, expands <code>~</code> to the user's home directory
   * @return a converter object
   */
  public static Converter<Path> asPath(boolean expandHomeDirectory) {
    return (PathConverter) () -> expandHomeDirectory;
  }

  /**
   * Returns <code>Converter&lt;String&gt;</code>.
   *
   * @return a converter object
   */
  public static Converter<String> asString() {
    return new StringConverter() {};
  }
}
