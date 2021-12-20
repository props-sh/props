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

import static sh.props.Utilities.assertNotNull;

import sh.props.annotations.Nullable;
import sh.props.converters.Converter;

/**
 * Builder class that creates {@link CustomProp} objects based on the provided parameters.
 *
 * @param <T> the type of the prop
 */
@SuppressWarnings({"PMD.AvoidFieldNameMatchingMethodName"})
public class CustomPropBuilder<T> {

  private final Registry registry;
  private final Converter<T> converter;
  @Nullable String description;
  @Nullable private T defaultValue;
  private boolean isRequired;
  private boolean isSecret;

  /**
   * Default constructor.
   *
   * @param registry the registry to which the created object will be bound
   * @param converter the type converter that can serialize/deserialize the Prop's value
   */
  public CustomPropBuilder(Registry registry, Converter<T> converter) {
    assertNotNull(registry, "registry");
    assertNotNull(converter, "converter");

    this.registry = registry;
    this.converter = converter;
  }

  /**
   * Creates a new {@link CustomProp} based on the provided arguments.
   *
   * @param key the Prop's key; this parameter is required and cannot be null
   * @return an initialized Prop object
   */
  public CustomProp<T> build(String key) {
    return this.registry.bind(
        new CustomProp<>(key, this.defaultValue, this.description, this.isRequired, this.isSecret) {
          @Override
          @Nullable
          public T decode(@Nullable String value) {
            return CustomPropBuilder.this.converter.decode(value);
          }

          @Override
          @Nullable
          public String encode(@Nullable T value) {
            return CustomPropBuilder.this.converter.encode(value);
          }
        });
  }

  /**
   * Specifies the Prop's default value, for when a value for the given key is not available in any
   * of the bound registries.
   *
   * @param defaultValue this Prop's default value
   * @return the current builder, providing a fluent interface
   */
  public CustomPropBuilder<T> defaultValue(T defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * Designates this Prop as required. When set to <code>true</code>, the prop must have either a
   * non-null value or default value.
   *
   * @param required true if the Prop must have a value, false if having a value is optional
   * @return the current builder, providing a fluent interface
   */
  public CustomPropBuilder<T> required(boolean required) {
    this.isRequired = required;
    return this;
  }

  /**
   * Designates this Prop as a secret. Its value will be redacted when printing it
   *
   * @param secret true if the Prop represents a secret
   * @return the current builder, providing a fluent interface
   */
  public CustomPropBuilder<T> secret(boolean secret) {
    this.isSecret = secret;
    return this;
  }

  /**
   * Metadata that explains what this Prop is used for. This field is aimed at developers, so that
   * teams can easily understand what a Prop was intended for.
   *
   * @param description the Prop's description
   * @return the current builder, providing a fluent interface
   */
  public CustomPropBuilder<T> description(String description) {
    this.description = description;
    return this;
  }
}
