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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import org.bson.Document;
import sh.props.source.Source;
import sh.props.util.BackgroundExecutorFactory;

public class MongoDbStore extends Source {
  public static final String ID = "mongodb";
  private static final Logger log = Logger.getLogger(MongoDbStore.class.getName());

  private static final Map<String, String> store = new ConcurrentHashMap<>();
  private static final int THRESHOLD = 100;

  /**
   * Class constructor that initializes the MongoDB database that will provide key,value pairs.
   * TODO: document https://docs.mongodb.com/manual/changeStreams/#access-control
   *
   * @param connectionString a valid MongoDB connection string (e.g.
   *     "mongodb://[::1]:27017/defaultDatabase")
   */
  public MongoDbStore(String connectionString, String database, String collection) {
    MongoCollection<Document> coll = getCollection(connectionString, database, collection);

    // open a change stream to capture any changes
    ChangeStreamIterable<Document> events = coll.watch().fullDocument(FullDocument.UPDATE_LOOKUP);

    // load the initial elements
    for (Document document : coll.find()) {
      store.put(document.getString("_id"), document.getString("value"));
    }

    // schedule the async processing of the change stream
    @SuppressWarnings("FutureReturnValueIgnored")
    Future<?> future =
        BackgroundExecutorFactory.create(1)
            .submit(
                () -> {
                  boolean shouldUpdate = false;
                  int currentUpdates = 0;
                  try (MongoCursor<ChangeStreamDocument<Document>> cursor = events.iterator()) {
                    while (!Thread.currentThread().isInterrupted()) {
                      Document doc = cursor.next().getFullDocument();
                      if (doc != null) {
                        store.put(doc.getString("_id"), doc.getString("value"));
                        shouldUpdate = true;
                        currentUpdates++;
                      } else {
                        log.warning("Invalid change stream event. Skipping!");
                      }

                      // if at least one valid update was registered
                      // and there aren't more updates in-flight
                      // or we've reached the update threshold
                      if (shouldUpdate && (cursor.available() == 0 || currentUpdates > THRESHOLD)) {
                        // reset flags
                        shouldUpdate = false;
                        currentUpdates = 0;
                        // and process the updates
                        updateSubscribers();
                      }
                    }
                  }
                });

    // TODO: cast to specific document type
    //       (https://docs.mongodb.com/manual/reference/change-events/)

    // TODO: read all existing documents

    // TODO: initialize change stream
    //    ChangeStreamPublisher<Document> watch = coll.watch();
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

  @Override
  public String id() {
    return ID;
  }

  @Override
  public Map<String, String> get() {
    return Collections.unmodifiableMap(store);
  }
}
