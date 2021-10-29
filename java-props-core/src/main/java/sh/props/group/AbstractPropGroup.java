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
import sh.props.interfaces.Prop;

abstract class AbstractPropGroup<TupleT> extends SubscribableProp<TupleT> {

  protected final AtomicReference<EpochTuple<TupleT>> value =
      new AtomicReference<>(new EpochTuple<>(null));
  private final String key;

  /**
   * Class constructor that accept this prop group's key.
   *
   * @param key the key representing this prop group
   */
  AbstractPropGroup(String key) {
    this.key = key;
  }

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
   * Designates this {@link Prop}'s key identifier.
   *
   * @return a string id
   */
  @Override
  public String key() {
    return this.key;
  }

  /**
   * Applies the specified transformation and signals the update to any subscribers.
   *
   * @param op the transformation to apply
   */
  protected void apply(UnaryOperator<TupleT> op) {
    EpochTuple<TupleT> updated = this.value.updateAndGet(t -> t.value(op));
    this.onUpdatedValue(updated.value, updated.epoch);
  }

  /**
   * Convenience method to check the correctness of the specified value.
   *
   * @param value a reference that could be null
   * @return a non-null reference
   */
  private EpochTuple<TupleT> nonNullValue(@Nullable EpochTuple<TupleT> value) {
    if (value == null) {
      throw new NullPointerException("Invalid state, EpochTuple should not be null!");
    }
    return value;
  }

  /**
   * Sends any errors to subscribing error handlers
   *
   * @param throwable the error to send
   */
  protected void error(Throwable throwable) {
    EpochTuple<TupleT> result = this.value.updateAndGet(eT -> eT.error(throwable));
    this.onUpdateError(throwable, result.epoch);
  }

  /**
   * Retrieve this prop group's value.
   *
   * @return the tuple of values represented by this prop group
   */
  @Override
  @Nullable
  public TupleT get() {
    EpochTuple<TupleT> result = this.nonNullValue(this.value.get());
    if (result.error != null) {
      // we are only expending RuntimeExceptions to be thrown by this implementation
      throw (RuntimeException) result.error;
    }

    return result.value;
  }

  @Override
  protected boolean setValue(@Nullable String value) {
    throw new IllegalStateException(
        "A prop group cannot be bound to the Registry, nor can its value be updated directly.");
  }

  protected static class EpochTuple<TupleT> {
    private final long epoch;
    private final @Nullable TupleT value;
    private final @Nullable Throwable error;

    public EpochTuple(@Nullable TupleT value) {
      this.epoch = 1;
      this.value = value;
      this.error = null;
    }

    public EpochTuple(long epoch, @Nullable TupleT value, @Nullable Throwable error) {
      this.epoch = epoch;
      this.value = value;
      this.error = error;
    }

    public EpochTuple<TupleT> value(TupleT value) {
      return new EpochTuple<>(this.epoch + 1, value, null);
    }

    public EpochTuple<TupleT> value(UnaryOperator<TupleT> op) {
      return new EpochTuple<>(this.epoch + 1, op.apply(this.value), null);
    }

    public EpochTuple<TupleT> error(Throwable throwable) {
      return new EpochTuple<>(this.epoch + 1, null, throwable);
    }
  }
}
