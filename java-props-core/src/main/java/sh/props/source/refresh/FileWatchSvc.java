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

package sh.props.source.refresh;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import sh.props.source.AbstractSource;
import sh.props.source.FileWatchable;

/**
 * {@link WatchService} adapter that allows sourced which are based on files on disk to be refreshed
 * when the backing file is created, modified, or deleted.
 */
public class FileWatchSvc implements Runnable {

  private static final Logger log = Logger.getLogger(FileWatchSvc.class.getName());

  protected final ScheduledExecutorService executor;
  protected final WatchService watcher;
  protected final Map<Path, Trigger> triggers = new HashMap<>();

  /**
   * Class constructor.
   *
   * <p>Creates a new scheduled executor using {@link BackgroundExecutorFactory#create(int)}, with
   * a single thread.
   */
  public FileWatchSvc() throws IOException {
    this(BackgroundExecutorFactory.create(1));
  }

  /**
   * Class constructor.
   *
   * @param executor The executor used to check for {@link java.nio.file.WatchKey} updates.
   */
  public FileWatchSvc(ScheduledExecutorService executor) throws IOException {
    this.executor = executor;
    this.watcher = FileSystems.getDefault().newWatchService();
  }

  /**
   * Registers the specified path for notification on updates.
   *
   * @param source an {@link AbstractSource} that is also {@link FileWatchable}
   */
  public <T extends AbstractSource & FileWatchable> ScheduledSource register(T source)
      throws IOException {
    Path path = source.file();
    if (path == null) {
      throw new NullPointerException("The passed argument must not be null");
    }
    if (path.getParent() == null) {
      throw new NullPointerException("The passed argument must be a valid path to a file");
    }

    // listen for updates on all event types
    path.getParent().register(this.watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
    // and map the file to a trigger object which will be later used for refreshing the data
    this.triggers.put(path, new Trigger(source));
    // mark this source as scheduled
    return new ScheduledSource(source, true);
  }

  /**
   * Main file-watching logic.
   */
  @Override
  public void run() {
    WatchKey key = null;
    try {
      while ((key = this.watcher.take()) != null) {
        // process all events for the given key
        key.pollEvents().stream()

            // ignore overflows
            .filter(event -> event.kind() != OVERFLOW)

            // retrieve the file
            .map(
                event -> {
                  @SuppressWarnings("unchecked") WatchEvent<Path> ev = (WatchEvent<Path>) event;
                  return ev.context();
                })

            // ensure each path only appears once
            // we don't care for duplicate events on the same file
            // only that it was updated
            .distinct()

            // retrieve the corresponding trigger
            .map(this.triggers::get)

            // ignore events for any other files except the ones we registered
            .filter(Objects::nonNull)

            // we can execute the run synchronously since in turn it
            // generates a CompletableFuture
            .forEach(Trigger::run);

        // finally, reset the key so that it may be reused
        key.reset();
      }

    } catch (InterruptedException e) {
      log.log(Level.WARNING, e, () -> "Interrupted while waiting for new events");

      if (key != null) {
        // best effort to reset the WatchKey
        // in case we can recover from the interruption
        key.reset();
      }
    }
  }

  /**
   * Starts the file watching logic in a dedicated executor.
   */
  public FileWatchSvc start() {
    this.executor.execute(this);
    return this;
  }

  /**
   * Returns the default file watch service, ensuring it is initialized on first-use.
   *
   * @return the default {@link FileWatchSvc}
   */
  public static FileWatchSvc instance() {
    return Holder.DEFAULT;
  }

  /**
   * Static holder for the default instance, ensuring lazy initialization.
   */
  private static final class Holder {

    private static final FileWatchSvc DEFAULT;

    static {
      try {
        DEFAULT = new FileWatchSvc().start();
      } catch (IOException e) {
        throw new RuntimeException(
            "Unexpected error while initializing the default file watcher", e);
      }
    }
  }
}
