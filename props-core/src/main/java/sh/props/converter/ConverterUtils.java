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

package sh.props.converter;

import static java.util.logging.Level.SEVERE;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sh.props.annotations.Nullable;

class ConverterUtils {

  private static final Logger log = Logger.getLogger(ConverterUtils.class.getName());

  /**
   * Attempts to parse a {@link String} to a {@link Number} and returns <code>null</code> if it
   * cannot.
   *
   * <p>This methods logs a {@link java.util.logging.Level#SEVERE} event instead of throwing {@link
   * ParseException}s.
   */
  @Nullable
  static Number safeParseNumber(String value) {
    try {
      return NumberFormat.getInstance().parse(value);
    } catch (ParseException e) {
      log.log(SEVERE, e, () -> "Could not parse " + value + " to a number");
      return null;
    }
  }

  /**
   * Attempts to parse a {@link String} to an {@link ChronoUnit} and returns <code>null</code> if it
   * cannot.
   *
   * <p>This methods logs a {@link java.util.logging.Level#SEVERE} event instead of throwing {@link
   * IllegalArgumentException}s or {@link NullPointerException}s.
   */
  @Nullable
  static ChronoUnit safeParseChronoUnit(String value) {
    try {
      return ChronoUnit.valueOf(value);
    } catch (IllegalArgumentException | NullPointerException e) {
      log.log(SEVERE, e, () -> "Could not parse " + value + " as a ChronoUnit");
      return null;
    }
  }

  /**
   * Attempts to parse a {@link String} to an {@link Duration} and returns <code>null</code> if it
   * cannot.
   *
   * <p>This methods logs a {@link java.util.logging.Level#SEVERE} event instead of throwing {@link
   * DateTimeParseException}s.
   */
  @Nullable
  static Duration safeParseDuration(String value) {
    try {
      return Duration.parse(value);
    } catch (DateTimeParseException e) {
      log.log(SEVERE, e, () -> "Could not parse " + value + " as a valid Duration");
      return null;
    }
  }

  /**
   * Attempts to parse a {@link String} to an {@link Instant} and returns <code>null</code> if it
   * cannot.
   *
   * <p>This methods logs a {@link java.util.logging.Level#SEVERE} event instead of throwing {@link
   * DateTimeParseException}s.
   */
  @Nullable
  static Instant safeParseInstant(String value) {
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (DateTimeParseException e) {
      log.log(SEVERE, e, () -> "Could not parse " + value + " as a valid DateTime");
      return null;
    }
  }

  /** Splits a {@link String} by the given <code>separator</code>. */
  static List<String> splitString(String input, String separator) {
    return List.of(input.split(Pattern.quote(separator)));
  }

  /**
   * Splits a {@link String} by the given <code>separator</code>, casts every item using the
   * specified <code>mapper</code> func and returns a {@link List} of numbers.
   *
   * <p>Any leading and trailing space characters, as defined by {@link String#trim()}, will be
   * removed after the string is split.
   */
  static <T extends Number> List<T> splitStringAsNumbers(
      String input, String separator, Function<Number, T> mapper) {
    return Stream.of(input.split(Pattern.quote(separator)))
        .map(String::trim)
        .map(ConverterUtils::safeParseNumber)
        .filter(Objects::nonNull)
        .map(mapper)
        .collect(Collectors.toList());
  }
}
