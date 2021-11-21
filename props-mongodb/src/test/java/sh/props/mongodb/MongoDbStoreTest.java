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
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static sh.props.mongodb.MongoDbStore.getCollection;
import static sh.props.mongodb.MongoDbStore.initClient;

import com.mongodb.client.MongoCollection;
import java.time.Duration;
import java.util.Base64;
import java.util.Random;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sh.props.Registry;
import sh.props.RegistryBuilder;

@SuppressWarnings("NullAway")
class MongoDbStoreTest {
  private static final String PROPS = "props";
  private static final String CONN_STRING = connectionString();
  private static String DB_NAME;
  private static MongoCollection<Document> collection;

  @BeforeAll
  static void beforeAll() {
    Assertions.assertDoesNotThrow(
        MongoDbStoreTest::canConnect, "This test needs a valid MongoDB cluster");

    DB_NAME = generateRandomAlphanum();
    collection = getCollection(CONN_STRING, DB_NAME, PROPS);

    // create one object
    collection.insertOne(createProp("my.prop", "value"));
  }

  private static Document createFilter(String key) {
    Document doc = new Document();
    doc.put("_id", key);
    return doc;
  }

  private static Document createProp(String key, String value) {
    Document doc = new Document();
    doc.put("_id", key);
    doc.put("value", value);
    return doc;
  }

  /**
   * Creates a connection string using any values provided via System Properties (<code>hosts</code>
   * ); defaults to <code>127.0.0.1:27017</code>.
   *
   * @return a connection string
   */
  private static String connectionString() {
    String connString =
        "mongodb://%s/?maxPoolSize=2&serverSelectionTimeoutMS=2000&connectTimeoutMS=2000"
            + "&socketTimeoutMS=2000&w=majority&readPreference=primaryPreferred";
    String hosts = System.getProperties().getProperty("hosts", "127.0.0.1:27017");
    return format(connString, hosts);
  }

  private static String generateRandomAlphanum() {
    byte[] data = new byte[7];
    new Random().nextBytes(data);
    String encoded = Base64.getMimeEncoder().encodeToString(data);
    return encoded.replaceAll("(?i)[^a-z0-9]+", "").toLowerCase();
  }

  private static void canConnect() {
    var client = initClient(CONN_STRING);
    client.startSession();
  }

  @Test
  void mongoDbStore() {
    // ARRANGE
    var source = new MongoDbStore(CONN_STRING, DB_NAME, PROPS);

    // ACT
    Registry registry = new RegistryBuilder(source).build();

    // ASSERT
    await().until(() -> registry.get("my.prop"), equalTo("value"));

    collection.insertOne(createProp("my.prop2", "value"));
    await().timeout(Duration.ofSeconds(30)).until(() -> registry.get("my.prop2"), equalTo("value"));

    collection.replaceOne(createFilter("my.prop2"), createProp("my.prop2", "value2"));
    await().until(() -> registry.get("my.prop2"), equalTo("value2"));
  }
}
