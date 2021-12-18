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

import sh.props.annotations.Nullable;
import sh.props.tuples.Pair;

/**
 * Common interface for datastore implementations.
 *
 * <p>Currently, this interface is not ready to be made public and only a single (internal)
 * implementation is provided.
 */
interface RegistryStore {

  /**
   * Retrieves a value,layer pair for the specified key.
   *
   * @param key the key to retrieve
   * @return a value,layer pair, or null if not found
   */
  @Nullable
  Pair<String, Layer> get(String key);

  /**
   * Updates a value and its originating layer, for the specified key.
   *
   * <p>If a <code>null</code> value is sent, implementations should unmap the specified key.
   *
   * @param key the key to update
   * @param valueLayer the value, layer pair to update
   * @return a value,layer pair if the key is defined in any layer, or null if this operation
   *     results in the deletion of the key
   */
  @Nullable
  Pair<String, Layer> put(String key, Pair<String, Layer> valueLayer);
}
