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
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import sh.props.AbstractProp;
import sh.props.SubscribableProp;
import sh.props.annotations.Nullable;
import sh.props.interfaces.Prop;
import sh.props.tuples.Pair;
import sh.props.tuples.Tuple;

// this was an initial implementation, replaced by a better algorightm in {@link
// AnotherQTSyncdPropGroup)
@Deprecated(forRemoval = true)
public class QuintupleSyncdPropGroup<T, U, V, W, X> extends SubscribableProp<Tuple<T, U, V, W, X>> {
  private static final int COUNT = 5;

  private final ConcurrentLinkedQueue<Runnable> ops = new ConcurrentLinkedQueue<>();
  private final AtomicReference<Tuple<T, U, V, W, X>> cached =
      new AtomicReference<>(Tuple.of(null, null, null, null, null));
  private final String key;
  private final List<Pair<Throwable, Long>> errors =
      new ArrayList<>(Collections.nCopies(COUNT, null));
  private final BitSet errorState = new BitSet(COUNT);
  private final AtomicLong epoch = new AtomicLong(0L);
  @Nullable private volatile T val1 = null;
  @Nullable private volatile U val2 = null;
  @Nullable private volatile V val3 = null;
  @Nullable private volatile W val4 = null;
  @Nullable private volatile X val5 = null;

  /**
   * Constructs a synchronized quintuple of values.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param fourth the fourth prop
   * @param fifth the fifth prop
   */
  public QuintupleSyncdPropGroup(
      AbstractProp<T> first,
      AbstractProp<U> second,
      AbstractProp<V> third,
      AbstractProp<W> fourth,
      AbstractProp<X> fifth) {
    key =
        AbstractPropGroup.multiKey(
            first.key(), second.key(), third.key(), fourth.key(), fifth.key());

    // load initial values
    loadInitialPropValue(first, 0, this::setValue1);
    loadInitialPropValue(second, 1, this::setValue2);
    loadInitialPropValue(third, 2, this::setValue3);
    loadInitialPropValue(fourth, 3, this::setValue4);
    loadInitialPropValue(fifth, 4, this::setValue5);
    applyOps();

    // ensure we don't miss any updates while we register for updates on each individual prop
    synchronized (this) {
      subscribeToProp(first, 0, this::setValue1);
      subscribeToProp(second, 1, this::setValue2);
      subscribeToProp(third, 2, this::setValue3);
      subscribeToProp(fourth, 3, this::setValue4);
      subscribeToProp(fifth, 4, this::setValue5);
    }
  }

  /**
   * Ensures thrown exceptions are unchecked.
   *
   * @param t the exception to be thrown
   * @return the object cast as {@link RuntimeException} or a new exception, wrapping the passed
   *     throwable
   */
  private static RuntimeException ensureUnchecked(Throwable t) {
    if (t instanceof RuntimeException) {
      return (RuntimeException) t;
    }

    return new RuntimeException(t);
  }

  /**
   * Helper method that loads a Prop's initial value, calling {@link #setErr(int, Throwable)} in
   * case {@link Prop#get()} throws an exception.
   *
   * @param prop the prop that we will retrieve a value from
   * @param idx the index of the prop in the tuple
   * @param valueModifier the update operation that will modify the underlying value
   * @param <PropT> the type of the prop
   */
  private <PropT> void loadInitialPropValue(
      AbstractProp<PropT> prop, int idx, Consumer<PropT> valueModifier) {
    try {
      valueModifier.accept(prop.get());
    } catch (Throwable t) {
      setErr(idx, t);
    }
  }

  /**
   * Helper method that subscribes to a Prop, processing value updates and errors.
   *
   * @param prop the prop that we will subscribe to
   * @param idx the index of the prop in the tuple
   * @param valueModifier the update operation that will modify the underlying value
   * @param <PropT> the type of the prop
   */
  private <PropT> void subscribeToProp(
      AbstractProp<PropT> prop, int idx, Consumer<PropT> valueModifier) {
    prop.subscribe(
        (v) -> {
          ops.add(() -> valueModifier.accept(v));
          applyOps();
        },
        (t) ->
            ops.add(
                () -> {
                  setErr(idx, t);
                  applyOps();
                }));
  }

  private void setValue1(T val) {
    epoch.incrementAndGet();
    this.val1 = val;
    clearErr(0);
  }

  private void setValue2(U val) {
    epoch.incrementAndGet();
    this.val2 = val;
    clearErr(1);
  }

  private void setValue3(V val) {
    epoch.incrementAndGet();
    this.val3 = val;
    clearErr(2);
  }

