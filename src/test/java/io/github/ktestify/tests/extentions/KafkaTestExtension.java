/*
 * Copyright 2026 Nil MALHOMME (malhomme.nil+oss@icloud.com)
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
package io.github.ktestify.tests.extentions;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * JUnit 5 extension that starts a Kafka broker via Testcontainers for the duration of the test class, and exposes
 * static helpers used by the tests.
 *
 * <p>The container is started once per test class (BeforeAll / AfterAll) and is shared across every test method, which
 * keeps suite execution fast while still providing full isolation through unique topic names per test.
 *
 * <p>The Kafka container is created on a Docker network that other containers (like Schema Registry) can join for
 * inter-container communication.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @ExtendWith(KafkaTestExtension.class)
 * class MyTest { … }
 * }</pre>
 */
public class KafkaTestExtension
        implements BeforeAllCallback, AfterAllCallback, ExtensionContext.Store.CloseableResource {

    // -----------------------------------------------------------------
    // Testcontainers image – pin to a specific Confluent Platform version
    // so that builds are reproducible. Bump when you need a newer Kafka.
    // -----------------------------------------------------------------
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:4.2.0");

    /** Network alias used for inter-container communication (e.g., with Schema Registry). */
    private static final String KAFKA_NETWORK_ALIAS = "kafka";

    /**
     * Shared container instance. Lazily initialised in {@link #beforeAll} and torn down in {@link #afterAll} (or via
     * the store's CloseableResource).
     */
    private static volatile KafkaContainer kafkaContainer;

    /** Shared Docker network for inter-container communication. */
    private static volatile Network network;

    // ----------------------------------------------------------------
    // JUnit 5 lifecycle
    // ----------------------------------------------------------------

    @Override
    public void beforeAll(ExtensionContext context) {
        if (kafkaContainer == null || !kafkaContainer.isRunning()) {
            // Create a shared network for inter-container communication
            network = Network.newNetwork();

            kafkaContainer = new KafkaContainer(KAFKA_IMAGE)
                    .withNetwork(network)
                    .withNetworkAliases(KAFKA_NETWORK_ALIAS)
                    // Reuse across JVM runs during local development when
                    // TESTCONTAINERS_REUSE_ENABLE=true is set in ~/.testcontainers.properties
                    .withReuse(false);
            kafkaContainer.start();

            System.out.println("[KafkaTestExtension] Kafka started with bootstrap servers: "
                    + kafkaContainer.getBootstrapServers());
            System.out.println("[KafkaTestExtension] Kafka network alias: " + KAFKA_NETWORK_ALIAS);

            // Register with the store so the container is stopped automatically
            // when the root context closes (i.e. at the end of the test suite).
            context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL).put(KafkaTestExtension.class.getName(), this);
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
        if (kafkaContainer != null && kafkaContainer.isRunning()) {
            kafkaContainer.stop();
        }
        if (network != null) {
            try {
                network.close();
            } catch (Exception e) {
                System.err.println("[KafkaTestExtension] Error closing network: " + e.getMessage());
            }
        }
    }

    // ----------------------------------------------------------------
    // Static helpers used by the tests
    // ----------------------------------------------------------------

    /**
     * Returns the Kafka bootstrap-servers string for the running container, e.g. {@code "localhost:49183"}.
     *
     * @throws IllegalStateException if the container has not been started yet.
     */
    public static String getBootstrapServers() {
        assertRunning();
        return kafkaContainer.getBootstrapServers();
    }

    /**
     * Returns the Kafka container instance for inter-container networking. Useful for Schema Registry and other
     * services that need to connect to Kafka via Docker's internal network.
     *
     * @return the running KafkaContainer instance
     * @throws IllegalStateException if the container has not been started yet.
     */
    public static KafkaContainer getKafkaContainer() {
        assertRunning();
        return kafkaContainer;
    }

    /**
     * Creates a topic with a single partition and a replication factor of 1 (appropriate for a single-broker test
     * container).
     *
     * @param topicName the name of the topic to create.
     * @throws RuntimeException if topic creation fails or times out.
     */
    public static void createTopic(String topicName) {
        createTopic(topicName, 1, (short) 1);
    }

    /**
     * Creates a topic with the supplied partition count and replication factor.
     *
     * @param topicName topic name.
     * @param numPartitions number of partitions.
     * @param replicationFactor replication factor (must be ≤ broker count).
     */
    public static void createTopic(String topicName, int numPartitions, short replicationFactor) {
        assertRunning();
        try (AdminClient admin = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers()))) {

            NewTopic newTopic = new NewTopic(topicName, numPartitions, replicationFactor);
            admin.createTopics(Collections.singletonList(newTopic)).all().get(30, TimeUnit.SECONDS);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Kafka topic: " + topicName, e);
        }
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private static void assertRunning() {
        if (kafkaContainer == null || !kafkaContainer.isRunning()) {
            throw new IllegalStateException("KafkaTestExtension: the Kafka container is not running. "
                    + "Make sure the test class is annotated with @ExtendWith(KafkaTestExtension.class).");
        }
    }
}
