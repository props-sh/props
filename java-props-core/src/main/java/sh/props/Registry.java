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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Registry {
    private final ConcurrentHashMap<String, Layer> owners = new ConcurrentHashMap<>();
    Layer topLayer;

    // registers ownership of a layer over a key
    void bindKey(String key, Layer layer) {
        // finds the current owner
        Layer owner = owners.get(key);

        // determines if ownership change is required
        if (owner.priority() < layer.priority()) {
            // change the current owner
            owners.put(key, layer);
            // TODO: notify subscribers of update
        }
    }

    // unregisters ownership of a layer over a key
    void unbindKey(String key, Layer layer) {
        // finds the current owner
        Layer owner = owners.get(key);

        // determines if ownership change is required
        if (owner == layer) {
            // change the current owner
            while (layer != null) {
                layer = layer.prev;
                if (layer.containsKey(key)) {
                    // update the owner
                    owners.put(key, layer);
                    // TODO: notify subscribers of update
                    return;
                }
            }

            // we could not replace the owner
            // it means this key is not defined in any layer
            owners.remove(key);
            // TODO: notify subscribers of update
        }

        // nothing to do, ownership did not change
    }

    void updateKey(String key, Layer layer) {
        // finds the current owner
        Layer owner = owners.get(key);

        // determines if the specified layer owns the key
        if (owner == layer) {
            // TODO: notify subscribers of update
        }
    }

    // retrieves the value for the specified key
    public <T> T get(String key, Class<T> clz) {
        // finds the owner
        Layer owner = owners.get(key);

        // retrieves the value
        String effectiveValue = owner.get(key);

        // casts it
        return clz.cast(effectiveValue);
    }

    public static class Builder {
        List<Source> sources = new ArrayList<>();

        private Builder source(Source source) {
            sources.add(source);
            return this;
        }

        public Registry build() {
            final Registry registry = new Registry();
            Layer tail = null;
            int counter = 0;

            for (Source source : sources) {
                Layer layer = new Layer(source, registry, counter++);
                layer.prev = tail;
                if (tail != null) {
                    tail.next = layer;
                }
                tail = layer;
            }
            registry.topLayer = tail;

            return registry;
        }
    }
}
