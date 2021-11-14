/*
 * MIT License
 *
 * Copyright (c) 2021 Mihai Bojin
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
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sh.props.CustomProp;
import sh.props.RegistryBuilder;
import sh.props.converter.Cast;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueResponse;

@SuppressWarnings("NullAway")
class AwsSecretsManagerIntTest {
  private static final String SECRET_VALUE = UUID.randomUUID().toString();
  // ensure we have two random secret names
  private static final String secret1 =
      format("secret1.%s.%s", System.nanoTime(), UUID.randomUUID());
  private static final String secret2 =
      format("secret2.%s.%s", System.nanoTime(), UUID.randomUUID());
  private static CompletableFuture<PutSecretValueResponse> putSecret1;
  private static CompletableFuture<PutSecretValueResponse> putSecret2;
  private static SecretsManagerAsyncClient client;

  @BeforeAll
  static void beforeAll() {
    // initialize the client
    client = AwsSecretsManager.buildClient(AwsSecretsManager.defaultClientConfiguration(), null);

    // submit requests to asynchronously create secrets
    putSecret1 =
        client.putSecretValue(
            PutSecretValueRequest.builder().secretId(secret1).secretString(SECRET_VALUE).build());
    putSecret2 =
        client.putSecretValue(
            PutSecretValueRequest.builder().secretId(secret2).secretString(SECRET_VALUE).build());
  }

  /** Wait for the futures responsible for creating secrets for the test environment to complete. */
  static void waitForSecretsToBeCreated() {
    putSecret1.join();
    putSecret2.join();
  }

  @AfterAll
  static void afterAll() {
    // before completing the test, ensure the secrets are deleted
    client.deleteSecret(DeleteSecretRequest.builder().secretId(secret1).build()).join();
    client.deleteSecret(DeleteSecretRequest.builder().secretId(secret2).build()).join();

    // then close the client
    client.close();
  }

  @Test
  void secretsCanBeRetrievedFromSecretsManager() {
    // ARRANGE
    waitForSecretsToBeCreated();

    var secretsManager = new AwsSecretsManager();
    var registry = new RegistryBuilder(secretsManager).build();

    // ACT
    CustomProp<String> prop1 = registry.builder(Cast.asString()).secret(true).build(secret1);
    CustomProp<String> prop2 = registry.builder(Cast.asString()).secret(true).build(secret2);
    CustomProp<String> prop3 =
        registry
            .builder(Cast.asString())
            .secret(true)
            .build("someInvalidSecretName" + UUID.randomUUID());

    // ASSERT
    await()
        .timeout(Duration.ofSeconds(5))
        .pollInterval(Duration.ofNanos(100))
        .until(prop1::get, equalTo(SECRET_VALUE));
    await()
        .timeout(Duration.ofSeconds(5))
        .pollInterval(Duration.ofNanos(100))
        .until(prop2::get, equalTo(SECRET_VALUE));

    assertThat(prop3.get(), equalTo(null));
  }
}
