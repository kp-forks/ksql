/*
 * Copyright 2024 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.confluent.ksql.security.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.confluent.ksql.security.KsqlClientConfig;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.config.ConfigException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OAuthBearerCredentialsTest {

  @Mock
  CachedOAuthTokenRetriever tokenRetriever;

  OAuthBearerCredentials credentials = new OAuthBearerCredentials();

  private Map<String, String> configMap;

  @Before
  public void initializeConfigMap() {
    configMap = new HashMap<>();
    configMap.put(KsqlClientConfig.BEARER_AUTH_SCOPE, "test-scope");
    configMap.put(KsqlClientConfig.BEARER_AUTH_CLIENT_SECRET, "my-secret");
    configMap.put(KsqlClientConfig.BEARER_AUTH_CLIENT_ID, "my-id");
    configMap.put(KsqlClientConfig.BEARER_AUTH_TOKEN_ENDPOINT_URL, "https://okta.com");
  }

  @Test
  public void testValidation() {
    Map<String, String> config = new HashMap<>();
    assertThrows(ConfigException.class, () -> credentials.configure(config));
  }

  @Test
  public void shouldGetBearerToken() {
    when(tokenRetriever.getToken()).thenReturn("expectedToken");

    credentials.init(tokenRetriever);

    assertEquals("expectedToken", credentials.retrieveToken());
    assertEquals("Bearer expectedToken", credentials.getAuthHeader());
  }

  @Test
  public void shouldGetExceptionWhenConfiguredInsufficientConfigs() {
    List<String> optionalConfigs = Arrays.asList(KsqlClientConfig.BEARER_AUTH_SCOPE,
        KsqlClientConfig.BEARER_AUTH_SCOPE_CLAIM_NAME,
        KsqlClientConfig.BEARER_AUTH_SUB_CLAIM_NAME);
    for (String missingKey : configMap.keySet()) {
      // ignoring optional keys
      if (optionalConfigs.contains(missingKey)) {
        continue;
      }

      Assert.assertThrows(
          String.format("The OAuth configuration option %s value must be non-null", missingKey),
          ConfigException.class,
          () -> {
            credentials.configure(getInsufficientConfigs(missingKey));
          });

    }
  }

  @Test
  public void testClientSslConfigurations() {

    Map<String, String> configsWithSsl = new HashMap<>(configMap);
    configsWithSsl.put("ssl.truststore.location", "truststore.jks");
    configsWithSsl.put("ssl.truststore.password", "password");

    // SSL configurations should get loaded if present in configuration
    Assert.assertThrows("Message", KafkaException.class,
        () -> {
          credentials.configure(configsWithSsl);
        });

  }

  private Map<String, Object> getInsufficientConfigs(String missingConfig) {
    Map<String, Object> insufficientConfigs = new HashMap<>(configMap);
    insufficientConfigs.remove(missingConfig);
    return insufficientConfigs;
  }

}
