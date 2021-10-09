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

package sh.props;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.annotations.Nullable;
import sh.props.converter.Converter;
import sh.props.exceptions.InvalidReadOpException;
import sh.props.exceptions.InvalidUpdateOpException;

/**
 * The base property object.
 *
 * @param <T> the property's type
 */
public abstract class Prop<T> extends SubscribedProp<T> implements Converter<T> {

  private static final Logger log = Logger.getLogger(Prop.class.getName());

  public final String key;

  @Nullable private final T defaultValue;

  @Nullable private final String description;

  private final boolean isRequired;

  private final boolean isSecret;

  @Nullable private volatile T currentValue;

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
  protected Prop(
      String key,
      @Nullable T defaultValue,
      @Nullable String description,
      boolean isRequired,
      boolean isSecret) {
    // validate
    if (isNull(key)) {
      throw new NullPointerException("The property's key cannot be null");
    }

    this.key = key;
    this.defaultValue = defaultValue;
    this.description = description;
    this.isRequired = isRequired;
    this.isSecret = isSecret;
  }

  /**
   * Validates any updates to a property's value.
   *
   * <p>This method can be overridden for more advanced validation requirements.
   *
   * @param value the value to validate
   * @throws InvalidUpdateOpException when the validation fails
   */
  protected void validateBeforeSet(@Nullable T value) throws InvalidUpdateOpException {
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
   * @throws InvalidReadOpException when validation fails
   */
  protected void validateBeforeGet(@Nullable T value) {
    // if the Prop is required, a value must be available
    if (this.isRequired && isNull(value)) {
      throw new InvalidReadOpException(
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
  boolean setValue(@Nullable String updateValue) {
    // decode the value, if non null
    T value = updateValue != null ? this.decode(updateValue) : null;

    try {
      // validate the value before updating it
      this.validateBeforeSet(value);

      // update the value
      this.currentValue = value;

      // log the event
      Prop.logDebug(() -> format("%s updated", this));

      // and notify subscribers
      this.sendUpdate(value);

      return true;

    } catch (InvalidUpdateOpException | RuntimeException e) {
      // logs the exception and signals any subscribers
      this.logError(e);
      this.signalError(e);
    }

    // the update failed
    return false;
  }

  /**
   * Logs debug messages to the default logger, at {@link Level#FINE}.
   *
   * <p>Can be overwritten if a custom implementation is desirable.
   *
   * @param message supplies the message to log
   */
  protected static void logDebug(Supplier<String> message) {
    if (log.isLoggable(Level.FINE)) {
      log.fine(message);
    }
  }

  /**
   * Logs exceptions to the default logger.
   *
   * <p>Can be overwritten if a custom implementation is desirable.
   *
   * @param t the exception to log
   */
  @Override
  protected void logError(Throwable t) {
    log.log(Level.SEVERE, t, () -> format("Error while updating %s to a new value", this));
  }

  /**
   * Returns the property's current value.
   *
   * @return the {@link Prop}'s current value, or <code>null</code>.
   * @throws InvalidReadOpException if the value could not be validated
   */
  @Nullable
  public T value() {
    T value = nonNull(this.currentValue) ? this.currentValue : this.defaultValue;
    // ensure the Prop is in a valid state before returning it
    this.validateBeforeGet(value);
    return value;
  }

  /**
   * Identifies the {@link Prop}.
   *
   * @return a string id
   */
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
   * Returns <code>true</code> if this {@link Prop} requires a value.
   *
   * @return true if the property should have a value or a default
   */
  public boolean isRequired() {
    return this.isRequired;
  }

  /**
   * Returns <code>true</code> if this {@link Prop} represents a secret and its value should be
   * redacted when printed.
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
  protected String redact(T value) {
    return "<redacted>";
  }

  @Override
  public String toString() {
    // copy the value to avoid an NPE caused by a race condition
    T currentValue = this.currentValue;
    if (currentValue != null) {
      return format(
          "Prop{%s=(%s)%s}",
          this.key,
          currentValue.getClass().getSimpleName(),
          this.isSecret() ? this.redact(currentValue) : currentValue);
    }

    return format("Prop{%s=null}", this.key);
  }
}
