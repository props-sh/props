/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Mihai Bojin
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
import sh.props.Holder;
import sh.props.Prop;
import sh.props.exceptions.MultiValueReadException;
import sh.props.tuples.Triple;
import sh.props.tuples.Tuple;

class PropGroupTriple<T, U, V> extends AbstractPropGroup<Triple<T, U, V>> {
  private final Prop<T> first;
  private final Prop<U> second;
  private final Prop<V> third;

  /**
   * Constructs a synchronized quintuple of values.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   */
  public PropGroupTriple(Prop<T> first, Prop<U> second, Prop<V> third) {
    super(new AtomicReference<>(new Holder<>()), multiKey(first.key(), second.key(), third.key()));
    this.first = first;
    this.second = second;
    this.third = third;

    // guarantee that holder.value is not null
    readValues();

    // register for prop updates
    first.subscribe(
        v -> setValueState(holderRef, t -> Triple.updateFirst(t, v), this::onValueUpdate),
        this::setErrorState);
    second.subscribe(
        v -> setValueState(holderRef, t -> Triple.updateSecond(t, v), this::onValueUpdate),
        this::setErrorState);
    third.subscribe(
        v -> setValueState(holderRef, t -> Triple.updateThird(t, v), this::onValueUpdate),
        this::setErrorState);
  }

  /** Reads the associated props. */
  private void readValues() {
    List<Throwable> errors = new ArrayList<>();

    // attempt to read all values
    T v1 = readVal(first, errors);
    U v2 = readVal(second, errors);
    V v3 = readVal(third, errors);

    if (errors.isEmpty()) {
      // if no errors, construct a result
      holderRef.updateAndGet(holder -> holder.value(Tuple.of(v1, v2, v3)));
    } else {
      // otherwise, set the errored state, suppressing all encountered errors
      holderRef.updateAndGet(holder -> holder.error(new MultiValueReadException(errors)));
    }
  }
}
