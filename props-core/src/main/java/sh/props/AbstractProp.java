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

import sh.props.annotations.Nullable;
import sh.props.converters.Converter;
import sh.props.interfaces.Prop;

/**
 * Abstract {@link Prop} class which encompasses all the methods needed by the {@link Registry} to
 * reason about a prop.
 *
 * @param <T> the property's type
 */
// TODO(mihaibojin): get rid of the extra OOP complexity and redefine this class a Prop<T>
//                   make all implementation rely on this class, basically making this class the
//                   contract of any Prop sub-implementations
public abstract class AbstractProp<T> extends SubscribableProp<T> implements Prop<T>, Converter<T> {

  /**
   * Setter method that should update the underlying implementation's value.
   *
   * @param value the new value to set
   * @return true if the update succeeded
   */
  protected abstract boolean setValue(@Nullable String value);
}
