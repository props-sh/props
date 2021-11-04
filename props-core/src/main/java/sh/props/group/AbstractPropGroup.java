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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import sh.props.SubscribableProp;
import sh.props.annotations.Nullable;

public abstract class AbstractPropGroup<TupleT> extends SubscribableProp<TupleT> {

  protected final AtomicReference<Holder<TupleT>> value = new AtomicReference<>(new Holder<>());

  /**
   * Helper function that concatenates the passed strings to generate a composite key. Each key part
   * is separated by the '∪' (union / U+222A) character.
   *
   * @param first a string representing the key's prefix
   * @param other optionally, any other keys to add to the key
   * @return a composite key made up by concatenating all the parts
   */
  protected static String multiKey(String first, @Nullable String... other) {
    if (other == null || other.length == 0) {
      // return only the first key, if no other parts were specified
      return first;
    }

    StringBuilder sb = new StringBuilder(first);
    for (String part : other) {
      sb.append("∪").append(part);
    }
    return sb.toString();
  }

  /**
   * Initializes the holder with a valid value for the tuple.
   *
   * <p>This method should be called in any implementing subclass's constructor.
   *
   * @param value the value to set
   */
  protected final void initialize(TupleT value) {
    this.value.updateAndGet(t -> t.value(value));
  }

  /**
   * Applies the specified transformation and signals the update to any subscribers.
   *
   * @param op the transformation to apply
   */
  protected void apply(UnaryOperator<TupleT> op) {
    Holder<TupleT> updated = this.value.updateAndGet(holder -> holder.value(op));
    this.onValueUpdate(updated.value, updated.epoch);
  }

  /**
   * Sends any errors to subscribing error handlers.
   *
   * @param throwable the error to send
   */
  protected void error(Throwable throwable) {
    Holder<TupleT> result = this.value.updateAndGet(holder -> holder.error(throwable));
    this.onUpdateError(throwable, result.epoch);
  }

  /**
   * Retrieve this prop group's value.
   *
   * @return the tuple of values represented by this prop group
   */
  @Override
  // we know a holder is always present
  // and we expect subclasses to call initialize() with a non-null value
  @SuppressWarnings("NullAway")
  public TupleT get() {
    Holder<TupleT> result = this.value.get();

    if (result.error != null) {
      // we are only expecting RuntimeExceptions to be thrown by this implementation
      throw (RuntimeException) result.error;
    }

    return result.value;
  }

  /**
   * Holder class that keep references to a value/error, as well as an epoch that can be used to
   * determine the most current value in a series of concurrent operations.
   *
   * @param <TupleT> the type of the value held by this class
   */
  protected static class Holder<TupleT> {
    private final long epoch;
    private final @Nullable TupleT value;
    private final @Nullable Throwable error;

    /** Default constructor that initializes with empty values, starting from epoch 0. */
    public Holder() {
      this.epoch = 0;
      this.value = null;
      this.error = null;
    }

    /**
     * Class constructor used to create a new complete Holder.
     *
     * @param epoch the epoch to set
     * @param value a value
     * @param error or an error
     */
    private Holder(long epoch, @Nullable TupleT value, @Nullable Throwable error) {
      this.epoch = epoch;
      this.value = value;
      this.error = error;
    }

    public Holder<TupleT> value(TupleT value) {
      return new Holder<>(this.epoch + 1, value, null);
    }

    public Holder<TupleT> value(UnaryOperator<TupleT> op) {
      return new Holder<>(this.epoch + 1, op.apply(this.value), null);
    }

    public Holder<TupleT> error(Throwable throwable) {
      return new Holder<>(this.epoch + 1, null, throwable);
    }
  }
}