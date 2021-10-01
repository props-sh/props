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

import java.util.Objects;

public class ValueLayerTuple {

  final String value;
  final Layer layer;

  /**
   * Class constructor.
   *
   * @param value a value to store
   * @param layer and the layer it originates from
   */
  public ValueLayerTuple(String value, Layer layer) {
    this.value = value;
    this.layer = layer;
  }

  /**
   * Value getter.
   *
   * @return this object's mapped value
   */
  public String value() {
    return this.value;
  }

  /**
   * Layer getter.
   *
   * @return this object's mapped layer
   */
  public Layer layer() {
    return this.layer;
  }

  /**
   * Checks if the current object equals the deconstructed value,layer pair.
   *
   * @param value the value to compare
   * @param layer the owning layer
   * @return true if equal
   */
  public boolean equalTo(String value, Layer layer) {
    return Objects.equals(value, this.value) && layer == this.layer;
  }

  /**
   * Checks if the current object's value equals the deconstructed value, but NOT the layer.
   *
   * @param value the value to compare
   * @param layer the owning layer to compare
   * @return true if only the value is equal
   */
  public boolean equalInValueOnly(String value, Layer layer) {
    return Objects.equals(value, this.value) && layer != this.layer;
  }
}
