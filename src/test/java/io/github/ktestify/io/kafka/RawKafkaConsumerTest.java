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
import io.github.ktestify.constants.ConfigConstants;
import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.io.kafka.impl.RawKafkaConsumer;
import io.github.ktestify.models.Topic;
import io.github.ktestify.tests.extentions.KafkaTestExtension;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration tests for {@link RawKafkaConsumer}.
 *
 * <p>Kafka broker is provided by {@link KafkaTestExtension}. Records are seeded via a plain {@link KafkaProducer} — NOT
 * the project's RawKafkaProducer — keeping the consumer under test fully isolated from the producer implementation.
 *
 * <p>Every test gets a fresh, uniquely-named topic and {@link KafkaRecordFetcher#clearMatchedRecords()} is called
 * before each test to prevent deduplication state leaking between tests.
 */
@ExtendWith(KafkaTestExtension.class)
@DisplayName("RawKafkaConsumer Integration Tests")
class RawKafkaConsumerTest {

    private static final String TOPIC_PREFIX = "test-raw-consumer-";

    // Content of src/test/resources/match/expected-order.json
    private static final String ORDER_JSON =
            "{\"orderId\":\"ORD-001\",\"customerId\":\"CUST-42\",\"amount\":99.99,\"status\":\"CREATED\"}";

    // Content of src/test/resources/match/expected-order.xml (without leading newline)
    private static final String ORDER_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <order>
                <orderId>ORD-001</orderId>
                <customerId>CUST-42</customerId>
                <amount>99.99</amount>
                <status>CREATED</status>
            </order>""";

    private KafkaProducer<String, String> seedProducer;
    private String topicName;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeAll
    static void setUpClass() {
        KtestifyConfig.reset();
        ConfigBuilder.create()
                .bootstrapServers(KafkaTestExtension.getBootstrapServers())
                .build();
    }

    @AfterAll
    static void tearDownClass() {
        KtestifyConfig.reset();
    }

    @BeforeEach
    void setUp() {
        topicName = TOPIC_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        KafkaTestExtension.createTopic(topicName);
        seedProducer = createSeedProducer();
        // Always clear the deduplication registry so previous tests don't interfere
        KafkaRecordFetcher.clearMatchedRecords();
    }

    @AfterEach
    void tearDown() {
        if (seedProducer != null) seedProducer.close();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private KafkaProducer<String, String> createSeedProducer() {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestExtension.getBootstrapServers());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaProducer<>(p);
    }

    /** Produces a record synchronously and flushes so it is visible before polling. */
    private void seedRecord(String key, String value) throws Exception {
        seedProducer.send(new ProducerRecord<>(topicName, key, value)).get();
        seedProducer.flush();
    }

    /** Builds an OUTPUT topic pointing at {@code topicName}. */
    private Topic outputTopic() {
        return Topic.builder()
                .topicName(topicName)
                .topicAlias("consumer-test")
                .topicType(Topic.Type.OUTPUT)
                .build();
    }

    /** Absolute path of a classpath resource under {@code match/}. */
    private static String resourcePath(String filename) {
        URL url = RawKafkaConsumerTest.class.getClassLoader().getResource("match/" + filename);
        assertNotNull(url, "Test resource not found: match/" + filename);
        return url.getPath();
    }

    /** Builds a {@link ConsumerContext} with a fresh consumer pointing at {@code topicName}. */
    private ConsumerContext<String, String> ctx() {
        return ConsumerContext.<String, String>builder()
                .topic(outputTopic())
                .consumer(KafkaClientFactory.createRawConsumer(
                        KtestifyConfig.getOrLoad(), "raw-consumer-test-" + UUID.randomUUID()))
                .readTimeout(10_000L)
                .consumerDeltaTime(60_000L) // look back 60 s so seeded records are visible
                .build();
    }

    // =========================================================================
    // Consume-only (NoOp matcher)
    // =========================================================================

    @Nested
    @DisplayName("Consume-only — no matcher")
    class ConsumeOnly {

        @Test
        @DisplayName("returns true when a record is present in the topic")
        void returnsTrueWhenRecordPresent() throws Exception {
            seedRecord(null, ORDER_JSON);

            boolean result = new RawKafkaConsumer(ctx()).call();
            assertTrue(result);
        }

        @Test
        @DisplayName("throws ConsumerException when topic is empty and timeout elapses")
        void throwsWhenTopicEmpty() {
            // Do NOT seed — consumer must time out
            ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                    .topic(outputTopic())
                    .consumer(KafkaClientFactory.createRawConsumer(
                            KtestifyConfig.getOrLoad(), "raw-timeout-" + UUID.randomUUID()))
                    .readTimeout(2_000L) // short timeout so the test finishes fast
                    .consumerDeltaTime(5_000L)
                    .build();

            assertThrows(ConsumerException.class, () -> new RawKafkaConsumer(ctx).call());
        }
    }

    // =========================================================================
    // File matching
    // =========================================================================

    @Nested
    @DisplayName("File matching — METHOD_MATCH_FILE")
    class FileMatching {

        @Test
        @DisplayName("returns true when record value matches expected file")
        void matchesFile() throws Exception {
            // The file content has a trailing newline; trim so the raw value sent
            // over Kafka equals what FileUtils reads from the file.
            String fileContent = new String(
                    RawKafkaConsumerTest.class
                            .getClassLoader()
                            .getResourceAsStream("match/expected-order.json")
                            .readAllBytes(),
                    StandardCharsets.UTF_8);
            seedRecord(null, fileContent);

            boolean result = new RawKafkaConsumer(ConsumerContext.<String, String>builder()
                            .topic(outputTopic())
                            .consumer(KafkaClientFactory.createRawConsumer(
                                    KtestifyConfig.getOrLoad(), "raw-file-match-" + UUID.randomUUID()))
                            .readTimeout(10_000L)
                            .consumerDeltaTime(60_000L)
                            .matchMethod(ConfigConstants.METHOD_MATCH_FILE)
                            .matchFilePath(resourcePath("expected-order.json"))
                            .build())
                    .call();

            assertTrue(result);
        }

        @Test
        @DisplayName("returns false when record value differs from expected file")
        void doesNotMatchFile() throws Exception {
            seedRecord(null, "{\"orderId\":\"WRONG\"}");

            boolean result = new RawKafkaConsumer(ConsumerContext.<String, String>builder()
                            .topic(outputTopic())
                            .consumer(KafkaClientFactory.createRawConsumer(
                                    KtestifyConfig.getOrLoad(), "raw-file-no-match-" + UUID.randomUUID()))
                            .readTimeout(10_000L)
                            .consumerDeltaTime(60_000L)
                            .matchMethod(ConfigConstants.METHOD_MATCH_FILE)
                            .matchFilePath(resourcePath("expected-order.json"))
                            .build())
                    .call();

            assertFalse(result);
        }
    }

    // =========================================================================
    // Key filter
    // =========================================================================

    @Nested
    @DisplayName("Key filter — expectedRecordKey")
    class KeyFilter {

        @Test
        @DisplayName("picks the record with the matching key and ignores others")
        void picksRecordWithMatchingKey() throws Exception {
            seedRecord("WRONG-KEY", "{\"orderId\":\"X\"}");
            seedRecord("ORDER-1", ORDER_JSON);

            boolean result = new RawKafkaConsumer(ConsumerContext.<String, String>builder()
                            .topic(outputTopic())
                            .consumer(KafkaClientFactory.createRawConsumer(
                                    KtestifyConfig.getOrLoad(), "raw-key-filter-" + UUID.randomUUID()))
                            .readTimeout(10_000L)
                            .consumerDeltaTime(60_000L)
                            .expectedRecordKey("ORDER-1")
                            .build())
                    .call();

            assertTrue(result);
        }
    }

    // =========================================================================
    // XML matching
    // =========================================================================

    @Nested
    @DisplayName("XML matching — METHOD_MATCH_XML")
    class XmlMatching {

        @Test
        @DisplayName("returns true when record value matches expected XML file")
        void matchesXml() throws Exception {
            seedRecord(null, ORDER_XML);

            boolean result = new RawKafkaConsumer(ConsumerContext.<String, String>builder()
                            .topic(outputTopic())
                            .consumer(KafkaClientFactory.createRawConsumer(
                                    KtestifyConfig.getOrLoad(), "raw-xml-match-" + UUID.randomUUID()))
                            .readTimeout(10_000L)
                            .consumerDeltaTime(60_000L)
                            .matchMethod(ConfigConstants.METHOD_MATCH_XML)
                            .matchFilePath(resourcePath("expected-order.xml"))
                            .build())
                    .call();

            assertTrue(result);
        }
    }

    // =========================================================================
    // Batch consumption
    // =========================================================================

    @Nested
    @DisplayName("Batch consumption — isBatchConsumer(true)")
    class BatchConsumption {

        /** The 4 payloads seeded in every test in this nested class. */
        private static final List<String> BATCH_PAYLOADS = List.of(
                "{\"orderId\":\"ORD-001\",\"customerId\":\"CUST-42\",\"amount\":99.99,\"status\":\"CREATED\"}",
                "{\"orderId\":\"ORD-002\",\"customerId\":\"CUST-43\",\"amount\":49.99,\"status\":\"PENDING\"}",
                "{\"orderId\":\"ORD-003\",\"customerId\":\"CUST-44\",\"amount\":19.99,\"status\":\"SHIPPED\"}",
                "{\"orderId\":\"ORD-004\",\"customerId\":\"CUST-45\",\"amount\":9.99,\"status\":\"DELIVERED\"}");

        /** Seeds all 4 records synchronously so they are available before the consumer starts. */
        private void seedBatch() throws Exception {
            for (int i = 0; i < BATCH_PAYLOADS.size(); i++) {
                seedRecord("KEY-" + (i + 1), BATCH_PAYLOADS.get(i));
            }
        }

        @Test
        @DisplayName("collects all 4 records and returns true (no-matcher)")
        void collectsAllFourRecords() throws Exception {
            seedBatch();

            boolean result = new RawKafkaConsumer(ConsumerContext.<String, String>builder()
                            .topic(outputTopic())
                            .consumer(KafkaClientFactory.createRawConsumer(
                                    KtestifyConfig.getOrLoad(), "raw-batch-noop-" + UUID.randomUUID()))
                            .readTimeout(15_000L)
                            .consumerDeltaTime(60_000L)
                            .isBatchConsumer(true)
                            .batchSize(4)
                            .build())
                    .call();

            assertTrue(result);
        }

        @Test
        @DisplayName("throws ConsumerException when fewer records than batchSize are available")
        void throwsWhenNotEnoughRecords() throws Exception {
            // Seed only 2 out of 4 required
            seedRecord("KEY-1", BATCH_PAYLOADS.get(0));
            seedRecord("KEY-2", BATCH_PAYLOADS.get(1));

            assertThrows(
                    ConsumerException.class,
                    () -> new RawKafkaConsumer(ConsumerContext.<String, String>builder()
                                    .topic(outputTopic())
                                    .consumer(KafkaClientFactory.createRawConsumer(
                                            KtestifyConfig.getOrLoad(), "raw-batch-short-" + UUID.randomUUID()))
                                    .readTimeout(3_000L) // short timeout — we expect a failure
                                    .consumerDeltaTime(60_000L)
                                    .isBatchConsumer(true)
                                    .batchSize(4)
                                    .build())
                            .call());
        }

        @Test
        @DisplayName("all 4 records are registered as matched — a second consumer finds nothing")
        void batchRecordsAreDeduplicated() throws Exception {
            seedBatch();

            // First batch consumer claims all 4 records
            new RawKafkaConsumer(ConsumerContext.<String, String>builder()
                            .topic(outputTopic())
                            .consumer(KafkaClientFactory.createRawConsumer(
                                    KtestifyConfig.getOrLoad(), "raw-batch-dedup-first-" + UUID.randomUUID()))
                            .readTimeout(15_000L)
                            .consumerDeltaTime(60_000L)
                            .isBatchConsumer(true)
                            .batchSize(4)
                            .build())
                    .call();

            // Second consumer — all records already matched, so it must time out
            assertThrows(
                    ConsumerException.class,
                    () -> new RawKafkaConsumer(ConsumerContext.<String, String>builder()
                                    .topic(outputTopic())
                                    .consumer(KafkaClientFactory.createRawConsumer(
                                            KtestifyConfig.getOrLoad(), "raw-batch-dedup-second-" + UUID.randomUUID()))
                                    .readTimeout(3_000L)
                                    .consumerDeltaTime(60_000L)
                                    .isBatchConsumer(true)
                                    .batchSize(4)
                                    .build())
                            .call());
        }
    }
}
