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
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bson.Document;
import sh.props.source.Source;
import sh.props.util.BackgroundExecutorFactory;

public class MongoDbStore extends Source {
  public static final String ID = "mongodb";
  private static final Logger log = Logger.getLogger(MongoDbStore.class.getName());

  private static final Map<String, String> store = new ConcurrentHashMap<>();

  /**
   * Class constructor that initializes the MongoDB database that will provide key,value pairs.
   * TODO: document https://docs.mongodb.com/manual/changeStreams/#access-control
   *
   * @param connectionString a valid MongoDB connection string (e.g.
   *     "mongodb://[::1]:27017/defaultDatabase")
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public MongoDbStore(String connectionString, String database, String collection) {
    MongoCollection<Document> coll = connect(connectionString, database, collection);

    // open a change stream to capture any changes
    ChangeStreamIterable<Document> it = coll.watch().fullDocument(FullDocument.UPDATE_LOOKUP);

    // load the initial elements
    for (Document document : coll.find()) {
      store.put(document.getString("_id"), document.getString("value"));
    }

    // schedule the async processing of the change stream
    BackgroundExecutorFactory.create(1)
        .submit(
            () -> {
              for (ChangeStreamDocument<Document> event : it) {
                Document document = event.getFullDocument();
                if (document == null) {
                  log.warning("Invalid change stream event. Skipping!");
                  continue;
                }
                store.put(document.getString("_id"), document.getString("value"));
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
  static MongoCollection<Document> connect(
      String connectionString, String database, String collection) {
    MongoClientSettings settings =
        MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(connectionString))
            .build();
    MongoClient mongoClient = MongoClients.create(settings);

    MongoDatabase db = mongoClient.getDatabase(database);
    return db.getCollection(collection)
        .withReadPreference(ReadPreference.primaryPreferred())
        .withReadConcern(ReadConcern.MAJORITY)
        .withWriteConcern(WriteConcern.MAJORITY);
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
