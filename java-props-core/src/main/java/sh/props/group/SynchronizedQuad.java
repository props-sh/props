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

package sh.props.group;

import static java.lang.String.format;

import java.util.function.UnaryOperator;
import sh.props.AbstractProp;
import sh.props.TemplatedProp;
import sh.props.interfaces.Prop;
import sh.props.tuples.Quad;
import sh.props.tuples.Tuple;

/**
 * Groups four Props, retrieving them all at once. If any of the underlying Props fails to update
 * its value and calls {@link #error(Throwable)}, the exception will be passed down to any
 * subscribers and the corresponding Prop will not be updated in the resulting {@link Tuple}.
 *
 * @param <T> the type of the first prop
 * @param <U> the type of the second prop
 * @param <V> the type of the third prop
 * @param <W> the type of the fourth prop
 */
class SynchronizedQuad<T, U, V, W> extends AbstractPropGroup<Quad<T, U, V, W>>
    implements Prop<Quad<T, U, V, W>> {

  private final AbstractProp<T> first;
  private final AbstractProp<U> second;
  private final AbstractProp<V> third;
  private final AbstractProp<W> fourth;

  /**
   * Constructs a synchronized quad of values. At least two {@link Prop}s should be specified (not
   * nullable), otherwise using this implementation makes no sense.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param fourth the fourth prop
   */
  SynchronizedQuad(
      AbstractProp<T> first,
      AbstractProp<U> second,
      AbstractProp<V> third,
      AbstractProp<W> fourth) {
    this.first = first;
    this.second = second;
    this.third = third;
    this.fourth = fourth;

    // subscribe to all updates and errors
    first.subscribe(v -> this.apply(SynchronizedQuad.updateFirst(v)), this::error);
    second.subscribe(v -> this.apply(SynchronizedQuad.updateSecond(v)), this::error);
    third.subscribe(v -> this.apply(SynchronizedQuad.updateThird(v)), this::error);
    fourth.subscribe(v -> this.apply(SynchronizedQuad.updateFourth(v)), this::error);

    // retrieve the current state of the underlying props
    // it's important for this step to execute after we have subscribed to the underlying props
    // since any change operations will have been captured and will be applied on the underlying
    // atomic reference
    this.initialize(Tuple.of(first.get(), second.get(), third.get(), fourth.get()));
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its first value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @param <V> the type of the third object in the value
   * @param <W> the type of the fourth object in the value
   * @return a new object with the value updated
   */
  private static <T, U, V, W> UnaryOperator<Quad<T, U, V, W>> updateFirst(T value) {
    return prev -> prev.updateFirst(value);
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its second value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @param <V> the type of the third object in the value
   * @param <W> the type of the fourth object in the value
   * @return a new object with the value updated
   */
  private static <T, U, V, W> UnaryOperator<Quad<T, U, V, W>> updateSecond(U value) {
    return prev -> prev.updateSecond(value);
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its third value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @param <V> the type of the third object in the value
   * @param <W> the type of the fourth object in the value
   * @return a new object with the value updated
   */
  private static <T, U, V, W> UnaryOperator<Quad<T, U, V, W>> updateThird(V value) {
    return prev -> prev.updateThird(value);
  }

  /**
   * Returns an operation that can be applied to the given object, modifying its fourth value.
   *
   * @param value the new value to set
   * @param <T> the type of the first object in the value
   * @param <U> the type of the second object in the value
   * @param <V> the type of the third object in the value
   * @param <W> the type of the fourth object in the value
   * @return a new object with the value updated
   */
  private static <T, U, V, W> UnaryOperator<Quad<T, U, V, W>> updateFourth(W value) {
    return prev -> prev.updateFourth(value);
  }

  /**
   * Converts the current prop group into a template prop, capable of merging the tuple's values
   * into the provided template.
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
  @Override
  public Prop<String> renderTemplate(String template) {
    return new TemplatedProp<>(this) {
      @Override
      protected String renderTemplate(Quad<T, U, V, W> value) {
        return format(
            template,
            TemplatedProp.encodeValue(value.first, SynchronizedQuad.this.first),
            TemplatedProp.encodeValue(value.second, SynchronizedQuad.this.second),
            TemplatedProp.encodeValue(value.third, SynchronizedQuad.this.third),
            TemplatedProp.encodeValue(value.fourth, SynchronizedQuad.this.fourth));
      }
    };
  }

  /**
   * Generates and returns a key for this object. The key is generated using {@link
   * AbstractPropGroup#multiKey(String, String...)}.
   *
   * @return the key that identifies this prop group
   */
  @Override
  public String key() {
    return AbstractPropGroup.multiKey(
        this.first.key(), this.second.key(), this.third.key(), this.fourth.key());
  }
}
