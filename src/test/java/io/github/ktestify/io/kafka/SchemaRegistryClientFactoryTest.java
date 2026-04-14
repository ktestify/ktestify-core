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

import static org.junit.jupiter.api.Assertions.*;

import io.github.ktestify.config.ConfigBuilder;
import io.github.ktestify.config.KtestifyConfig;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for SchemaRegistryClientFactory. */
class SchemaRegistryClientFactoryTest {

    @Test
    void testCreateClientWithDefaultConfig() {
        // Given: Default configuration
        KtestifyConfig config = KtestifyConfig.getOrLoad();

        // When: Creating a client
        SchemaRegistryClient client = SchemaRegistryClientFactory.createClient(config);

        // Then: Client should be created successfully
        assertNotNull(client, "Schema Registry client should not be null");
    }

    @Test
    void testCreateClientWithCustomConfig() {
        // Given: Custom configuration
        KtestifyConfig config = ConfigBuilder.create()
                .bootstrapServers("localhost:9092")
                .schemaRegistryUrl("http://localhost:8081")
                .build();

        // When: Creating a client
        SchemaRegistryClient client = SchemaRegistryClientFactory.createClient(config);

        // Then: Client should be created successfully
        assertNotNull(client, "Schema Registry client should not be null");
    }

    @Test
    void testCreateClientWithMultipleUrls() {
        // Given: Multiple Schema Registry URLs for HA setup
        var urls = Arrays.asList("http://schema-registry-1:8081", "http://schema-registry-2:8081");

        // When: Creating a client with multiple URLs
        SchemaRegistryClient client = SchemaRegistryClientFactory.createClient(urls, 1000);

        // Then: Client should be created successfully
        assertNotNull(client, "Schema Registry client should not be null");
    }

    @Test
    void testCreateClientWithOverrides() {
        // Given: Custom configuration and overrides
        KtestifyConfig config = ConfigBuilder.create()
                .bootstrapServers("localhost:9092")
                .schemaRegistryUrl("http://localhost:8081")
                .build();

        Map<String, String> overrides = new HashMap<>();
        overrides.put("basic.auth.credentials.source", "USER_INFO");
        overrides.put("basic.auth.user.info", "user:password");

        // When: Creating a client with overrides
        SchemaRegistryClient client = SchemaRegistryClientFactory.createClient(config.getSchemaRegistry(), overrides);

        // Then: Client should be created successfully
        assertNotNull(client, "Schema Registry client should not be null");
    }

    @Test
    void testCreateClientUsingDefaultStaticMethod() {
        // When: Creating a client using the default static method
        SchemaRegistryClient client = SchemaRegistryClientFactory.createClient();

        // Then: Client should be created successfully
        assertNotNull(client, "Schema Registry client should not be null");
    }
}
