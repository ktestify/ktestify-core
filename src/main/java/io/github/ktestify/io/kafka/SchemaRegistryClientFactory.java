/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.ktestify.io.kafka;

import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.config.SchemaRegistryConfig;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating Schema Registry clients using ktestify configuration.
 *
 * <p>This factory provides convenient methods to create pre-configured Schema Registry clients using settings from
 * {@link SchemaRegistryConfig}.
 *
 * <p>Usage:
 *
 * <pre>
 * // Using default configuration
 * SchemaRegistryClient client = SchemaRegistryClientFactory.createClient();
 *
 * // Using custom configuration
 * KtestifyConfig config = ConfigBuilder.create()
 *     .schemaRegistryUrl("http://schema-registry:8081")
 *     .build();
 * SchemaRegistryClient client = SchemaRegistryClientFactory.createClient(config);
 *
 * // Get schema by ID
 * Schema schema = client.getById(1);
 *
 * // Register a new schema
 * int schemaId = client.register("my-topic-value", avroSchema);
 * </pre>
 *
 * @since 0.2.51
 */
public final class SchemaRegistryClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaRegistryClientFactory.class);

    private SchemaRegistryClientFactory() {
        // Utility class
    }

    /**
     * Creates a Schema Registry client using default configuration.
     *
     * @return a new Schema Registry client
     */
    public static SchemaRegistryClient createClient() {
        return createClient(KtestifyConfig.getOrLoad());
    }

    /**
     * Creates a Schema Registry client using the provided configuration.
     *
     * @param config the ktestify configuration
     * @return a new Schema Registry client
     */
    public static SchemaRegistryClient createClient(KtestifyConfig config) {
        SchemaRegistryConfig srConfig = config.getSchemaRegistry();
        return createClient(srConfig);
    }

    /**
     * Creates a Schema Registry client using the provided Schema Registry configuration.
     *
     * @param srConfig the schema registry configuration
     * @return a new Schema Registry client
     */
    public static SchemaRegistryClient createClient(SchemaRegistryConfig srConfig) {
        String baseUrl = srConfig.getUrl();
        int cacheCapacity = srConfig.getCacheCapacity();

        LOG.debug("Creating Schema Registry client with URL: {} and cache capacity: {}", baseUrl, cacheCapacity);

        // Build the REST service configuration with optional authentication and SSL
        Map<String, String> configs = buildRestServiceConfigs(srConfig);
        RestService restService = new RestService(baseUrl);
        restService.configure(configs);

        // Create a cached schema registry client
        return new CachedSchemaRegistryClient(
                restService,
                cacheCapacity,
                null, // Use default providers
                configs);
    }

    /**
     * Creates a Schema Registry client for multiple base URLs (for HA setups).
     *
     * @param baseUrls list of Schema Registry URLs
     * @param cacheCapacity the maximum number of schemas to cache
     * @return a new Schema Registry client
     */
    public static SchemaRegistryClient createClient(List<String> baseUrls, int cacheCapacity) {
        LOG.debug("Creating Schema Registry client with URLs: {} and cache capacity: {}", baseUrls, cacheCapacity);

        Map<String, String> configs = new HashMap<>();
        return new CachedSchemaRegistryClient(baseUrls, cacheCapacity, configs);
    }

    /**
     * Creates a Schema Registry client with custom configuration overrides.
     *
     * @param srConfig the schema registry configuration
     * @param overrides additional configuration properties to override
     * @return a new Schema Registry client
     */
    public static SchemaRegistryClient createClient(SchemaRegistryConfig srConfig, Map<String, String> overrides) {
        String baseUrl = srConfig.getUrl();
        int cacheCapacity = srConfig.getCacheCapacity();

        LOG.debug(
                "Creating Schema Registry client with URL: {}, cache capacity: {} and overrides: {}",
                baseUrl,
                cacheCapacity,
                overrides.keySet());

        Map<String, String> configs = buildRestServiceConfigs(srConfig);
        configs.putAll(overrides);

        RestService restService = new RestService(baseUrl);
        restService.configure(configs);

        return new CachedSchemaRegistryClient(restService, cacheCapacity, null, configs);
    }

    /**
     * Builds REST service configuration map from Schema Registry config.
     *
     * @param srConfig the schema registry configuration
     * @return configuration map for RestService
     */
    private static Map<String, String> buildRestServiceConfigs(SchemaRegistryConfig srConfig) {
        Map<String, String> configs = new HashMap<>();

        // Authentication
        srConfig.getBasicAuthCredentialsSource()
                .ifPresent(value -> configs.put("basic.auth.credentials.source", value));
        srConfig.getBasicAuthUserInfo().ifPresent(value -> configs.put("basic.auth.user.info", value));

        // SSL
        srConfig.getSslTruststoreLocation()
                .ifPresent(value -> configs.put("schema.registry.ssl.truststore.location", value));
        srConfig.getSslTruststorePassword()
                .ifPresent(value -> configs.put("schema.registry.ssl.truststore.password", value));
        srConfig.getSslKeystoreLocation()
                .ifPresent(value -> configs.put("schema.registry.ssl.keystore.location", value));
        srConfig.getSslKeystorePassword()
                .ifPresent(value -> configs.put("schema.registry.ssl.keystore.password", value));

        LOG.trace("Built Schema Registry REST service configs: {}", configs.keySet());
        return configs;
    }
}
