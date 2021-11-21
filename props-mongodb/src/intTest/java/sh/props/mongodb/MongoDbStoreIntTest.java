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
import static org.hamcrest.MatcherAssert.assertThat;
import static sh.props.mongodb.MongoDbStore.getCollection;
import static sh.props.mongodb.testfixtures.Fixtures.connectionString;
import static sh.props.mongodb.testfixtures.Fixtures.createFilter;
import static sh.props.mongodb.testfixtures.Fixtures.createProp;
import static sh.props.mongodb.testfixtures.Fixtures.generateRandomAlphanum;

import com.mongodb.client.MongoCollection;
import java.time.Duration;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    // create database prop(s)
    collection = getCollection(connString, dbName, PROPS);
    collection.insertOne(createProp("my.prop", "value"));
  }

  @Test
  void mongoDbStore() {
    // ARRANGE
    var source = new MongoDbStore(connString, dbName, PROPS);

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
