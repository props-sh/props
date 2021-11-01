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

import java.util.function.Consumer;
import sh.props.interfaces.Prop;

/**
 * Partial implementation that can be used to combine a string template with a backing {@link Prop},
 * rendering the template with the provided value.
 *
 * @param <T> the type of the backing prop
 */
public abstract class TemplatedProp<T> implements Prop<String> {

  private final Prop<T> prop;

  /**
   * Default constructor for the template prop, accepting the backing {@link Prop} that supplies
   * values and accepts subscriptions.
   *
   * @param prop the backing prop
   */
  public TemplatedProp(Prop<T> prop) {
    this.prop = prop;
  }

  /**
   * Implement this method and render a string template using the provided value.
   *
   * @param value the tuple to render into a template
   * @return the rendered template
   */
  protected abstract String renderTemplate(T value);

  @Override
  public String key() {
    return this.prop.key();
  }

  @Override
  public String get() {
    return this.renderTemplate(this.prop.get());
  }

  @Override
  public void subscribe(Consumer<String> onUpdate, Consumer<Throwable> onError) {
    this.prop.subscribe(this::renderTemplate, onError);
  }
}
