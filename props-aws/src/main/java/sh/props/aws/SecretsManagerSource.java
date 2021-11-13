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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sh.props.source.Source;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

public class SecretsManagerSource extends Source {
  public static final String ID = "aws-secretsmanager";
  private final List<Region> regions;
  private final List<SecretsManagerClient> clients;

  /**
   * Class constructor.
   *
   * <p>Specifying more than one region is useful when dealing with multi-region secrets, if you
   * want to load balance operations across these regions.
   *
   * @see <a
   *     href="https://docs.aws.amazon.com/secretsmanager/latest/userguide/create-manage-multi-region-secrets.html">AWS
   *     Multi Region Secrets</a>
   * @param regions the AWS region (or regions) to use for retrieving values.
   */
  public SecretsManagerSource(String... regions) {
    if (regions == null || regions.length == 0) {
      throw new IllegalArgumentException("Need to specify at least one region");
    }

    this.regions = Stream.of(regions).map(Region::of).collect(toList());
    this.clients = this.regions.stream().map(this::buildClient).collect(toList());
  }

  /**
   * Initializes a client.
   *
   * @param region the region that the client should use
   * @return an initialized object
   */
  SecretsManagerClient buildClient(Region region) {
    return SecretsManagerClient.builder().region(region).build();
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public Map<String, String> get() {
    var definedSecrets = listSecrets(clients.get(0));
    return getSecrets(definedSecrets);
  }

  List<String> listSecrets(SecretsManagerClient client) throws SecretsManagerException {
    ListSecretsResponse secretsResponse = client.listSecrets();
    List<SecretListEntry> secrets = secretsResponse.secretList();

    return secrets.stream().map(SecretListEntry::name).collect(Collectors.toList());
  }

  // TODO: finish the impl
  Map<String, String> getSecrets(List<String> secrets) {
    // parallelize retrieving secrets
    // handle throttling
    return Map.of();
  }
}
