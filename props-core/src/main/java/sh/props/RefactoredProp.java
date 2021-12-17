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

import static sh.props.Validate.ensureUnchecked;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import sh.props.annotations.Nullable;
import sh.props.exceptions.ValueCannotBeReadException;
import sh.props.interfaces.Prop;

/**
 * Helper class which attempts to resolve two properties, converting the second property's value to
 * the first property's type.
 *
 * <p>This class has the following logic:
 *
 * <ul>
 *   <li>- firstly, it attempts to retrieve the refactored prop's value
 *   <li>- if an exception is encountered while calling {@link AbstractProp#get()} (usually an
 *       {@link ValueCannotBeReadException}), it will be thrown or passed to any subscribers,
 *       regardless of the value of the original prop
 *   <li>- if the refactored prop is <code>null</code>, the class will then attempt to load a value
 *       from the original (old) prop
 *   <li>- the value will be convereted to the desired type, using the provided <code>converter
 *       </code>
 *   <li>- if an exception is encountered (usually an {@link ValueCannotBeReadException}), it will
 *       be thrown or passed to any subscribers
 *   <li>- otherwise the resulting value, or <code>null</code>, will be returned or passed to any
 *       subscribers
 * </ul>
 *
 * @param <T> the type of the property being refactored
 * @param <R> the type of the new property
 */
public class RefactoredProp<T, R> implements Prop<R> {

  private final AbstractProp<T> originalProp;
  private final AbstractProp<R> refactoredProp;
  private final String key;
  private final Function<T, R> converter;

  /**
   * Class constructor.
   *
   * @param originalProp the old property that is being refactored
   * @param refactoredProp the refactored prop
   * @param converter a converter function that can transform the old data type into the new
   *     datatype
   */
  public RefactoredProp(
      AbstractProp<T> originalProp, AbstractProp<R> refactoredProp, Function<T, R> converter) {
    this.originalProp = originalProp;
    this.refactoredProp = refactoredProp;
    this.key = refactoredProp.key();
    this.converter = converter;
  }

  /**
   * Attempts to retrieve a value from the refactored prop and then subsequently from the old prop,
   * by attempting to convert the value to the appropriate type.
   *
   * <p>Any passed <code>converter</code>s must handle null values.
   *
   * @param refactored the refactored (newer) prop
   * @param old the older prop
   * @param converter the converter that can transform old (T) to R (refactored) types
   * @param <T> the type of the older prop
   * @param <R> the type of the newer (refactored) prop
   * @return a {@link Holder} that either contains a valid value or an error state
   */
  private static <T, R> Holder<R> attemptValueRetrieval(
      Supplier<R> refactored, Supplier<T> old, Function<T, R> converter) {
    final R preferredValue;
    try {
      // attempt to retrieve the refactored prop
      preferredValue = refactored.get();

    } catch (RuntimeException e) {
      // if the refactored prop is returning an error, stop here
      return Holder.ofError(e);
    }

    // if the preferred value is set
    if (preferredValue != null) {
      // we can stop here since we have the desired value
      return Holder.ofValue(preferredValue);
    }

    try {
      // attempt to retrieve the old prop's value
      T oldPropValue = old.get();
      // and then convert it to the expected type
      return Holder.ofValue(converter.apply(oldPropValue));

    } catch (RuntimeException e) {
      // and mark the error state
      return Holder.ofError(e);
    }
  }

  /**
   * Constructs a supplier that throws the provided error as a {@link RuntimeException}.
   *
   * @param err the encountered error
   * @param <T> the type of the supplier
   * @return a supplier that in fact always throws an exception
   */
  private static <T> Supplier<T> error(Throwable err) {
    return () -> {
      throw ensureUnchecked(err);
    };
  }

  /**
   * Retrieves the prop's value by attempting to read the refactored prop, then the old prop, and
   * finally returning null.
   *
   * @return the prop's value, or null
   * @throws ValueCannotBeReadException if the value cannot be read
   * @throws RuntimeException if the old prop is converted to the new type, but the conversion fails
   */
  @Override
  @Nullable
  public R get() {
    try {
      return attemptValueRetrieval(refactoredProp, originalProp, converter).value();
    } catch (Throwable e) {
      // we know attemptValueRetrieval() will only contain InvalidReadOpException types
      throw (ValueCannotBeReadException) e;
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
   * Subscribes consumers to this prop's events.
   *
   * @param onUpdate called when a new value is received
   * @param onError called when an error occurs (a value cannot be received)
   */
  @Override
  public void subscribe(Consumer<R> onUpdate, Consumer<Throwable> onError) {
    AtomicReference<Holder<R>> preferred = new AtomicReference<>(new Holder<>());

    this.refactoredProp.subscribe(
        v -> {
          Holder<R> updated = attemptValueRetrieval(() -> v, originalProp, this.converter);
          updateState(preferred, updated, onUpdate, onError);
        },
        err -> {
          Holder<R> updated = attemptValueRetrieval(error(err), originalProp, this.converter);
          updateState(preferred, updated, onUpdate, onError);
        });

    this.originalProp.subscribe(
        v -> {
          Holder<R> updated = attemptValueRetrieval(refactoredProp, () -> v, this.converter);
          updateState(preferred, updated, onUpdate, onError);
        },
        err -> {
          Holder<R> updated = attemptValueRetrieval(refactoredProp, error(err), this.converter);
          updateState(preferred, updated, onUpdate, onError);
        });
  }

  /**
   * Updates the AtomicReference's state and notifies the appropriate consumer.
   *
   * @param ref the reference to the current state
   * @param updatedState the updated state
   * @param onUpdate notified on successful value updates
   * @param onError notified on errors
   */
  private void updateState(
      AtomicReference<Holder<R>> ref,
      Holder<R> updatedState,
      Consumer<R> onUpdate,
      Consumer<Throwable> onError) {
    Holder<R> result =
        ref.updateAndGet(
            current -> {
              if (updatedState.error == null) {
                // no errors observed, update to a value-state
                return current.value(updatedState.value);
              } else {
                // update to an error-state
                return current.error(updatedState.error);
              }
            });

    try {
      // the operation succeeded, send a value update
      onUpdate.accept(result.value());
    } catch (Throwable err) {
      // the operation failed, send the error to downstream subscribers
      onError.accept(err);
    }
  }
}
