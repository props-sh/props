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

package sh.props.group;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import sh.props.AbstractProp;
import sh.props.Holder;
import sh.props.exceptions.InvalidReadOpException;
import sh.props.tuples.Quad;
import sh.props.tuples.Tuple;

class PropGroupQuad<T, U, V, W> extends AbstractPropGroup<Quad<T, U, V, W>> {
  private final String key;
  private final AbstractProp<T> first;
  private final AbstractProp<U> second;
  private final AbstractProp<V> third;
  private final AbstractProp<W> fourth;

  /**
   * Constructs a synchronized quintuple of values.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param fourth the fourth prop
   */
  public PropGroupQuad(
      AbstractProp<T> first,
      AbstractProp<U> second,
      AbstractProp<V> third,
      AbstractProp<W> fourth) {
    super(new AtomicReference<>(new Holder<>()));

    this.first = first;
    this.second = second;
    this.third = third;
    this.fourth = fourth;
    key = multiKey(first.key(), second.key(), third.key(), fourth.key());

    // guarantee that holder.value is not null
    readValues();

    // register for prop updates
    first.subscribe(
        v -> updateValue(holderRef, t -> Quad.updateFirst(t, v), this::onValueUpdate),
        this::setError);
    second.subscribe(
        v -> updateValue(holderRef, t -> Quad.updateSecond(t, v), this::onValueUpdate),
        this::setError);
    third.subscribe(
        v -> updateValue(holderRef, t -> Quad.updateThird(t, v), this::onValueUpdate),
        this::setError);
    fourth.subscribe(
        v -> updateValue(holderRef, t -> Quad.updateFourth(t, v), this::onValueUpdate),
        this::setError);
  }

  /** Reads the associated props. */
  private void readValues() {
    List<Throwable> errors = new ArrayList<>();

    // attempt to read all values
    T v1 = readVal(first, errors);
    U v2 = readVal(second, errors);
    V v3 = readVal(third, errors);
    W v4 = readVal(fourth, errors);

    if (errors.isEmpty()) {
      // if no errors, construct a result
      holderRef.updateAndGet(holder -> holder.value(Tuple.of(v1, v2, v3, v4)));
    } else {
      // otherwise, collect all the logged exceptions
      var exc = new InvalidReadOpException("One or more errors");
      errors.forEach(exc::addSuppressed);

      // and set the errored state
      holderRef.updateAndGet(holder -> holder.error(exc));
    }
  }

  /**
   * Returns a key for this object. The key is generated using {@link #multiKey(String, String...)}.
   *
   * @return the key that identifies this prop group
   */
  @Override
  public String key() {
    return key;
  }
}
