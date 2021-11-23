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

package sh.props.mongodb;

import static java.lang.String.format;

import com.mongodb.ConnectionString;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.OperationType;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.BsonDocument;
import org.bson.BsonTimestamp;
import org.bson.Document;
import sh.props.source.Source;
import sh.props.util.BackgroundExecutorFactory;

public class MongoDbStore extends Source {
  public static final String ID = "mongodb";
  public static final boolean WATCH_CHANGE_STREAM = true;
  public static final boolean LOAD_ALL_ON_DEMAND = false;
  private static final int BATCH_SIZE = 100;
  private static final Logger log = Logger.getLogger(MongoDbStore.class.getName());

  private static final Map<String, String> store = new ConcurrentHashMap<>();
  private final Supplier<MongoCollection<Document>> collectionSupplier;
  private final boolean changeStreamEnabled;

  /**
   * Class constructor that initializes the MongoDB database that will provide key,value pairs.
   *
   * <p>If change streams are enabled, the client connecting to the database must have the <code>
   * changeStream</code> and <code>find</code> privileges on the underlying collection.
   *
   * @see <a href="https://docs.mongodb.com/manual/changeStreams/#access-control">Change Streams
   *     Access Control</a>
   * @param connectionString a valid MongoDB connection string (e.g. "mongodb://[::1]:27017/")
   * @param database the database to connect to
   * @param collection the collection that holds the key,value pairs
   * @param changeStreamEnabled if <code>true</code>, the implementation will load all Props once
   *     and then open a change stream and watch for any insert/update/delete operations. The stream
   *     will be restarted on errors and invalidate operations; if <code>false</code>, the full list
   *     of Props will be synchronously re-read on every {@link #get()} operation.
   */
  public MongoDbStore(
      String connectionString, String database, String collection, boolean changeStreamEnabled) {
    this.changeStreamEnabled = changeStreamEnabled;
    collectionSupplier = () -> getCollection(connectionString, database, collection);
    MongoCollection<Document> coll = collectionSupplier.get();

    // store the starting time, ensuring that we catch any operations that are not returned by the
    // query
    var changeStreamStartTime = new BsonTimestamp(Instant.now().toEpochMilli());

    store.putAll(loadAllKeyValuesFrom(coll));

    if (changeStreamEnabled) {
      // schedule the async processing of the change stream
      @SuppressWarnings({"FutureReturnValueIgnored", "UnusedVariable"})
      Future<?> future =
          BackgroundExecutorFactory.create(1)
              .submit(new ChangeStreamWatcher(changeStreamStartTime));
    }
  }

  /**
   * Initializes a {@link MongoCollection} using the provided parameters.
   *
   * @param connectionString the connection string to use
   * @param database the database to use
   * @param collection the collection to use
   * @return an initialized object
   */
  static MongoCollection<Document> getCollection(
      String connectionString, String database, String collection) {
    MongoClient mongoClient = initClient(connectionString);

    MongoDatabase db = mongoClient.getDatabase(database);
    return db.getCollection(collection)
        .withReadPreference(ReadPreference.primaryPreferred())
        .withReadConcern(ReadConcern.MAJORITY);
  }

  /**
   * Initializes a {@link MongoClient} from the specified connection string.
   *
   * @param connectionString the MongoDB connection string
   * @return an initialized connection to a valid cluster
   */
  static MongoClient initClient(String connectionString) {
    return MongoClients.create(new ConnectionString(connectionString));
  }

  /**
   * Initializes the {@link #store} with all the currently defined props in the underlying
   * collection.
   *
   * @param collection the collection that holds the (key,value) pairs
   * @return an unmodifiable map that contains all the key,value pairs found in the provided
   *     collection
   */
  private Map<String, String> loadAllKeyValuesFrom(MongoCollection<Document> collection) {
    Map<String, String> results = new HashMap<>();
    collection
        .find()
        .forEach(doc -> results.put(doc.getString(Schema.ID), doc.getString(Schema.VALUE)));
    return Collections.unmodifiableMap(results);
  }

  @Override
  public String id() {
    return ID;
  }

  /**
   * Retrieves all the key,value pairs defined in the underlying collection.
   *
   * <p>If {@link #changeStreamEnabled} is <code>true</code>, this method will simply return the
   * underlying {@link #store}, wrapped in {@link java.util.Collections#unmodifiableMap(Map)}.
   *
   * <p>If {@link #changeStreamEnabled} is <code>false</code>, this method will connect to the
   * database and retrieve all the key,value pairs found in the underlying collection.
   *
   * @return a map of key,value pairs
   */
  @Override
  public Map<String, String> get() {
    if (changeStreamEnabled) {
      return Collections.unmodifiableMap(store);
    } else {
      return loadAllKeyValuesFrom(collectionSupplier.get());
    }
  }

