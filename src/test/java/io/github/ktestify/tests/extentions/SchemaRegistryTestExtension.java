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
package io.github.ktestify.tests.extentions;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

/**
 * JUnit 5 extension that starts a Confluent Schema Registry container via Testcontainers for the duration of the test
 * class, and exposes static helpers used by the tests.
 *
 * <p>The container is started once per test class (BeforeAll / AfterAll) and is shared across every test method, which
 * keeps suite execution fast while still providing full isolation through unique topic names per test.
 *
 * <p>The Schema Registry container is connected to the same Docker network as the Kafka container (managed by
 * {@link KafkaTestExtension}) to enable inter-container communication.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @ExtendWith({KafkaTestExtension.class, SchemaRegistryTestExtension.class})
 * class MyTest { … }
 * }</pre>
 *
 * <p><strong>Important:</strong> When using this extension, you must also use {@link KafkaTestExtension} and ensure it
 * is listed first in the @ExtendWith annotation so that the Kafka container starts before Schema Registry.
 */
@Slf4j
public class SchemaRegistryTestExtension
        implements BeforeAllCallback, AfterAllCallback, ExtensionContext.Store.CloseableResource {

    // -----------------------------------------------------------------
    // Testcontainers image – pin to a specific Confluent Platform version
    // so that builds are reproducible. Bump when you need a newer version.
    // -----------------------------------------------------------------
    private static final DockerImageName SCHEMA_REGISTRY_IMAGE =
            DockerImageName.parse("confluentinc/cp-schema-registry:7.9.0");

    /** The internal port on which Schema Registry listens inside the container. */
    private static final int SCHEMA_REGISTRY_PORT = 8081;

    /** Network alias used by the Kafka container for inter-container communication. */
    private static final String KAFKA_NETWORK_ALIAS = "kafka";

    /**
     * Shared container instance. Lazily initialised in {@link #beforeAll} and torn down in {@link #afterAll} (or via
     * the store's CloseableResource).
     */
    private static volatile GenericContainer<?> schemaRegistryContainer;

    /** Cached Schema Registry client for interacting with the registry. */
    private static volatile SchemaRegistryClient schemaRegistryClient;

    // ----------------------------------------------------------------
    // JUnit 5 lifecycle
    // ----------------------------------------------------------------

    @Override
    public void beforeAll(ExtensionContext context) {
        if (schemaRegistryContainer == null || !schemaRegistryContainer.isRunning()) {
            // Get the Kafka container and its network from KafkaTestExtension
            var kafkaContainer = KafkaTestExtension.getKafkaContainer();
            if (kafkaContainer == null || !kafkaContainer.isRunning()) {
                throw new IllegalStateException(
                        "KafkaTestExtension must be initialized before SchemaRegistryTestExtension. "
                                + "Make sure KafkaTestExtension.class appears first in @ExtendWith annotation.");
            }

            // Get the network from the Kafka container
            Network network = kafkaContainer.getNetwork();

            // Configure the Schema Registry container to connect to Kafka using the internal network
            // Note: KafkaContainer exposes internal broker on port 9093 for inter-container communication
            schemaRegistryContainer = new GenericContainer<>(SCHEMA_REGISTRY_IMAGE)
                    .withNetwork(network)
                    .withExposedPorts(SCHEMA_REGISTRY_PORT)
                    .withEnv(Map.of(
                            // Use the Kafka network alias with internal port 9093
                            "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                            "PLAINTEXT://" + KAFKA_NETWORK_ALIAS + ":9093",
                            "SCHEMA_REGISTRY_HOST_NAME",
                            "schema-registry",
                            "SCHEMA_REGISTRY_LISTENERS",
                            "http://0.0.0.0:" + SCHEMA_REGISTRY_PORT,
                            // Additional debugging
                            "SCHEMA_REGISTRY_DEBUG",
                            "true"))
                    .withLogConsumer(
                            outputFrame -> log.trace(outputFrame.getUtf8String().trim()))
                    .withReuse(false);

            schemaRegistryContainer.start();

            log.info("[SchemaRegistryTestExtension] Schema Registry started at: " + getSchemaRegistryUrl());

            // Initialize the Schema Registry client
            schemaRegistryClient = new CachedSchemaRegistryClient(
                    getSchemaRegistryUrl(), 100 // Cache capacity
                    );

            // Register with the store so the container is stopped automatically
            // when the root context closes (i.e. at the end of the test suite).
            context.getRoot()
                    .getStore(ExtensionContext.Namespace.GLOBAL)
                    .put(SchemaRegistryTestExtension.class.getName(), this);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // Intentionally left empty: the container is stopped via close()
        // registered in the global store, which fires once the entire suite
        // has finished rather than after each test class.
    }

    /** Called by JUnit when the global store is torn down (end of the JVM run). */
    @Override
    public void close() {
        if (schemaRegistryClient != null) {
            try {
                // Schema Registry client doesn't require explicit closing in most cases
                schemaRegistryClient = null;
            } catch (Exception e) {
                log.error("[SchemaRegistryTestExtension] Error closing Schema Registry client: " + e.getMessage());
            }
        }
        if (schemaRegistryContainer != null && schemaRegistryContainer.isRunning()) {
            schemaRegistryContainer.stop();
        }
    }

    // ----------------------------------------------------------------
    // Static helpers used by the tests
    // ----------------------------------------------------------------

    /**
     * Returns the Schema Registry URL that clients can use to connect from the host machine.
     *
     * @return the Schema Registry URL, e.g. {@code "http://localhost:49184"}
     * @throws IllegalStateException if the container has not been started yet.
     */
    public static String getSchemaRegistryUrl() {
        assertRunning();
        return "http://" + schemaRegistryContainer.getHost() + ":"
                + schemaRegistryContainer.getMappedPort(SCHEMA_REGISTRY_PORT);
    }

    /**
     * Returns the Schema Registry client instance for interacting with the registry.
     *
     * @return the SchemaRegistryClient instance
     * @throws IllegalStateException if the container has not been started yet.
     */
    public static SchemaRegistryClient getSchemaRegistryClient() {
        assertRunning();
        if (schemaRegistryClient == null) {
            throw new IllegalStateException("Schema Registry client has not been initialized. "
                    + "This should not happen - please check the extension setup.");
        }
        return schemaRegistryClient;
    }

    /**
     * Returns the Schema Registry container instance for advanced usage.
     *
     * @return the running GenericContainer instance
     * @throws IllegalStateException if the container has not been started yet.
     */
    public static GenericContainer<?> getSchemaRegistryContainer() {
        assertRunning();
        return schemaRegistryContainer;
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private static void assertRunning() {
        if (schemaRegistryContainer == null || !schemaRegistryContainer.isRunning()) {
            throw new IllegalStateException(
                    "SchemaRegistryTestExtension: the Schema Registry container is not running. "
                            + "Make sure the test class is annotated with "
                            + "@ExtendWith({KafkaTestExtension.class, SchemaRegistryTestExtension.class}).");
        }
    }
}
