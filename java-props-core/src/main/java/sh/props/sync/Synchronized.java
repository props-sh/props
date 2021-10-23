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

package sh.props.sync;

import java.util.function.Consumer;
import sh.props.BaseProp;
import sh.props.Prop;
import sh.props.Subscribable;
import sh.props.annotations.Nullable;
import sh.props.tuples.Quad;
import sh.props.tuples.Tuple;

public class Synchronized {

  /**
   * Synchronizes four Props, allowing the user to retrieve all four values concurrently. The
   * returned type implements {@link Subscribable}, allowing the user to receive events when any of
   * the values are updated.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param fourth the fourth prop
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   * @param <W> the type of the fourth prop
   * @return a synchronized Quad of props, which can be retrieved together
   */
  public static <T, U, V, W> QuadSupplier<T, U, V, W> synchronize(
      BaseProp<T> first, BaseProp<U> second, BaseProp<V> third, BaseProp<W> fourth) {
    return new QuadSupplierImpl<>(first, second, third, fourth);
  }

  /**
   * Synchronizes five Props, allowing the user to retrieve all four values concurrently. The
   * returned type implements {@link Subscribable}, allowing the user to receive events when any of
   * the values are updated.
   *
   * @param first the first prop
   * @param second the second prop
   * @param third the third prop
   * @param fourth the fourth prop
   * @param fifth the fourth prop
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   * @param <W> the type of the fourth prop
   * @param <X> the type of the fifth prop
   * @return a synchronized Quad of props, which can be retrieved together
   */
  public static <T, U, V, W, X> Prop<Tuple<T, U, V, W, X>> synchronize(
      BaseProp<T> first,
      BaseProp<U> second,
      BaseProp<V> third,
      BaseProp<W> fourth,
      BaseProp<X> fifth) {
    return new SynchronizedTuple<>(first, second, third, fourth, fifth);
  }

  private static <T, U, V, W, X> Prop<Quad<T, U, V, W>> toQuad(Prop<Tuple<T, U, V, W, X>> tuple) {
    return new Prop<>() {
      @Override
      @Nullable
      public Quad<T, U, V, W> get() {
        return Synchronized.tupleToQuad(tuple.get());
      }

      @Override
      public void subscribe(Consumer<Quad<T, U, V, W>> onUpdate, Consumer<Throwable> onError) {
        tuple.subscribe(value -> onUpdate.accept(Synchronized.tupleToQuad(value)), onError);
      }
    };
  }

  /**
   * Safely converts a tuple to a quad, accounting for null tuples
   *
   * @param tuple the initial tuple
   * @param <T> the type of the first prop
   * @param <U> the type of the second prop
   * @param <V> the type of the third prop
   * @param <W> the type of the fourth prop
   * @param <X> the type of the fifth prop
   * @return a quad containing the result of {@link Tuple#toQuad()}, or null
   */
  @Nullable
  private static <T, U, V, W, X> Quad<T, U, V, W> tupleToQuad(
      @Nullable Tuple<T, U, V, W, X> tuple) {
    return tuple != null ? tuple.toQuad() : null;
  }
}
