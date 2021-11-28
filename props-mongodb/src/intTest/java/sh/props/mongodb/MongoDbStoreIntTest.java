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
@SuppressWarnings("NullAway")
class MongoDbStoreIntTest {
  @Container
  private static final MongoDBContainer mongoDBContainer =
      new MongoDBContainer(DockerImageName.parse("mongo:5.0.4-focal")).withExposedPorts(27017);

  private static final String PROPS = "props";
  private MongoClient mongoClient;
  private String connString;
  private String dbName;
  private MongoCollection<Document> collection;

  @BeforeAll
  static void beforeAll() {
    mongoDBContainer.start();
    assertThat(mongoDBContainer.isRunning(), equalTo(true));
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
    collection.insertOne(createProp("my.prop", "value"));
  }

  @AfterEach
  void tearDown() {
    mongoClient.close();
  }

  @Test
  void watchCollectionChangeStream() {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS, WATCH_CHANGE_STREAM);

    // ACT
    Registry registry = new RegistryBuilder(source).build();

    // ASSERT
    await().until(() -> registry.get("my.prop"), equalTo("value"));

    collection.insertOne(createProp("my.prop2", "value"));
    await().until(() -> registry.get("my.prop2"), equalTo("value"));

    collection.replaceOne(createFilter("my.prop2"), createProp("my.prop2", "value2"));
    await().until(() -> registry.get("my.prop2"), equalTo("value2"));
  }

  @Test
  void reloadAllDataFromCollection() {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS, RELOAD_ON_DEMAND);

    // ACT
    Registry registry = new RegistryBuilder(source).build();

    // ASSERT
    await().until(() -> registry.get("my.prop"), equalTo("value"));

    collection.insertOne(createProp("my.prop2", "value"));
    assertThat(registry.get("my.prop2"), nullValue());

    source.refresh();
    assertThat(registry.get("my.prop2"), equalTo("value"));

    collection.replaceOne(createFilter("my.prop2"), createProp("my.prop2", "value2"));
    assertThat(registry.get("my.prop2"), equalTo("value"));

    source.refresh();
    assertThat(registry.get("my.prop2"), equalTo("value2"));
  }

  @Test
  void dropBackingDatabase() {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS, WATCH_CHANGE_STREAM);
    Registry registry = new RegistryBuilder(source).build();

    // ACT
    assertThat(registry.get("my.prop"), equalTo("value"));
    mongoClient.getDatabase(dbName).drop();
    collection.insertOne(createProp("my.prop", "value2"));

    // ASSERT
    await().until(() -> registry.get("my.prop"), equalTo("value2"));
  }

  @Test
  void dropBackingCollection() {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS, WATCH_CHANGE_STREAM);
    Registry registry = new RegistryBuilder(source).build();

    // ACT
    assertThat(registry.get("my.prop"), equalTo("value"));
    mongoClient.getDatabase(dbName).getCollection(PROPS).drop();
    collection.insertOne(createProp("my.prop", "value2"));

    // ASSERT
    await().until(() -> registry.get("my.prop"), equalTo("value2"));
  }

  @Test
  void renameBackingCollection() {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS, WATCH_CHANGE_STREAM);
    Registry registry = new RegistryBuilder(source).build();

    // ACT
    assertThat(registry.get("my.prop"), equalTo("value"));
    mongoClient
        .getDatabase(dbName)
        .getCollection(PROPS)
        .renameCollection(new MongoNamespace(dbName, PROPS + "_renamed"));
    collection.insertOne(createProp("my.prop", "value2"));

    // ASSERT
    await().until(() -> registry.get("my.prop"), equalTo("value2"));
  }

  @Test
  @Timeout(value = 5)
  void replicaSetStepDown() throws Exception {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS, WATCH_CHANGE_STREAM);
    Registry registry = new RegistryBuilder(source).build();

    // ACT
    assertThat(registry.get("my.prop"), equalTo("value"));
    replicaSetPrimaryStepDown(mongoClient, 1);

    boolean inserted = false;
    while (!inserted) {
      try {
        collection.replaceOne(createFilter("my.prop"), createProp("my.prop", "value2"));
        inserted = true;
      } catch (MongoNotPrimaryException e) {
        // while the primary is not elected
        Thread.sleep(500);
      }
    }

    // ASSERT
    await().until(() -> registry.get("my.prop"), equalTo("value2"));
  }
}