  /**
   * Ensures a valid event is passed, resulting in a non-null change stream document.
   *
   * @param event the event that provides the document
   * @param op the change stream operation
   * @param id the id of the change stream document
   * @return a non-null result
   */
  private Document getNonNullDocument(
      ChangeStreamDocument<Document> event, OperationType op, String id) {
    Document doc = event.getFullDocument();
    if (doc == null) {
      throw new NullPointerException(
          format("Unexpected null document for op %s (_id=%s)", op.getValue(), id));
    }
    return doc;
  }

  /** Holds the field names used by this implementation. */
  private static class Schema {
    public static final String CLUSTER_TIME = "clusterTime";
    private static final String ID = "_id";
    private static final String VALUE = "value";
  }

  /** Processes change stream events. */
  private class ChangeStreamWatcher implements Runnable {

    private BsonTimestamp startAtOperationTime;

    /**
     * Starts a change stream watcher, from the specified point in time.
     *
     * @param changeStreamStartTime the epoch time from which to start observing events
     */
    public ChangeStreamWatcher(BsonTimestamp changeStreamStartTime) {
      startAtOperationTime = changeStreamStartTime;
    }

    @Override
    public void run() {
      // follow the change stream until the app is shut down
      while (!Thread.currentThread().isInterrupted()) {
        try {
          // open a change stream to capture any changes on the provided collection
          var changeStream =
              collectionSupplier
                  .get()
                  .watch()
                  .startAtOperationTime(startAtOperationTime)
                  .fullDocument(FullDocument.UPDATE_LOOKUP);

          // if a valid BsonTimestamp is returned, ensure we restart the stream after that time
          startAtOperationTime = followChangeStream(changeStream);
        } catch (Exception e) {
          log.log(Level.WARNING, e, () -> "Unexpected error while processing a change stream");
        }
      }
    }

    /**
     * Follows the provided change stream, processing all valid events and restarting in case of an
     * {@link OperationType#INVALIDATE} operation.
     *
     * @param changeStream the change stream to process
     * @return a {@link BsonTimestamp} that designates the point in time at which in invalidate
     *     event would have occurred; if the app is shutting down, this method will return {@link
     *     Long#MAX_VALUE}
     */
    private BsonTimestamp followChangeStream(ChangeStreamIterable<Document> changeStream) {
      boolean shouldUpdate = false;
      int currentUpdates = 0;

      // iterate over the change stream and ensure the cursor is closed before returning
      try (MongoCursor<ChangeStreamDocument<Document>> cursor = changeStream.iterator()) {
        // stop processing if the current thread is interrupted (i.e., the app is shutting down)
        while (!Thread.currentThread().isInterrupted()) {
          // this code ensures that we avoid calling updateSubscribers() for every change stream
          // event and instead we batch these calls, processing multiple events at once, no more
          // than
          // BATCH_SIZE at a time

          if (shouldUpdate && (cursor.available() == 0 || currentUpdates >= BATCH_SIZE)) {
            // reset the flags
            shouldUpdate = false;
            currentUpdates = 0;

            // and process the updates
            updateSubscribers();
          }

          // wait for and retrieve the next change stream event
          ChangeStreamDocument<Document> event = cursor.next();

          // retrieve the document's id
          BsonDocument key = event.getDocumentKey();
          if (key == null) {
            // nothing to do, not related to a single document
            // e.g., this is a collection rename
            continue;
          }
          @SuppressWarnings("NullAway") // the document key should always have an _id field
          final var id = key.get(Schema.ID).asString().getValue();
          final var op = event.getOperationType();

          if (op == OperationType.INVALIDATE) {
            // in the case of an invalidate operation, return the cluster time, so that we can
            // restart the stream from that point
            Document doc = getNonNullDocument(event, op, id);
            return doc.get(Schema.CLUSTER_TIME, BsonTimestamp.class);

          } else if (op == OperationType.INSERT
              || op == OperationType.UPDATE
              || op == OperationType.REPLACE) {
            Document doc = getNonNullDocument(event, op, id);
            store.put(id, doc.getString(Schema.VALUE));

            shouldUpdate = true;
            currentUpdates++;

          } else if (op == OperationType.DELETE) {
            store.remove(id);

            shouldUpdate = true;
            currentUpdates++;
          } else {
            log.log(
                Level.WARNING, () -> format("Nothing to do for op %s (_id=%s)", op.getValue(), id));
          }
        }
      }

      // if we have reached this point, we are most likely stopping the app (thread was interrupted)
      // return an invalid value to prevent receiving any events, in case the calling code
      // incorrectly continues processing
      return new BsonTimestamp(Long.MAX_VALUE);
    }
  }
}
