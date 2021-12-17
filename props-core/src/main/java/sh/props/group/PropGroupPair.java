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
import sh.props.exceptions.MultiValueReadException;
import sh.props.tuples.Pair;
import sh.props.tuples.Tuple;

class PropGroupPair<T, U> extends AbstractPropGroup<Pair<T, U>> {
  private final AbstractProp<T> first;
  private final AbstractProp<U> second;

  /**
   * Constructs a synchronized quintuple of values.
   *
   * @param first the first prop
   * @param second the second prop
   */
  public PropGroupPair(AbstractProp<T> first, AbstractProp<U> second) {
    super(new AtomicReference<>(new Holder<>()), multiKey(first.key(), second.key()));
    this.first = first;
    this.second = second;

    // guarantee that holder.value is not null
    readValues();

    // register for prop updates
    // TODO(mihaibojin): might need a CountDownLatch to prevent updates being lost between
    //                   readValues and prop*.subscribe
    first.subscribe(
        v -> this.setValueState(holderRef, pair -> Pair.updateFirst(pair, v), this::onValueUpdate),
        this::setErrorState);
    second.subscribe(
        v -> setValueState(holderRef, pair -> Pair.updateSecond(pair, v), this::onValueUpdate),
        this::setErrorState);
  }

  /** Reads the associated props. This method is synchronized to prevent concurrent runs. */
  // TODO(mihaibojin): attempt to use this method for updating values/errors (all PropGroupTypes)
  private void readValues() {
    List<Throwable> errors = new ArrayList<>();

    // attempt to read all values
    T v1 = readVal(first, errors);
    U v2 = readVal(second, errors);

    if (errors.isEmpty()) {
      // if no errors, construct a result
      holderRef.updateAndGet(holder -> holder.value(Tuple.of(v1, v2)));
    } else {
      // otherwise, set the errored state, suppressing all encountered errors
      holderRef.updateAndGet(holder -> holder.error(new MultiValueReadException(errors)));
    }
  }
}
