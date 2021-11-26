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

package sh.props.mongodb.testfixtures;

import static java.lang.String.format;

import com.mongodb.ConnectionString;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.Base64;
import java.util.Random;
import org.bson.Document;

public class MongoDbFixtures {

  /**
   * Creates a connection string using any values provided via System Properties (<code>hosts</code>
   * ); defaults to <code>127.0.0.1:27017</code>.
   *
   * @return a connection string
   */
  public static String connectionString(int port) {
    String connString =
        "mongodb://127.0.0.1:%d/?maxPoolSize=2&serverSelectionTimeoutMS=2000&connectTimeoutMS=2000"
            + "&socketTimeoutMS=2000&w=majority&readPreference=primaryPreferred";
    return format(connString, port);
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
   * Initializes a {@link MongoCollection} using the provided parameters.
   *
   * @param connectionString the connection string which will be used to init the client
   * @param database the database to connect to
   * @param collection the collection to use
   * @return an initialized object
   */
  public static MongoCollection<Document> getCollection(
      String connectionString, String database, String collection) {
    MongoClient mongoClient = initClient(connectionString);

    MongoDatabase db = mongoClient.getDatabase(database);
    return db.getCollection(collection)
        .withReadPreference(ReadPreference.primaryPreferred())
        .withReadConcern(ReadConcern.MAJORITY);
  }

  /**
   * Simple method that generates a short random alphanumeric string.
   *
   * @return a generated string
   */
  public static String generateRandomAlphanum() {
    byte[] data = new byte[7];
    new Random().nextBytes(data);
    String encoded = Base64.getMimeEncoder().encodeToString(data);
    return encoded.replaceAll("(?i)[^a-z0-9]+", "").toLowerCase();
  }

  /**
   * Creates a filter to be used in a MongoDB query.
   *
   * @param key the Prop key to filter on
   * @return a filter document
   */
  public static Document createFilter(String key) {
    Document doc = new Document();
    doc.put("_id", key);
    return doc;
  }

  /**
   * Creates a key,value Prop.
   *
   * @param key the Prop's key
   * @param value the Prop's value
   * @return a Prop document
   */
  public static Document createProp(String key, String value) {
    Document doc = new Document();
    doc.put("_id", key);
    doc.put("value", value);
    return doc;
  }
}
