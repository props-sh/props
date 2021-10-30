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

import static java.lang.String.format;

import java.util.function.Consumer;
import sh.props.interfaces.Prop;

public class TemplateProp<T> implements Prop<String> {
  private final String template;
  private final Prop<T> prop;

  /**
   * Class constructor, accepting the template to render and a backing prop.
   *
   * @param template the template that should be rendered
   * @param prop the prop which supplies the value
   */
  public TemplateProp(String template, Prop<T> prop) {
    if (!template.contains("%s")) {
      // validate the template
      throw new IllegalStateException("Expecting at least one placeholder");
    }

    this.template = template;
    this.prop = prop;
  }

  @Override
  public String key() {
    return this.prop.key();
  }

  @Override
  public String get() {
    return format(this.template, Prop.encodeValue(this.prop.get(), this.prop));
  }

  @Override
  public void subscribe(Consumer<String> onUpdate, Consumer<Throwable> onError) {
    this.prop.subscribe(v -> Prop.encodeValue(v, this.prop), onError);
  }
}
