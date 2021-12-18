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
import sh.props.BaseProp;
import sh.props.Holder;
import sh.props.exceptions.MultiValueReadException;
import sh.props.tuples.Quintuple;
import sh.props.tuples.Tuple;

class PropGroupQuintuple<T, U, V, W, X> extends AbstractPropGroup<Quintuple<T, U, V, W, X>> {
  private final BaseProp<T> first;
  private final BaseProp<U> second;
  private final BaseProp<V> third;
  private final BaseProp<W> fourth;
  private final BaseProp<X> fifth;

  /**
   * Constructs a synchronized quintuple of values.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param fourth the fourth prop
   * @param fifth the fifth prop
   */
  public PropGroupQuintuple(
      BaseProp<T> first,
      BaseProp<U> second,
      BaseProp<V> third,
      BaseProp<W> fourth,
      BaseProp<X> fifth) {
    super(
        new AtomicReference<>(new Holder<>()),
        multiKey(first.key(), second.key(), third.key(), fourth.key(), fifth.key()));

    this.first = first;
    this.second = second;
    this.third = third;
    this.fourth = fourth;
    this.fifth = fifth;

    // guarantee that holder.value is not null
    readValues();

    // register for prop updates
    first.subscribe(
        v -> setValueState(holderRef, t -> Quintuple.updateFirst(t, v), this::onValueUpdate),
        this::setErrorState);
    second.subscribe(
        v -> setValueState(holderRef, t -> Quintuple.updateSecond(t, v), this::onValueUpdate),
        this::setErrorState);
    third.subscribe(
        v -> setValueState(holderRef, t -> Quintuple.updateThird(t, v), this::onValueUpdate),
        this::setErrorState);
    fourth.subscribe(
        v -> setValueState(holderRef, t -> Quintuple.updateFourth(t, v), this::onValueUpdate),
        this::setErrorState);
    fifth.subscribe(
        v -> setValueState(holderRef, t -> Quintuple.updateFifth(t, v), this::onValueUpdate),
        this::setErrorState);
  }

  /** Reads the associated props. */
  private void readValues() {
    List<Throwable> errors = new ArrayList<>();

    // attempt to read all values
    T v1 = readVal(first, errors);
    U v2 = readVal(second, errors);
    V v3 = readVal(third, errors);
    W v4 = readVal(fourth, errors);
    X v5 = readVal(fifth, errors);

    if (errors.isEmpty()) {
      // if no errors, construct a result
      holderRef.updateAndGet(holder -> holder.value(Tuple.of(v1, v2, v3, v4, v5)));
    } else {
      // otherwise, set the errored state, suppressing all encountered errors
      holderRef.updateAndGet(holder -> holder.error(new MultiValueReadException(errors)));
    }
  }
}
