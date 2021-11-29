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

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import sh.props.annotations.Nullable;
import sh.props.source.OnDemandSource;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class AwsSecretsManagerOnDemand extends OnDemandSource {
  public static final String ID = "aws-secretsmanager-ondemand";
  private static final Logger log = Logger.getLogger(AwsSecretsManagerOnDemand.class.getName());
  private final Random random = new Random();

  private final List<SecretsManagerAsyncClient> clients;

  /**
   * Class constructor.
   *
   * <p>Specifying more than one region is useful when dealing with multi-region secrets, if you
   * want to load balance operations across these regions.
   *
   * <p>If a region is not specified, the implementation will rely on Amazon's default behaviour of
   * consulting the <code>AWS_REGION</code> env. variable, its standard configuration files or, if
   * running inside AWS, the metadata endpoint.
   *
   * @see <a
   *     href="https://docs.aws.amazon.com/secretsmanager/latest/userguide/create-manage-multi-region-secrets.html">AWS
   *     Multi Region Secrets</a>
   * @see <a
   *     href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/region-selection.html">AWS
   *     Region Selection</a>
   * @param regions the AWS region (or regions) to use for retrieving values.
   */
  public AwsSecretsManagerOnDemand(String... regions) {
    this(defaultClientConfiguration(), regions);
  }

  /**
   * More specific constructor that allows customizing Aws's client configuration.
   *
   * @param configuration the configuration to pass when building the client
   * @param regions the AWS region (or regions) to use for retrieving values.
   */
  public AwsSecretsManagerOnDemand(ClientOverrideConfiguration configuration, String... regions) {
    if (regions == null || regions.length == 0) {
      // if no region is specified, use the default, as determined by AWS's implementation
      this.clients = List.of(buildClient(configuration, null));
    } else {
      // otherwise, create one client for each specified region
      this.clients =
          Stream.of(regions)
              .map(Region::of)
              .map(region -> buildClient(configuration, region))
              .collect(toList());
    }
  }

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

  @Override
  public String id() {
    return ID;
  }

  /**
   * Uses one of the configured clients to retrieve a secret by its id.
   *
   * @param key the secret to retrieve
   * @return a {@link CompletableFuture} that, when completed, will return the secret's value
   */
  @Override
  protected String loadKey(String key) {
    // chose a client
    int i = 0;
    if (clients.size() > 1) {
      // if more than one region was provided, randomly choose one
      i = random.nextInt(clients.size());
    }

    return clients
        .get(i)
        .getSecretValue(GetSecretValueRequest.builder().secretId(key).build())
        .handle(this::processSecretResponse)
        .join();
  }

  @Nullable
  private String processSecretResponse(GetSecretValueResponse response, Throwable error) {
    if (response != null) {
      // Decrypts secret using the associated KMS CMK.
      // Depending on whether the secret is a string or binary, one of these fields
      // will be populated.
      if (response.secretString() != null) {
        return response.secretString();
      } else {
        return new String(
            Base64.getDecoder().decode(response.secretBinary().asByteBuffer()).array(),
            Charset.defaultCharset());
      }
    }

    // log the exception
    log.log(Level.WARNING, error, () -> "Unexpected exception while retrieving the secret's value");
    return null;
  }
}
