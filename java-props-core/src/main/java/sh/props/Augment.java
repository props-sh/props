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
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import sh.props.source.Source;
import sh.props.thread.BackgroundExecutorFactory;

public class Augment {

  public static Consumer<Consumer<Map<String, String>>> watchPath(
      Source source, Path path, Duration refreshInterval) throws IOException {
    return new FileWatchAdapter(source, path, refreshInterval);
  }

  public static class FileWatchAdapter implements Consumer<Consumer<Map<String, String>>> {

    private static final Logger log = Logger.getLogger(FileWatchAdapter.class.getName());

    private final ScheduledExecutorService executor = BackgroundExecutorFactory.create(1);
    private final Source source;
    private final Path filePath;
    private final Duration refreshInterval;
    private final WatchKey key;

    /**
     * Constructs a file watching adapter for a source.
     *
     * @param source Source to reload
     * @param filePath The path to watch
     * @param refreshInterval The refresh interval at which to check for updates
     * @throws IOException throws an exception if a {@link WatchService} cannot be constructed
     */
    public FileWatchAdapter(Source source, Path filePath, Duration refreshInterval)
        throws IOException {
      this.source = source;
      this.filePath = filePath;
      this.refreshInterval = refreshInterval;

      if (filePath == null || this.filePath.getParent() == null) {
        throw new NullPointerException("The passed filePath must be a valid path to a file");
      }

      WatchService watcher = FileSystems.getDefault().newWatchService();
      this.key =
          this.filePath.getParent().register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void accept(Consumer<Map<String, String>> consumer) {
      this.executor.scheduleAtFixedRate(
          () -> this.update(consumer), 0, this.refreshInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    void update(Consumer<Map<String, String>> consumer) {
      try {
        for (WatchEvent<?> event : this.key.pollEvents()) {
          // retrieve all events but ignore overflows
          WatchEvent.Kind<?> kind = event.kind();
          if (kind == OVERFLOW) {
            continue;
          }

          @SuppressWarnings("unchecked")
          WatchEvent<Path> ev = (WatchEvent<Path>) event;
          if (!Objects.equals(this.filePath, ev.context())) {
            // ignore events for any other files except the configured one
            continue;
          }

          // update
          consumer.accept(this.source.values());
          return;
        }

        // invalid events, nothing to do
      } finally {
        // reset the key so that it can be reused
        boolean valid = this.key.reset();
        if (!valid) {
          log.warning(
              () ->
                  format(
                      "Watched key %s no longer valid; was the parent directory deleted?",
                      this.key));
        }
      }
    }
  }

  public static Consumer<Consumer<Map<String, String>>> refreshAtInterval(
      Source source, Duration refreshInterval) {
    return new Adapter(source, refreshInterval);
  }

  public static class Adapter implements Consumer<Consumer<Map<String, String>>> {

    private final ScheduledExecutorService executor = BackgroundExecutorFactory.create(1);
    private final Source source;
    private final Duration refreshInterval;

    public Adapter(Source source, Duration refreshInterval) {
      this.source = source;
      this.refreshInterval = refreshInterval;
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void accept(Consumer<Map<String, String>> consumer) {
      this.executor.scheduleAtFixedRate(
          () -> consumer.accept(this.source.values()),
          0,
          this.refreshInterval.toMillis(),
          TimeUnit.MILLISECONDS);
    }
  }
}
