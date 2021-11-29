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
import static sh.props.aws.AwsHelpers.buildClient;
import static sh.props.aws.AwsHelpers.defaultClientConfiguration;
import static sh.props.aws.AwsHelpers.getSecretValue;
import static sh.props.aws.AwsHelpers.listSecrets;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import sh.props.source.Source;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;

/** {@link Source} implementation that loads secrets from AWS SecretsManager. */
public class AwsSecretsManager extends Source {
  public static final String ID = "aws-secretsmanager";
  private static final Logger log = Logger.getLogger(AwsSecretsManager.class.getName());
  private final Map<String, String> secrets = new ConcurrentHashMap<>();

  private final List<Region> regions;
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
  public AwsSecretsManager(String... regions) {
    this(defaultClientConfiguration(), regions);
  }

  /**
   * More specific constructor that allows customizing Aws's client configuration.
   *
   * @param configuration the configuration to pass when building the client
   * @param regions the AWS region (or regions) to use for retrieving values.
   */
  public AwsSecretsManager(ClientOverrideConfiguration configuration, String... regions) {
    if (regions == null || regions.length == 0) {
      // if no region is specified, use the default, as determined by AWS's implementation
      this.regions = List.of();
      this.clients = List.of(buildClient(configuration, null));
    } else {
      // otherwise, create one client for each specified region
      this.regions = Stream.of(regions).map(Region::of).collect(toList());
      this.clients =
          this.regions.stream().map(region -> buildClient(configuration, region)).collect(toList());
    }
  }

  @Override
  public Map<String, String> get() {
    var definedSecrets = listSecrets(clients.get(0));
    retrieveSecrets(definedSecrets);
    return secrets;
  }

  /**
   * Uses {@link SecretsManagerAsyncClient} to schedule the retrieval of all the defined secrets.
   * Whenever a secret is retrieved, the underlying store ({@link #secrets}) will be updated.
   *
   * @param allSecretIds the list of secret ids to retrieve
   */
  void retrieveSecrets(List<String> allSecretIds) {
    var futures =
        IntStream.range(0, allSecretIds.size())
            .mapToObj(
                i -> {
                  var client = clients.get(i % clients.size());
                  var key = allSecretIds.get(i);
                  return getSecretValue(client, key)
                      .handle(
                          (response, err) -> AwsHelpers.processSecretResponse(response, err, key))
                      .thenAccept(value -> this.secrets.put(key, value));
                })
            .toArray(CompletableFuture[]::new);

    // wait for all secrets to be retrieved before returning
    try {
      CompletableFuture.allOf(futures).join();
    } catch (CompletionException e) {
      log.log(Level.WARNING, e, () -> "Unexpected exception while retrieving secrets");
    }
  }
}
