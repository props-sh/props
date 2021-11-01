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

import java.util.function.Consumer;
import java.util.function.Function;
import sh.props.annotations.Nullable;
import sh.props.exceptions.InvalidReadOpException;
import sh.props.group.AbstractPropGroup;
import sh.props.group.Group;
import sh.props.interfaces.Prop;
import sh.props.tuples.Pair;

/**
 * Helper class which attempts to resolve two properties, converting the second property's value to
 * the first property's type.
 *
 * <p>If any of the two {@link Prop}s are <code>required</code> the call to {@link #get()} will
 * result in a {@link InvalidReadOpException} when neither of the two properties can resolve a
 * value.
 *
 * @param <T> the type of the property being refactored
 * @param <R> the type of the new property
 */
public class RefactoredProp<T, R> implements Prop<R>, TemplatedPropSupplier {

  private final String key;
  private final AbstractPropGroup<Pair<T, R>> group;
  private final Function<T, R> converter;

  /**
   * Class constructor.
   *
   * @param oldProp the old property that is being refactored
   * @param refactoredProp the refactored prop
   * @param converter a converter function that can transform the old data type into the new
   *     datatype
   */
  public RefactoredProp(
      AbstractProp<T> oldProp, AbstractProp<R> refactoredProp, Function<T, R> converter) {
    this.key = refactoredProp.key();
    this.group = Group.of(oldProp, refactoredProp);
    this.converter = converter;
  }

  /**
   * Retrieves the prop's value by attempting to read the refactored prop, then the old prop, and
   * finally returning null.
   *
   * @return the prop's value, or null
   * @throws InvalidReadOpException if the value cannot be read
   * @throws RuntimeException if the old prop is converted to the new type, but the conversion fails
   */
  @Override
  @Nullable
  public R get() {
    // if the appropriate value is set, return it
    Pair<T, R> pair = this.group.get();
    if (pair.second != null) {
      return pair.second;
    }

    try {
      // else attempt to convert the old prop's value
      if (pair.first != null) {
        return RefactoredProp.this.converter.apply(pair.first);
      }

      // if both values are null, return null
      return null;
    } catch (RuntimeException e) {
      // wrap any exceptions
      throw new InvalidReadOpException(e);
    }
  }

  /**
   * Identify this prop as the refactored/newer prop.
   *
   * @return this prop's key
   */
  @Override
  public String key() {
    return this.key;
  }

  /**
   * Supports rendering the current implementation's value into the provided template.
   *
   * @param template a template that accept this prop's value
   * @return a {@link Prop} that can returned the rendered template and allows subscriptions
   */
  @Override
  public Prop<String> renderTemplate(String template) {
    return this.group.renderTemplate(template);
  }

  /**
   * Subscribes consumers to this prop's events.
   *
   * @param onUpdate called when a new value is received
   * @param onError called when an error occurs (a value cannot be received)
   */
  @Override
  public void subscribe(Consumer<R> onUpdate, Consumer<Throwable> onError) {
    this.group.subscribe(
        pair -> {
          try {
            onUpdate.accept(RefactoredProp.this.get());
          } catch (InvalidReadOpException e) {
            // if the value could not be retrieved, signal that an error occurred
            onError.accept(e);
          }
        },
        onError);
  }
}
