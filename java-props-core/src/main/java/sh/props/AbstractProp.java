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

import static java.lang.String.format;

import sh.props.annotations.Nullable;
import sh.props.converter.Converter;
import sh.props.interfaces.Prop;

/**
 * Abstract {@link Prop} class which encompasses all the methods needed by the {@link Registry} to
 * reason about a prop.
 *
 * @param <T> the property's type
 */
public abstract class AbstractProp<T> implements Prop<T> {
  /**
   * Helper method that can encode the provided value using a converter, if the associated prop
   * implements {@link Converter}.
   *
   * @param <PropT> the type of the prop
   * @param value the value to encode as string
   * @param prop the prop to use as a Converter
   * @return the prop's value string representation, or null if the provided value was null
   */
  @Nullable
  public static <T, PropT extends Prop<T>> String encodeValue(@Nullable T value, PropT prop) {
    // if the value is null, stop here
    if (value == null) {
      return null;
    }

    if (prop instanceof Converter) {
      // if the Prop is a Converter, use it to encode the value
      @SuppressWarnings("unchecked")
      Converter<T> converter = (Converter<T>) prop;
      return converter.encode(value);
    }

    // fall back to using Object.toString()
    return value.toString();
  }

  /**
   * Setter method that should update the underlying implementation's value.
   *
   * @param value the new value to set
   * @return true if the update succeeded
   */
  protected abstract boolean setValue(@Nullable String value);

  /**
   * Converts the current prop into a template prop, capable of merging the tuple's values into the
   * provided template.
   *
   * <p>The implementation will convert the tuple's values into strings (using each Prop's
   * corresponding {@link sh.props.converter.Converter}) before feeding them into the provided
   * template. For that reason, you can only use string-based format specifiers (e.g., <code>%s
   * </code>). You can also use argument indices such as <code>%2$s</code>, to reuse positional
   * values more than once. See {@link String#format(String, Object...)} for more details.
   *
   * @param template the template to populate
   * @return a <code>Prop</code> that returns the rendered value on {@link Prop#get()} and also
   *     supports subscriptions
   */
  public Prop<String> renderTemplate(String template) {
    return new TemplatedProp<>(this) {
      @Override
      protected String renderTemplate(T value) {
        return format(template, AbstractProp.encodeValue(value, AbstractProp.this));
      }
    };
  }
}
