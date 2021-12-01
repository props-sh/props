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

package sh.props.group;

import static java.lang.String.format;

import java.util.function.Consumer;
import sh.props.AbstractProp;
import sh.props.annotations.Nullable;
import sh.props.converters.Converter;
import sh.props.interfaces.Prop;
import sh.props.tuples.Pair;
import sh.props.tuples.Quad;
import sh.props.tuples.Triple;
import sh.props.tuples.Tuple;

/**
 * Partial implementation that can be used to combine a string template with a backing {@link Prop},
 * rendering the template with the provided value.
 *
 * @param <T> the type of the backing prop
 */
public abstract class TemplatedProp<T> implements Prop<String> {

  private final Prop<T> prop;

  /**
   * Converts the passed Prop into a Templated Prop, capable of merging the tuple's values into the
   * provided template.
   *
   * <p>The implementation will convert the tuple's values into strings (using each Prop's
   * corresponding {@link sh.props.converters.Converter}) before feeding them into the provided
   * template. For that reason, you can only use string-based format specifiers (e.g., <code>%s
   * </code>). You can also use argument indices such as <code>%2$s</code>, to reuse positional
   * values more than once. See {@link String#format(String, Object...)} for more details.
   *
   * @param prop the prop to wrap
   */
  protected TemplatedProp(Prop<T> prop) {
    this.prop = prop;
  }

  /**
   * Creates a Templated Prop.
   *
   * @param template the template that will be populated with the prop
   * @param prop the first value supplier
   * @param <T> the type of the first prop
   * @return a <code>Prop</code> that returns the rendered template on {@link Prop#get()} and also
   *     supports subscriptions
   */
  public static <T> TemplatedProp<T> of(String template, AbstractProp<T> prop) {
    return new TemplatedProp<>(prop) {
      @Override
      protected String renderTemplate(T value) {
        return format(template, TemplatedProp.encodeValue(value, prop));
      }
    };
  }

  /**
   * Creates a Templated Prop.
   *
   * @param template the template that will be populated with the props
   * @param first the first value supplier
   * @param second the second value supplier
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @return a <code>Prop</code> that returns the rendered template on {@link Prop#get()} and also
   *     supports subscriptions
   */
  public static <T, U> TemplatedProp<Pair<T, U>> of(
      String template, AbstractProp<T> first, AbstractProp<U> second) {
    return new TemplatedProp<>(Group.of(first, second)) {
      @Override
      protected String renderTemplate(Pair<T, U> value) {
        return format(
            template,
            TemplatedProp.encodeValue(value.first, first),
            TemplatedProp.encodeValue(value.second, second));
      }
    };
  }

  /**
   * Creates a Templated Prop.
   *
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   * @param template the template that will be populated with the props
   * @param first the first value supplier
   * @param second the second value supplier
   * @param third the third value supplier
   * @return a <code>Prop</code> that returns the rendered template on {@link Prop#get()} and also
   *     supports subscriptions
   */
  public static <T, U, V> TemplatedProp<Triple<T, U, V>> of(
      String template, AbstractProp<T> first, AbstractProp<U> second, AbstractProp<V> third) {
    return new TemplatedProp<>(Group.of(first, second, third)) {
      @Override
      protected String renderTemplate(Triple<T, U, V> value) {
        return format(
            template,
            TemplatedProp.encodeValue(value.first, first),
            TemplatedProp.encodeValue(value.second, second),
            TemplatedProp.encodeValue(value.third, third));
      }
    };
  }

  /**
   * Creates a Templated Prop.
   *
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   * @param <W> the type of the fourth prop
   * @param template the template that will be populated with the props
   * @param first the first value supplier
   * @param second the second value supplier
   * @param third the third value supplier
   * @param fourth the fourth value supplier
   * @return a <code>Prop</code> that returns the rendered template on {@link Prop#get()} and also
   *     supports subscriptions
   */
  public static <T, U, V, W> TemplatedProp<Quad<T, U, V, W>> of(
      String template,
      AbstractProp<T> first,
      AbstractProp<U> second,
      AbstractProp<V> third,
      AbstractProp<W> fourth) {
    return new TemplatedProp<>(Group.of(first, second, third, fourth)) {
      @Override
      protected String renderTemplate(Quad<T, U, V, W> value) {
        return format(
            template,
            TemplatedProp.encodeValue(value.first, first),
            TemplatedProp.encodeValue(value.second, second),
            TemplatedProp.encodeValue(value.third, third),
            TemplatedProp.encodeValue(value.fourth, fourth));
      }
    };
  }

  /**
   * Creates a Templated Prop.
   *
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   * @param <W> the type of the fourth prop
   * @param <X> the type of the fifth prop
   * @param template the template that will be populated with the props
   * @param first the first value supplier
   * @param second the second value supplier
   * @param third the third value supplier
   * @param fourth the fourth value supplier
   * @param fifth the fifth value supplier
   * @return a <code>Prop</code> that returns the rendered template on {@link Prop#get()} and also
   *     supports subscriptions
   */
  public static <T, U, V, W, X> TemplatedProp<Tuple<T, U, V, W, X>> of(
      String template,
      AbstractProp<T> first,
      AbstractProp<U> second,
      AbstractProp<V> third,
      AbstractProp<W> fourth,
      AbstractProp<X> fifth) {
    return new TemplatedProp<>(Group.of(first, second, third, fourth, fifth)) {
      @Override
      protected String renderTemplate(Tuple<T, U, V, W, X> value) {
        return format(
            template,
            TemplatedProp.encodeValue(value.first, first),
            TemplatedProp.encodeValue(value.second, second),
            TemplatedProp.encodeValue(value.third, third),
            TemplatedProp.encodeValue(value.fourth, fourth),
            TemplatedProp.encodeValue(value.fifth, fifth));
      }
    };
  }

  /**
   * Helper method that can encode the provided value using a {@link Converter}.
   *
   * @param value the value to encode as string
   * @param converter the converter object that can encode/decode the value
   * @param <T> the type of the value to encode
   * @return the prop's value string representation, or null if the provided value was null
   */
  @Nullable
  protected static <T> String encodeValue(@Nullable T value, Converter<T> converter) {
    // if the value is null, stop here
    if (value == null) {
      return null;
    }

    // if the Prop is a Converter, use it to encode the value
    return converter.encode(value);
  }

  /**
   * Implement this method and render a string template using the provided value.
   *
   * @param value the tuple to render into a template
   * @return the rendered template
   */
  protected abstract String renderTemplate(T value);

  @Override
  public String key() {
    return this.prop.key();
  }

  @Override
  public String get() {
    return this.renderTemplate(this.prop.get());
  }

  @Override
  public void subscribe(Consumer<String> onUpdate, Consumer<Throwable> onError) {
    this.prop.subscribe(value -> onUpdate.accept(this.renderTemplate(value)), onError);
  }
}
