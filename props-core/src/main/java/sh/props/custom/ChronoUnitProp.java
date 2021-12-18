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

package sh.props.custom;

import java.time.temporal.ChronoUnit;
import sh.props.CustomProp;
import sh.props.annotations.Nullable;
import sh.props.converters.ChronoUnitConverter;

/** Convenience implementation that constructs a {@link CustomProp} of the underlying type. */
public class ChronoUnitProp extends CustomProp<ChronoUnit> implements ChronoUnitConverter {

  /**
   * Convenience constructor that creates an optional {@link CustomProp} without specifying a
   * default value.
   *
   * @param key the Prop's key
   */
  public ChronoUnitProp(String key) {
    this(key, null, null, false, false);
  }

  /**
   * Complete constructor that can fully customize a {@link CustomProp}.
   *
   * @param key the Prop's key
   * @param defaultValue a default value, or null if one doesn't exist
   * @param description a generic description used to explain what the prop is for
   * @param isRequired true if the Prop must have a value when {@link sh.props.AbstractProp#get()}
   *     is called
   * @param isSecret true if the Prop represents a secret, in which case its value will be redacted
   *     when {@link Object#toString()} is called
   */
  public ChronoUnitProp(
      String key,
      @Nullable ChronoUnit defaultValue,
      @Nullable String description,
      boolean isRequired,
      boolean isSecret) {
    super(key, defaultValue, description, isRequired, isSecret);
  }
}
