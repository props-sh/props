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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static sh.props.mongodb.MongoDbStore.RELOAD_ON_DEMAND;
import static sh.props.mongodb.MongoDbStore.WATCH_CHANGE_STREAM;
import static sh.props.mongodb.testfixtures.MongoDbFixtures.connectionString;
import static sh.props.mongodb.testfixtures.MongoDbFixtures.createFilter;
import static sh.props.mongodb.testfixtures.MongoDbFixtures.createProp;
import static sh.props.mongodb.testfixtures.MongoDbFixtures.generateRandomAlphanum;
import static sh.props.mongodb.testfixtures.MongoDbFixtures.getCollection;
import static sh.props.mongodb.testfixtures.MongoDbFixtures.initClient;
import static sh.props.mongodb.testfixtures.MongoDbFixtures.replicaSetPrimaryStepDown;

import com.mongodb.MongoNamespace;
import com.mongodb.MongoNotPrimaryException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import sh.props.Registry;
import sh.props.RegistryBuilder;

@Testcontainers
@SuppressWarnings({
  "NullAway",
})
class MongoDbStoreIntTest {

  public static final String PROP_SHOULD_BE_LOADED =
      "Expecting the Registry to have loaded values from the MongoDB source";
  private static final String PROPS = "props";
  private static final String KEY_1 = "key1";
  private static final String KEY_2 = "key2";
  private static final String VALUE_1 = "value1";
  private static final String VALUE_2 = "value2";

  @Container
  private static final MongoDBContainer mongoDBContainer =
      new MongoDBContainer(DockerImageName.parse(System.getProperty("testcontainers:mongo")))
          .withExposedPorts(27017);

  private MongoClient mongoClient;
  private String connString;
  private String dbName;
  private MongoCollection<Document> collection;

  @BeforeAll
  static void beforeAll() {
    mongoDBContainer.start();
    assertThat("This test needs a running container", mongoDBContainer.isRunning(), equalTo(true));
  }

  @AfterAll
  static void afterAll() {
    mongoDBContainer.stop();
  }

  @BeforeEach
  void setUp() {
    // initialize the connection string and database name for this test
    connString = connectionString(mongoDBContainer.getMappedPort(27017));
    dbName = generateRandomAlphanum();
    mongoClient = initClient(connString);

    // create database prop(s)
    collection = getCollection(mongoClient, dbName, PROPS);
    collection.insertOne(createProp(KEY_1, VALUE_1));
  }

  @AfterEach
  void tearDown() {
    mongoClient.close();
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // PMD doesn't support awaitility
  void watchCollectionChangeStream() {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS, WATCH_CHANGE_STREAM);

    // ACT
    Registry registry = new RegistryBuilder(source).build();

    // ASSERT
    await().until(() -> registry.get(KEY_1), equalTo(VALUE_1));

    collection.insertOne(createProp(KEY_2, VALUE_1));
    await().until(() -> registry.get(KEY_2), equalTo(VALUE_1));

    collection.replaceOne(createFilter(KEY_2), createProp(KEY_2, VALUE_2));
    await().until(() -> registry.get(KEY_2), equalTo(VALUE_2));
  }

  @Test
  void reloadAllDataFromCollection() {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS, RELOAD_ON_DEMAND);

    // ACT
    Registry registry = new RegistryBuilder(source).build();

    // ASSERT
    await().until(() -> registry.get(KEY_1), equalTo(VALUE_1));

    collection.insertOne(createProp(KEY_2, VALUE_1));
    assertThat(
        "Expecting value to not have been propagated without a refresh",
        registry.get(KEY_2),
        nullValue());

    source.refresh();
    assertThat(
        "Expecting value update to have been propagated after refresh",
        registry.get(KEY_2),
        equalTo(VALUE_1));

    collection.replaceOne(createFilter(KEY_2), createProp(KEY_2, VALUE_2));
    assertThat("Expecting value update to not propagate", registry.get(KEY_2), equalTo(VALUE_1));

    source.refresh();
    assertThat(
        "Expecting value to have been updated after refresh",
        registry.get(KEY_2),
        equalTo(VALUE_2));
  }

  @Test
  void dropBackingDatabase() {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS, WATCH_CHANGE_STREAM);
    Registry registry = new RegistryBuilder(source).build();

    // ACT
    assertThat(PROP_SHOULD_BE_LOADED, registry.get(KEY_1), equalTo(VALUE_1));
    mongoClient.getDatabase(dbName).drop();
    collection.insertOne(createProp(KEY_1, VALUE_2));

    // ASSERT
    // expecting a value update, regardless of the collection drop
    // and the change stream having to have been reinitialized
    await().until(() -> registry.get(KEY_1), equalTo(VALUE_2));
  }

  @Test
  void dropBackingCollection() {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS, WATCH_CHANGE_STREAM);
    Registry registry = new RegistryBuilder(source).build();

    // ACT
    assertThat(PROP_SHOULD_BE_LOADED, registry.get(KEY_1), equalTo(VALUE_1));
    mongoClient.getDatabase(dbName).getCollection(PROPS).drop();
    collection.insertOne(createProp(KEY_1, VALUE_2));

    // ASSERT
    // expecting a value update, regardless of the collection drop
    // and the change stream having to have been reinitialized
    await().until(() -> registry.get(KEY_1), equalTo(VALUE_2));
  }

  @Test
  void renameBackingCollection() {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS, WATCH_CHANGE_STREAM);
    Registry registry = new RegistryBuilder(source).build();

    // ACT
    assertThat(PROP_SHOULD_BE_LOADED, registry.get(KEY_1), equalTo(VALUE_1));
    mongoClient
        .getDatabase(dbName)
        .getCollection(PROPS)
        .renameCollection(new MongoNamespace(dbName, PROPS + "_renamed"));
    collection.insertOne(createProp(KEY_1, VALUE_2));

    // ASSERT
    // expecting a value update, regardless of the collection rename
    // and the change stream having to have been reinitialized
    await().until(() -> registry.get(KEY_1), equalTo(VALUE_2));
  }

  @Test
  @Timeout(value = 5)
  void replicaSetStepDown() throws Exception {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS, WATCH_CHANGE_STREAM);
    Registry registry = new RegistryBuilder(source).build();

    // ACT
    assertThat(PROP_SHOULD_BE_LOADED, registry.get(KEY_1), equalTo(VALUE_1));

    replicaSetPrimaryStepDown(mongoClient, 1);

    // repeatedly update one key,value pair, waiting for a PRIMARY to be elected
    boolean inserted = false;
    while (!inserted) {
      try {
        collection.replaceOne(createFilter(KEY_1), createProp(KEY_1, VALUE_2));
        inserted = true;
      } catch (MongoNotPrimaryException e) {
        // while the primary is not elected
        Thread.sleep(500);
      }
    }

    // ASSERT
    // expecting a value update, regardless of the Primary step-down operation
    // and the change stream having to have been reinitialized
    await().until(() -> registry.get(KEY_1), equalTo(VALUE_2));
  }
}
