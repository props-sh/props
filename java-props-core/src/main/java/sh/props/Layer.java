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

import sh.props.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class Layer {
    private final Map<String, String> store = new HashMap<>();
    @Nullable Layer prev;
    @Nullable Layer next;

    private final Source source;
    private final Registry registry;
    private final int priority;

    Layer(Source source, Registry registry, int priority) {
        this.source = source;
        this.registry = registry;
        this.priority = priority;
    }

    // decides the priority of this layer, in the current registry
    public int priority() {
        return priority;
    }

    // retrieves the value for the specified key, from this layer alone
    @Nullable String get(String key) {
        return store.get(key);
    }

    boolean containsKey(String key) {
        return store.containsKey(key);
    }

    // processes the reloaded data
    public void onReload(final Map<String, String> freshValues) {
        for (String existingKey : store.keySet()) {
            if (!freshValues.containsKey(existingKey)) {
                // key was deleted
                store.remove(existingKey);
                registry.unbindKey(existingKey, this);
                continue;
            }

            String updatedValue = freshValues.get(existingKey);
            if (!Objects.equals(store.get(existingKey), updatedValue)) {
                // key was modified
                // update it
                store.put(existingKey, updatedValue);
                registry.updateKey(existingKey, this);
                // and remove it from the new map
                freshValues.remove(existingKey);
            }
        }

        // only new keys are left
        store.putAll(freshValues);
        freshValues.keySet().forEach(key -> registry.bindKey(key, this));
    }
}
