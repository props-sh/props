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

package sh.props;

import static java.lang.String.format;
import static sh.props.Validate.assertNotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import sh.props.annotations.Nullable;
import sh.props.converters.Converter;
import sh.props.exceptions.ValueCannotBeReadException;
import sh.props.exceptions.ValueCannotBeSetException;

/**
 * An almost complete implementation of property objects, containing additional metadata that may be
 * useful to callers. To complete this class, the user most combine it with a type {@link
 * Converter}, either by using one of the converters provided by this library, or by writing a
 * custom converter.
 *
 * @param <T> the property's type
 */
public abstract class CustomProp<T> extends AbstractProp<T> implements Converter<T> {

  public final String key;

  @Nullable private final T defaultValue;

  @Nullable private final String description;

  private final boolean isRequired;

  private final boolean isSecret;

  private final AtomicReference<Holder<T>> ref = new AtomicReference<>(new Holder<>());

  /**
   * Constructs a new property class.
   *
   * @param key the property's key
   * @param defaultValue a default value for this key, if one isn't defined
   * @param description describes this property and what it is used for; mostly aimed at allowing
   *     developers to easily understand the intended usage of a Prop
   * @param isRequired true if a value (or default) must be provided
   * @param isSecret true if this prop represents a secret and its value must not be accidentally
   *     exposed
   * @throws NullPointerException if the constructed object is in an invalid state
   */
  protected CustomProp(
      String key,
      @Nullable T defaultValue,
      @Nullable String description,
      boolean isRequired,
      boolean isSecret) {
    assertNotNull(key, "property key");

    this.key = key;
    this.defaultValue = defaultValue;
    this.description = description;
    this.isRequired = isRequired;
    this.isSecret = isSecret;
  }

  /**
   * Validates any updates to a property's value.
   *
   * <p>This method can be overridden for more advanced validation requirements. If you override
   * this method, please be aware that any exceptions thrown by this method will only be observed by
   * error consumers, registered to the prop object with {@link #subscribe(Consumer, Consumer)}.
   *
   * @param value the value to validate
   * @throws ValueCannotBeSetException when the validation fails
   */
  protected void validateBeforeSet(@Nullable T value) throws ValueCannotBeSetException {
    // no validation ops by default
  }

  /**
   * This method validates the property's value before returning it.
   *
   * <p>This method can be overridden for more advanced validation requirements. In that case, the
   * overriding implementation should still call this method via <code>super.validateOnGet()</code>,
   * to preserve the non-null value required property guarantee.
   *
   * @param value the value to validate
   * @throws ValueCannotBeReadException when validation fails
   */
  protected void validateBeforeGet(@Nullable T value) {
    // if the Prop is required, a value must be available
    if (this.isRequired && value == null) {
      throw new ValueCannotBeReadException(
          format(
              "Prop '%s' is required, but neither a value or a default were specified", this.key));
    }
  }

  /**
   * Update this property's value.
   *
   * <p>This method catches all exceptions (checked and unchecked) and then logs and notifies any
   * subscribers.
   *
   * @param updateValue the new value to set
   * @return true if the update operation succeeded
   */
  @Override
  protected boolean setValue(@Nullable String updateValue) {
    // decode the value, if non null
    T value = updateValue != null ? this.decode(updateValue) : null;

    // validate the value before updating it
    try {
      this.validateBeforeSet(value);

    } catch (ValueCannotBeSetException err) {
      // if value cannot be validated before an update, mark the prop to be in an error state
      var res = this.ref.updateAndGet(holder -> holder.error(err));

      // and notify subscribers
      this.onUpdateError(err, res.epoch);

      return false;
    }

    // update the value
    var res = this.ref.updateAndGet(holder -> holder.value(value));

    // then send the value to any subscribers
    T currentValue = valueOrDefault(res.value);

    try {
      // ensure the value is valid before reading it
      this.validateBeforeGet(currentValue);
      // then update subscribers
      this.onValueUpdate(currentValue, res.epoch);

      return true;

    } catch (ValueCannotBeReadException err) {
      // if the value is not valid for reads, notify subscribers
      this.onUpdateError(err, res.epoch);

      return false;
    }
  }

  /**
   * Returns the property's current value.
   *
   * @return the {@link CustomProp}'s current value, or <code>null</code>.
   * @throws ValueCannotBeReadException if the value could not be validated
   * @throws ValueCannotBeSetException if the value could not be updated
   */
  @Override
  @Nullable
  public T get() {
    // ensure the Prop is in a valid state before returning it
    T value = valueOrDefault(getValue());
    this.validateBeforeGet(value);
    return value;
  }

  /**
   * Retrieves the currently set value.
   *
   * @return value or null
   */
  @SuppressWarnings("NullAway")
  private T getValue() {
    // we are always guaranteed to get a non-null Holder from the AtomicRef
    return this.ref.get().value();
  }

  /**
   * Retrieves the currently set value of this prop, or the {@link #defaultValue}, if the currently
   * set value is null.
   */
  @Nullable
  private T valueOrDefault(@Nullable T currentValue) {
    return currentValue != null ? currentValue : this.defaultValue;
  }

  /**
   * Identifies the {@link CustomProp}.
   *
   * @return a string id
   */
  @Override
  public String key() {
    return this.key;
  }

  /**
   * Returns a short description explaining what this prop is used for.
   *
   * @return a short, developer-friendly description
   */
  @Nullable
  public String description() {
    return this.description;
  }

  /**
   * Returns <code>true</code> if this {@link CustomProp} requires a value.
   *
   * @return true if the property should have a value or a default
   */
  public boolean isRequired() {
    return this.isRequired;
  }

  /**
   * Returns <code>true</code> if this {@link CustomProp} represents a secret and its value should
   * be redacted when printed.
   *
   * @return true if the property is a 'secret'
   */
  public boolean isSecret() {
    return this.isSecret;
  }

  /**
   * Helper method for redacting secret values. Allows a subclass to configure the format of the
   * redacted value.
   *
   * @param value the value to be redacted
   * @return the redacted value
   */
  protected String redact(@Nullable T value) {
    return "<redacted>";
  }

  /**
   * Returns this Prop's redacted value if {@link CustomProp#isSecret()} is true, or the Prop's
   * string representation, by calling {@link Converter#encode(Object)}.
   *
   * @return a safe value that can be printed in logs or standard output
   */
  @Nullable
  protected String getSafeValue() {
    final T value;
    try {
      value = getValue();
    } catch (RuntimeException e) {
      // if the value cannot be retrieved, signal an error
      return "<ERROR>";
    }

    // process the value and ensure it is safe to print it
    T currentValue = valueOrDefault(value);
    if (this.isSecret()) {
      // redact the value, if necessary
      return this.redact(currentValue);
    } else {
      return this.encode(currentValue);
    }
  }

  /**
   * Print this Prop as a string.
   *
   * @return this Prop's string representation
   */
  @Override
  public String toString() {
    final String template = "Prop{%s=%s}";
    return format(template, this.key, this.getSafeValue());
  }
}
