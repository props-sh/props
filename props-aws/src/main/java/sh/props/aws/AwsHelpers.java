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

import static java.util.stream.Collectors.toList;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import sh.props.annotations.Nullable;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

class AwsHelpers {
  /**
   * Default implementation for AWS's client configuration, that set a default timeout of 5 seconds
   * and will retry calls failed due to throttling up to 5 times (see {@link
   * BackoffStrategy#defaultThrottlingStrategy()}).
   *
   * @return a valid AWS configuration
   */
  static ClientOverrideConfiguration defaultClientConfiguration() {
    return ClientOverrideConfiguration.builder()
        .apiCallAttemptTimeout(Duration.ofSeconds(5))
        .retryPolicy(
            RetryPolicy.builder()
                .backoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                .numRetries(5)
                .build())
        .build();
  }

  /**
   * Initializes a client.
   *
   * @param config the AWS client configuration
   * @param region the region that the client should use; if null, the implementation will choose
   * @return an initialized object
   */
  static SecretsManagerAsyncClient buildClient(
      ClientOverrideConfiguration config, @Nullable Region region) {
    var builder = SecretsManagerAsyncClient.builder().overrideConfiguration(config);
    if (region != null) {
      builder.region(region);
    }
    return builder.build();
  }

  /**
   * Uses the provided client to retrieve a secret by its id.
   *
   * @param client the client which will execute the operation
   * @param id the secret to retrieve
   * @return a {@link CompletableFuture} that, when completed, will return the secret's value
   */
  static CompletableFuture<GetSecretValueResponse> getSecretValue(
      SecretsManagerAsyncClient client, String id) {
    return client.getSecretValue(GetSecretValueRequest.builder().secretId(id).build());
  }

  /**
   * Retrieves a list of all the defined secrets. We need to wait for this operation to complete, as
   * we need the list of secrets before retrieving their values.
   *
   * @param client the client which will execute the operation
   * @return a list of defined secrets
   * @throws SecretsManagerException if the operation fails
   */
  static List<String> listSecrets(SecretsManagerAsyncClient client) throws SecretsManagerException {
    return client
        .listSecrets()
        .thenApply(ListSecretsResponse::secretList)
        .thenApply(secrets -> secrets.stream().map(SecretListEntry::name).collect(toList()))
        .join();
  }
}