  private void setValue4(W val) {
    epoch.incrementAndGet();
    this.val4 = val;
    clearErr(3);
  }

  private void setValue5(X val) {
    epoch.incrementAndGet();
    this.val5 = val;
    clearErr(4);
  }

  /**
   * Marks an error state at the specified index.
   *
   * @param idx the index to set
   * @param err the error class
   */
  private void setErr(int idx, Throwable err) {
    long clock = epoch.incrementAndGet();
    this.errors.set(idx, Tuple.of(err, clock));

    // notify subscribers of any errors
    // calling this hook here as opposed to in applyOps()
    // ensures we only send each error once
    this.onUpdateError(err, clock);
  }

  /**
   * Clears the error state from the specified index.
   *
   * @param idx the index to clear
   */
  private void clearErr(int idx) {
    if (errors.get(idx) != null) {
      errorState.clear(idx);
    }
    this.errors.set(idx, null);
  }

  /** Processes submitted operations and caches the results. */
  private synchronized void applyOps() {
    // process the queue
    while (!ops.isEmpty()) {
      ops.poll().run();
    }

    if (errorState.isEmpty()) {
      // only update the cached value if we have no errors
      cached.set(Tuple.of(val1, val2, val3, val4, val5));

      // send the cached value to any subscribers
      this.onValueUpdate(cached.get(), epoch.get());

      // NOTE: the corresponding this.onUpdateError(...) method
      //       is called directly in setErr(...) to ensure that
      //       any encountered errors are immediately sent to subscribers
      //       and that they are only sent once
    }
  }

  /**
   * Retrieves a {@link Tuple} of values. If any errors were encountered via the underlying {@link
   * sh.props.interfaces.Prop}s, this method will throw an exception. This method will continue to
   * throw an exception until all errors are cleared.
   *
   * @return a tuple, containing the current values
   * @throws RuntimeException in case any errors were set by any of the underlying props
   */
  @Override
  @SuppressWarnings("NullAway")
  public Tuple<T, U, V, W, X> get() {
    // this implementation is optimized for read operations and makes a best effort attempt
    // to return the current value without locking
    // in highly concurrent implementations, we could end up in a state where the epoch is
    // continuously incremented before the cached value could be retrieved;  to avoid this scenario
    // which could result in effects similar to a deadlock, this method will lock after the 10th
    // failed attempt and block other ops from being applied, compute a return value and then allow
    // the class to resume updating
    for (int attempts = 0; ; attempts++) {
      // if we failed to return a clean value or error for more than 10 times
      // wait for a 'window of opportunity
      if (attempts >= 10) {
        synchronized (this) {
          if (errorState.isEmpty()) {
            return cached.get();
          } else {
            // since this section is synchronized, findOldestError() should always return an
            // Exception class; however, in the off-chance of a bug, this method call will not fail
            // since it will effectively result in throw new RuntimeException(null), which is an
            // allowed call; if you do however, notice this particular scenario in production,
            // please open a bug at https://github.com/props-sh/props/issues/new/choose Thanks!
            throw ensureUnchecked(findOldestError());
          }
        }
      }

      // mark the current epoch
      long clock = epoch.get();

      // if we're not currently in an error state, attempt to return the value from cache
      if (errorState.isEmpty()) {
        // we ensure a non-null value by pre-initializing the cache in the constructor
        var current = cached.get();
        // check that the epoch hasn't changed
        if (epoch.get() == clock) {
          // no other ops were applied, and we can safely return the cached value
          return current;
        }

        // otherwise, proceed to the next cycle
        continue;
      }

      // we are in an error state, attempt to retrieve the first noted error
      Throwable throwable = findOldestError();

      // if we found an error, and the error state wasn't cleared in between and the epoch hasn't
      // changed
      if (throwable != null && !errorState.isEmpty() && epoch.get() == clock) {
        // we have a clean error state that we can return to the user
        throw ensureUnchecked(throwable);
      }
    }
  }

  /**
   * Iterates through the list of errors and finds the oldest, if an error exists.
   *
   * @return the oldest error, or null if not found
   */
  @Nullable
  private Throwable findOldestError() {
    return errors.stream()
        .sorted(Comparator.comparingLong(pair -> pair.second))
        .map(pair -> pair.first)
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns a key for this object. The key is generated using {@link
   * AbstractPropGroup#multiKey(String, String...)}.
   *
   * @return the key that identifies this prop group
   */
  @Override
  public String key() {
    return key;
  }
}
