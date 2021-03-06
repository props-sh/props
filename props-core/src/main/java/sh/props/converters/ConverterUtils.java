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

package sh.props.converters;

import static java.lang.String.format;
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
import java.util.function.Supplier;
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
  static Number safeParseNumber(@Nullable String value) {
    if (value == null) {
      return null;
    }

    try {
      return NumberFormat.getInstance().parse(value);
    } catch (ParseException e) {
      if (log.isLoggable(SEVERE)) {
        log.log(SEVERE, e, parseExceptionDetails(value, Number.class));
      }
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
  static ChronoUnit safeParseChronoUnit(@Nullable String value) {
    if (value == null) {
      return null;
    }

    try {
      return ChronoUnit.valueOf(value);
    } catch (IllegalArgumentException e) {
      if (log.isLoggable(SEVERE)) {
        log.log(SEVERE, e, parseExceptionDetails(value, ChronoUnit.class));
      }
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
  static Duration safeParseDuration(@Nullable String value) {
    if (value == null) {
      return null;
    }

    try {
      return Duration.parse(value);
    } catch (DateTimeParseException e) {
      if (log.isLoggable(SEVERE)) {
        log.log(SEVERE, e, parseExceptionDetails(value, Duration.class));
      }
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
  static Instant safeParseInstant(@Nullable String value) {
    if (value == null) {
      return null;
    }

    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (DateTimeParseException e) {
      if (log.isLoggable(SEVERE)) {
        log.log(SEVERE, e, parseExceptionDetails(value, Instant.class));
      }
      return null;
    }
  }

  /** Splits a {@link String} by the given <code>separator</code>. */
  static List<String> splitString(@Nullable String input, String separator) {
    if (input == null) {
      return List.of();
    }

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
      @Nullable String input, String separator, Function<Number, T> mapper) {
    if (input == null) {
      return List.of();
    }

    return Stream.of(input.split(Pattern.quote(separator)))
        .map(String::trim)
        .map(ConverterUtils::safeParseNumber)
        .filter(Objects::nonNull)
        .map(mapper)
        .collect(Collectors.toList());
  }

  /**
   * Helper method that standardizes the error messages returned by this class.
   *
   * @param value the value that was attempted to be cast
   * @param type the type we tried to cast to
   * @return an error message
   */
  static Supplier<String> parseExceptionDetails(Object value, Class<?> type) {
    return () -> format("Could not parse %s as a %s", value, type.getSimpleName());
  }
}
