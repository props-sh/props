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

package sh.props.aws;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static sh.props.aws.AwsHelpers.buildClient;
import static sh.props.aws.AwsHelpers.defaultClientConfiguration;
import static sh.props.aws.AwsHelpers.getSecretValue;
import static sh.props.aws.AwsHelpers.listSecrets;

import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sh.props.CustomProp;
import sh.props.RegistryBuilder;
import sh.props.converter.Cast;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;

@SuppressWarnings("NullAway")
class AwsSecretsManagerIntTest {
  private static final String SECRET_VALUE = UUID.randomUUID().toString();
  // ensure we have two random secret names
  private static final String secret1 =
      format("secret1.%s.%s", System.nanoTime(), UUID.randomUUID());
  private static final String secret2 =
      format("secret2.%s.%s", System.nanoTime(), UUID.randomUUID());
  private static SecretsManagerAsyncClient client;

  @BeforeAll
  static void beforeAll() {
    // initialize the client
    client = buildClient(defaultClientConfiguration(), null);

    // create secrets for the test env.
    var key1 = CreateSecretRequest.builder().name(secret1).secretString(SECRET_VALUE).build();
    client.createSecret(key1).join();

    var key2 = CreateSecretRequest.builder().name(secret2).secretString(SECRET_VALUE).build();
    client.createSecret(key2).join();

    Awaitility.setDefaultTimeout(5, SECONDS);
    Awaitility.setDefaultPollDelay(500, MILLISECONDS);
    Awaitility.setDefaultPollInterval(1, SECONDS);

    // ensure the secrets can be retrieved and listed
    // the secrets don't immediately become available in AWS SecretsManager
    // and as such, we much first wait and ensure they are properly set-up by the test
    await().until(() -> getSecretValue(client, secret1).join().secretString(), notNullValue());
    await()
        .pollDelay(Duration.ZERO)
        .until(() -> getSecretValue(client, secret2).join().secretString(), notNullValue());
    await().until(() -> listSecrets(client), hasSize(equalTo(2)));
  }

  @AfterAll
  static void afterAll() {
    // before completing the test, ensure the secrets are deleted
    client.deleteSecret(deleteRequest(secret1)).join();
    client.deleteSecret(deleteRequest(secret2)).join();

    // then close the client
    client.close();
  }

  private static DeleteSecretRequest deleteRequest(String id) {
    return DeleteSecretRequest.builder().secretId(id).forceDeleteWithoutRecovery(true).build();
  }

  @Test
  @Timeout(value = 5)
  void secretsCanBeRetrievedFromSecretsManager() {
    // ARRANGE
    var secretsManager = new AwsSecretsManagerOnDemand();
    var registry = new RegistryBuilder(secretsManager).build();

    var invalidKey = "someInvalidSecretName" + UUID.randomUUID();

    // ACT
    CustomProp<String> prop1 = registry.builder(Cast.asString()).secret(true).build(secret1);
    CustomProp<String> prop2 = registry.builder(Cast.asString()).secret(true).build(secret2);
    CustomProp<String> prop3 = registry.builder(Cast.asString()).secret(true).build(invalidKey);

    // ASSERT
    await()
        .pollDelay(Duration.ZERO)
        .pollInterval(Duration.ofNanos(100))
        .until(prop1::get, equalTo(SECRET_VALUE));
    await()
        .pollDelay(Duration.ZERO)
        .pollInterval(Duration.ofNanos(100))
        .until(prop2::get, equalTo(SECRET_VALUE));

    assertThat(prop3.get(), equalTo(null));
  }

  @Test
  @Timeout(value = 5)
  void loadSecretsOnDemand() {
    // ARRANGE
    var secretsManager = new AwsSecretsManager();
    var registry = new RegistryBuilder(secretsManager).build();

    var invalidKey = "someInvalidSecretName" + UUID.randomUUID();

    // ACT
    CustomProp<String> prop1 = registry.builder(Cast.asString()).secret(true).build(secret1);
    String value2 = registry.get(secret2, Cast.asString());
    String invalid = registry.get(invalidKey, Cast.asString());

    // ASSERT
    await()
        .pollDelay(Duration.ZERO)
        .pollInterval(Duration.ofNanos(100))
        .until(prop1::get, equalTo(SECRET_VALUE));
    assertThat(value2, equalTo(SECRET_VALUE));
    assertThat(invalid, nullValue());
  }
}
