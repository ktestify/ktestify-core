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

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.github.ktestify.config.ConfigBuilder;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.constants.ConfigConstants;
import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.io.kafka.impl.AvroKafkaConsumer;
import io.github.ktestify.models.Topic;
import io.github.ktestify.tests.extentions.KafkaTestExtension;
import io.github.ktestify.tests.extentions.SchemaRegistryTestExtension;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
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
 * Integration tests for {@link AvroKafkaConsumer}.
 *
 * <p>Infrastructure:
 *
 * <ul>
 *   <li>{@link KafkaTestExtension} — Kafka broker via Testcontainers
 *   <li>{@link SchemaRegistryTestExtension} — Confluent Schema Registry via Testcontainers
 * </ul>
 *
 * <p>Records are seeded via a plain {@link KafkaProducer} with {@link KafkaAvroSerializer} — NOT the project's
 * {@code AvroKafkaProducer} — keeping the consumer under test fully isolated from the producer implementation.
 *
 * <p>{@link KafkaRecordFetcher#clearMatchedRecords()} is called before every test to prevent deduplication state
 * leaking between tests.
 */
@ExtendWith({KafkaTestExtension.class, SchemaRegistryTestExtension.class})
@DisplayName("AvroKafkaConsumer Integration Tests")
class AvroKafkaConsumerTest {

    private static final String TOPIC_PREFIX = "test-avro-consumer-";

    // -------------------------------------------------------------------------
    // Shared Avro schema — same shape as expected-order.json
    // -------------------------------------------------------------------------
    static final Schema ORDER_SCHEMA = SchemaBuilder.record("Order")
            .namespace("io.github.ktestify.test")
            .fields()
            .requiredString("orderId")
            .requiredString("customerId")
            .requiredDouble("amount")
            .requiredString("status")
            .endRecord();

    private KafkaProducer<String, GenericRecord> seedProducer;
    private String topicName;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeAll
    static void setUpClass() {
        KtestifyConfig.reset();
        ConfigBuilder.create()
                .bootstrapServers(KafkaTestExtension.getBootstrapServers())
                .schemaRegistryUrl(SchemaRegistryTestExtension.getSchemaRegistryUrl())
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
        KafkaRecordFetcher.clearMatchedRecords();
    }

    @AfterEach
    void tearDown() {
        if (seedProducer != null) seedProducer.close();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private KafkaProducer<String, GenericRecord> createSeedProducer() {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestExtension.getBootstrapServers());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        p.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SchemaRegistryTestExtension.getSchemaRegistryUrl());
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaProducer<>(p);
    }

    /** Produces a GenericRecord synchronously and flushes. */
    private void seedRecord(String key, GenericRecord value) throws Exception {
        seedProducer.send(new ProducerRecord<>(topicName, key, value)).get();
        seedProducer.flush();
    }

    /** Builds a matching GenericRecord (orderId=ORD-001, customerId=CUST-42, …). */
    private GenericRecord matchingOrder() {
        GenericRecord r = new GenericData.Record(ORDER_SCHEMA);
        r.put("orderId", "ORD-001");
        r.put("customerId", "CUST-42");
        r.put("amount", 99.99);
        r.put("status", "CREATED");
        return r;
    }

    /** Builds a GenericRecord with different field values. */
    private GenericRecord differentOrder(String orderId, String status) {
        GenericRecord r = new GenericData.Record(ORDER_SCHEMA);
        r.put("orderId", orderId);
        r.put("customerId", "CUST-99");
        r.put("amount", 1.00);
        r.put("status", status);
        return r;
    }

    /** Absolute path of a classpath resource under {@code match/}. */
    private static String resourcePath(String filename) {
        URL url = AvroKafkaConsumerTest.class.getClassLoader().getResource("match/" + filename);
        assertNotNull(url, "Test resource not found: match/" + filename);
        return url.getPath();
    }

    /** Builds a {@link ConsumerContext} for {@code topicName} with a fresh Avro consumer. */
    private ConsumerContext<String, GenericRecord> ctx() {
        return ConsumerContext.<String, GenericRecord>builder()
                .topic(outputTopic())
                .consumer(createAvroKafkaConsumer())
                .readTimeout(10_000L)
                .consumerDeltaTime(60_000L)
                .build();
    }

    private Topic outputTopic() {
        return Topic.builder()
                .topicName(topicName)
                .topicAlias("avro-consumer-test")
                .topicType(Topic.Type.OUTPUT)
                .build();
    }

    /** Creates a plain Kafka consumer configured for Avro deserialization. */
    private org.apache.kafka.clients.consumer.Consumer<String, GenericRecord> createAvroKafkaConsumer() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestExtension.getBootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "avro-consumer-test-" + UUID.randomUUID());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        p.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SchemaRegistryTestExtension.getSchemaRegistryUrl());
        p.put("specific.avro.reader", "false");
        return new KafkaConsumer<>(p);
    }

    // =========================================================================
    // Consume-only (NoOp matcher)
    // =========================================================================

    @Nested
    @DisplayName("Consume-only — no matcher")
    class ConsumeOnly {

        @Test
        @DisplayName("returns true when an Avro record is present in the topic")
        void returnsTrueWhenRecordPresent() throws Exception {
            seedRecord(null, matchingOrder());

            boolean result = new AvroKafkaConsumer(ctx()).call();
            assertTrue(result);
        }

        @Test
        @DisplayName("throws ConsumerException when topic is empty and timeout elapses")
        void throwsWhenTopicEmpty() {
            ConsumerContext<String, GenericRecord> ctx = ConsumerContext.<String, GenericRecord>builder()
                    .topic(outputTopic())
                    .consumer(createAvroKafkaConsumer())
                    .readTimeout(2_000L)
                    .consumerDeltaTime(5_000L)
                    .build();

            assertThrows(ConsumerException.class, () -> new AvroKafkaConsumer(ctx).call());
        }
    }

    // =========================================================================
    // File matching — AvroFileRecordMatcher
    // =========================================================================

    @Nested
    @DisplayName("File matching — METHOD_MATCH_FILE")
    class FileMatching {

        @Test
        @DisplayName("returns true when Avro record matches expected JSON file")
        void matchesFile() throws Exception {
            seedRecord(null, matchingOrder());

            boolean result = new AvroKafkaConsumer(ConsumerContext.<String, GenericRecord>builder()
                            .topic(outputTopic())
                            .consumer(createAvroKafkaConsumer())
                            .readTimeout(10_000L)
                            .consumerDeltaTime(60_000L)
                            .matchMethod(ConfigConstants.METHOD_MATCH_FILE)
                            .matchFilePath(resourcePath("expected-order.json"))
                            .build())
                    .call();

            assertTrue(result);
        }

        @Test
        @DisplayName("returns false when Avro record differs from expected JSON file")
        void doesNotMatchFile() throws Exception {
            seedRecord(null, differentOrder("ORD-999", "FAILED"));

            boolean result = new AvroKafkaConsumer(ConsumerContext.<String, GenericRecord>builder()
                            .topic(outputTopic())
                            .consumer(createAvroKafkaConsumer())
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
            seedRecord("WRONG-KEY", differentOrder("ORD-999", "FAILED"));
            seedRecord("ORDER-1", matchingOrder());

            boolean result = new AvroKafkaConsumer(ConsumerContext.<String, GenericRecord>builder()
                            .topic(outputTopic())
                            .consumer(createAvroKafkaConsumer())
                            .readTimeout(10_000L)
                            .consumerDeltaTime(60_000L)
                            .expectedRecordKey("ORDER-1")
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

        /** 4 orders with distinct keys and field values. */
        private List<GenericRecord> buildBatch() {
            return List.of(
                    buildOrder("ORD-001", "CUST-42", 99.99, "CREATED"),
                    buildOrder("ORD-002", "CUST-43", 49.99, "PENDING"),
                    buildOrder("ORD-003", "CUST-44", 19.99, "SHIPPED"),
                    buildOrder("ORD-004", "CUST-45", 9.99, "DELIVERED"));
        }

        private GenericRecord buildOrder(String orderId, String customerId, double amount, String status) {
            GenericRecord r = new GenericData.Record(ORDER_SCHEMA);
            r.put("orderId", orderId);
            r.put("customerId", customerId);
            r.put("amount", amount);
            r.put("status", status);
            return r;
        }

        /** Seeds all 4 records synchronously. */
        private void seedBatch() throws Exception {
            List<GenericRecord> batch = buildBatch();
            for (int i = 0; i < batch.size(); i++) {
                seedRecord("KEY-" + (i + 1), batch.get(i));
            }
        }

        @Test
        @DisplayName("collects all 4 Avro records and returns true (no-matcher)")
        void collectsAllFourRecords() throws Exception {
            seedBatch();

            boolean result = new AvroKafkaConsumer(ConsumerContext.<String, GenericRecord>builder()
                            .topic(outputTopic())
                            .consumer(createAvroKafkaConsumer())
                            .readTimeout(15_000L)
                            .consumerDeltaTime(60_000L)
                            .isBatchConsumer(true)
                            .batchSize(4)
                            .build())
                    .call();

            assertTrue(result);
        }

        @Test
        @DisplayName("throws ConsumerException when fewer Avro records than batchSize are available")
        void throwsWhenNotEnoughRecords() throws Exception {
            // Seed only 2 out of 4 required
            List<GenericRecord> batch = buildBatch();
            seedRecord("KEY-1", batch.get(0));
            seedRecord("KEY-2", batch.get(1));

            assertThrows(
                    ConsumerException.class,
                    () -> new AvroKafkaConsumer(ConsumerContext.<String, GenericRecord>builder()
                                    .topic(outputTopic())
                                    .consumer(createAvroKafkaConsumer())
                                    .readTimeout(3_000L)
                                    .consumerDeltaTime(60_000L)
                                    .isBatchConsumer(true)
                                    .batchSize(4)
                                    .build())
                            .call());
        }

        @Test
        @DisplayName("all 4 Avro records are registered as matched — a second consumer finds nothing")
        void batchRecordsAreDeduplicated() throws Exception {
            seedBatch();

            // First batch consumer claims all 4
            new AvroKafkaConsumer(ConsumerContext.<String, GenericRecord>builder()
                            .topic(outputTopic())
                            .consumer(createAvroKafkaConsumer())
                            .readTimeout(15_000L)
                            .consumerDeltaTime(60_000L)
                            .isBatchConsumer(true)
                            .batchSize(4)
                            .build())
                    .call();

            // Second consumer — all records already matched, must time out
            assertThrows(
                    ConsumerException.class,
                    () -> new AvroKafkaConsumer(ConsumerContext.<String, GenericRecord>builder()
                                    .topic(outputTopic())
                                    .consumer(createAvroKafkaConsumer())
                                    .readTimeout(3_000L)
                                    .consumerDeltaTime(60_000L)
                                    .isBatchConsumer(true)
                                    .batchSize(4)
                                    .build())
                            .call());
        }
    }
}
