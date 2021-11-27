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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
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
  public static final boolean RELOAD_ON_DEMAND = false;
  private static final int BATCH_SIZE = 100;
  private static final Logger log = Logger.getLogger(MongoDbStore.class.getName());
  private static final Map<String, String> store = new ConcurrentHashMap<>();
  protected final String connectionString;
  protected final String dbName;
  protected final String collectionName;
  protected final boolean changeStreamEnabled;
  protected final MongoClient mongoClient;

  /**
   * Class constructor that initializes the MongoDB database that will provide key,value pairs.
   *
   * <p>If change streams are enabled, the client connecting to the database must have the <code>
   * changeStream</code> and <code>find</code> privileges on the underlying collection.
   *
   * @see <a href="https://docs.mongodb.com/manual/changeStreams/#access-control">Change Streams
   *     Access Control</a>
   * @param connectionString a valid MongoDB connection string (e.g. "mongodb://[::1]:27017/")
   * @param dbName the database to connect to
   * @param collectionName the collection that holds the key,value pairs
   * @param changeStreamEnabled if <code>true</code>, the implementation will load all Props once
   *     and then open a change stream and watch for any insert/update/delete operations. The stream
   *     will be restarted on errors and invalidate operations; if <code>false</code>, the full list
   *     of Props will be synchronously re-read on every {@link #get()} operation.
   */
  public MongoDbStore(
      String connectionString, String dbName, String collectionName, boolean changeStreamEnabled) {
    // ensure the client connection is valid
    mongoClient = initClient(connectionString);

    this.connectionString = connectionString;
    this.dbName = dbName;
    this.collectionName = collectionName;

    // change streams do not work without an oplog
    Document replSetStatus = getReplSetStatus(mongoClient);
    boolean clientSupportsChangeStreams =
        isReplicaSet(replSetStatus) || isShardedCluster(mongoClient);
    if (!clientSupportsChangeStreams && changeStreamEnabled) {
      // print a warning in case the user assumes change streams will function
      log.warning(
          () ->
              format(
                  "%s did not connect to a replica set or cluster; change streams cannot be enabled",
                  connectionString));
    }

    store.putAll(loadAllKeyValues());

    this.changeStreamEnabled = changeStreamEnabled && clientSupportsChangeStreams;
    if (this.changeStreamEnabled) {
      // schedule the async processing of the change stream
      // starting at the cluster time reported by the initial status call
      @SuppressWarnings({"FutureReturnValueIgnored", "UnusedVariable"})
      Future<?> future =
          BackgroundExecutorFactory.create(1)
              .submit(new ChangeStreamWatcher(getClusterTime(replSetStatus)));
    }
  }

  /**
   * Retrieves the replica set status on the provided client.
   *
   * @param client the client connection
   * @return the results of <code>rs.status()</code>
   */
  static Document getReplSetStatus(MongoClient client) {
    return client.getDatabase("admin").runCommand(new Document("replSetGetStatus", 1));
  }

  /**
   * Determines if the client is connected to a replica set.
   *
   * @param status the result of {@link #getReplSetStatus(MongoClient)}
   * @return true if the provided status indicates a replicaset
   */
  static boolean isReplicaSet(Document status) {
    // if the answer does not indicate success
    if (status.getDouble("ok") != 1d || status.containsKey("errmsg")) {
      // stop here
      return false;
    }

    // we should have at least one primary
    List<Document> members = status.getList("members", Document.class);
    return members.stream()
        .map(m -> m.getString("stateStr"))
        .anyMatch(s -> Objects.equals(s, "PRIMARY"));
  }

  /**
   * Retrieves the current cluster time of the provided status operation.
   *
   * @param status the result of {@link #getReplSetStatus(MongoClient)}
   * @return a {@link BsonTimestamp} indicating the cluster's time
   */
  static BsonTimestamp getClusterTime(Document status) {
    Document data = status.get("$clusterTime", Document.class);
    return data.get("clusterTime", BsonTimestamp.class);
  }

  /**
   * Determines if the client is connected to a sharded cluster.
   *
   * @param client the client connection
   * @return true if the connection points to a sharded cluster
   */
  static boolean isShardedCluster(MongoClient client) {
    MongoDatabase configDB = client.getDatabase("config");
    Document version = configDB.getCollection("version").find().limit(1).first();

    // if we're not running this command against a MongoS
    if (version == null) {
      // stop here
      return false;
    }

    // we should have at least one shard
    return configDB.getCollection("shards").countDocuments() > 0;
  }

  /**
   * Initializes a {@link MongoCollection}.
   *
   * <p>This method can be overwritten by a subclass, if a different configuration is required.
   *
   * @return an initialized object
   */
  protected MongoCollection<Document> getCollection() {
    return mongoClient
        .getDatabase(dbName)
        .getCollection(collectionName)
        .withReadPreference(ReadPreference.primaryPreferred())
        .withReadConcern(ReadConcern.MAJORITY);
  }

  /**
   * Initializes a {@link MongoClient} from the specified connection string.
   *
   * <p>This method can be overwritten by a subclass, if a more advanced client configuration is
   * required.
   *
   * @param connectionString the MongoDB connection string
   * @return an initialized connection to a valid cluster
   */
  protected MongoClient initClient(String connectionString) {
    return MongoClients.create(new ConnectionString(connectionString));
  }

  /**
   * Initializes the {@link #store} with all the currently defined props in the underlying
   * collection.
   *
   * @return an unmodifiable map that contains all the key,value pairs found in the provided
   *     collection
   */
  private Map<String, String> loadAllKeyValues() {
    Map<String, String> results = new HashMap<>();
    getCollection()
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
      return loadAllKeyValues();
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
              getCollection()
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

      // initialize last op time as current cluster time
      BsonTimestamp lastOpClusterTime = getClusterTime(getReplSetStatus(mongoClient));
      BsonDocument resumeToken = null;

      // iterate over the change stream and ensure the cursor is closed before returning
      try (MongoCursor<ChangeStreamDocument<Document>> cursor = changeStream.iterator()) {
        // stop processing if the current thread is interrupted (i.e., the app is shutting down)
        while (cursor.available() > 0) {
          // ensure that we avoid calling updateSubscribers() for every change stream
          // event; we will, however, batch these events into no more than BATCH_SIZE per
          // subscriber update
          if (shouldUpdate && (cursor.available() == 0 || currentUpdates >= BATCH_SIZE)) {
            // reset the flags
            shouldUpdate = false;
            currentUpdates = 0;

            // and process the updates
            updateSubscribers();
          }

          // wait for and retrieve the next change stream event
          ChangeStreamDocument<Document> event;
          try {
            event = cursor.next();
          } catch (RuntimeException e) {
            // if the stream fails for any reason, restart after the last successful op
            return lastOpClusterTime;
          }

          final var op = event.getOperationType();
          resumeToken = event.getResumeToken();

          // check for invalidations
          if (op == OperationType.INVALIDATE) {
            // in the case of an invalidate operation, return the cluster time
            // and restart the stream from that point
            return event.getClusterTime();
          }

          // mark the last successful op's cluster time
          lastOpClusterTime = event.getClusterTime();

          // retrieve the document's id
          BsonDocument key = event.getDocumentKey();
          if (key == null) {
            // nothing to do, not related to a single document
            // e.g., this is a collection rename or drop
            continue;
          }

          @SuppressWarnings("NullAway") // the document key should always have an _id field
          final var id = key.get(Schema.ID).asString().getValue();

          if (op == OperationType.INSERT
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
      } finally {
        // if any updates were stored before exiting, ensure we send them to subscribers before
        // returning
        if (shouldUpdate) {
          updateSubscribers();
        }
      }

      // if we have reached this point, we are most likely stopping the app (thread was interrupted)
      // return an invalid value to prevent receiving any events, in case the calling code
      // incorrectly continues processing
      return new BsonTimestamp(Long.MAX_VALUE);
    }
  }
}
