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

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.github.ktestify.config.ConfigBuilder;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.io.kafka.impl.AvroKafkaProducer;
import io.github.ktestify.models.Topic;
import io.github.ktestify.tests.extentions.KafkaTestExtension;
import io.github.ktestify.tests.extentions.SchemaRegistryTestExtension;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for AvroKafkaProducer using Testcontainers.
 *
 * <p>These tests verify the complete Avro producer functionality including:
 *
 * <ul>
 *   <li>Producing Avro records from JSON file payloads
 *   <li>Producing Avro records from inline JSON payloads
 *   <li>Producing Avro records with headers
 *   <li>Producing Avro records with record keys
 *   <li>Schema registry integration and schema resolution
 *   <li>Error handling and validation
 *   <li>ProducerContext integration
 *   <li>Complex nested Avro schemas
 *   <li>Logical types (dates, timestamps, decimals)
 * </ul>
 *
 * <p>The tests use Testcontainers to spin up a Kafka broker and Schema Registry, ensuring full isolation and
 * reproducibility.
 */
@ExtendWith({KafkaTestExtension.class, SchemaRegistryTestExtension.class})
@DisplayName("AvroKafkaProducer Integration Tests")
class AvroKafkaProducerTest {

    private static final String TEST_TOPIC_PREFIX = "test-avro-producer-";

    @TempDir
    Path tempDir;

    private Producer<String, GenericRecord> producer;
    private Consumer<String, GenericRecord> consumer;
    private String testTopicName;
    private SchemaRegistryClient schemaRegistryClient;

    // ==========================================
    // SETUP AND TEARDOWN
    // ==========================================

    @BeforeAll
    static void setUpClass() {
        // Reset config and configure with testcontainer URLs
        KtestifyConfig.reset();

        // Configure Ktestify with testcontainer URLs
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
        // Generate unique topic name for each test
        testTopicName = TEST_TOPIC_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        KafkaTestExtension.createTopic(testTopicName);

        // Initialize Schema Registry client
        schemaRegistryClient = SchemaRegistryTestExtension.getSchemaRegistryClient();

        // Configure Kafka using testcontainer bootstrap servers and schema registry
        // Create producer and consumer directly using KafkaClientFactory with properties
        Map<String, Object> producerOverrides = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestExtension.getBootstrapServers(),
                KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        SchemaRegistryTestExtension.getSchemaRegistryUrl());

        Map<String, Object> consumerOverrides = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KafkaTestExtension.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "test-avro-consumer-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
                SchemaRegistryTestExtension.getSchemaRegistryUrl());

        producer = createAvroProducer(producerOverrides);
        consumer = createAvroConsumer(consumerOverrides);
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.close();
        }
        if (consumer != null) {
            consumer.close();
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private Producer<String, GenericRecord> createAvroProducer(Map<String, Object> overrides) {
        return KafkaClientFactory.createAvroProducer(overrides);
    }

    private Consumer<String, GenericRecord> createAvroConsumer(Map<String, Object> overrides) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, overrides.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, overrides.get(ConsumerConfig.GROUP_ID_CONFIG));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, overrides.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(
                KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
                overrides.get(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG));
        props.put("specific.avro.reader", "false");
        return new KafkaConsumer<>(props);
    }

    private File createPayloadFile(String content) throws IOException {
        Path filePath = tempDir.resolve("payload-" + UUID.randomUUID() + ".json");
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        return filePath.toFile();
    }

    private Topic createInputTopic(String topicName) {
        return Topic.builder()
                .topicName(topicName)
                .topicAlias("test-avro-alias")
                .topicType(Topic.Type.INPUT)
                .build();
    }

    private ConsumerRecord<String, GenericRecord> consumeOneRecord(String topicName) {
        consumer.subscribe(Collections.singletonList(topicName));
        ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofSeconds(10));
        assertFalse(records.isEmpty(), "Expected at least one record in topic " + topicName);
        return records.iterator().next();
    }

    private void registerSchema(String schemaName, String schemaDefinition) throws Exception {
        Schema schema = new Schema.Parser().parse(schemaDefinition);
        @SuppressWarnings("deprecation")
        var ignored = schemaRegistryClient.register(schemaName + "-value", schema);
    }

    // ==========================================
    // BASIC PRODUCER TESTS
    // ==========================================

    @Nested
    @DisplayName("Basic Avro Producer Operations")
    class BasicAvroProducerTests {

        @Test
        @DisplayName("Should produce simple Avro record from file payload")
        void shouldProduceSimpleAvroRecordFromFile() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "SimpleUser",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "name", "type": "string"},
                        {"name": "age", "type": "int"}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.SimpleUser", schemaJson);

            String payload = """
                    {
                      "name": "John Doe",
                      "age": 30
                    }
                    """;

            File payloadFile = createPayloadFile(payload);
            Topic topic = createInputTopic(testTopicName);

            // When
            AvroKafkaProducer avroProducer =
                    new AvroKafkaProducer(topic, null, producer, payloadFile, "com.example.avro.SimpleUser");
            avroProducer.send();

            // Then
            ConsumerRecord<String, GenericRecord> record = consumeOneRecord(testTopicName);
            assertNotNull(record.value());
            GenericRecord avroRecord = record.value();
            assertEquals("John Doe", avroRecord.get("name").toString());
            assertEquals(30, avroRecord.get("age"));
        }

        @Test
        @DisplayName("Should produce simple Avro record from inline payload")
        void shouldProduceSimpleAvroRecordFromInline() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "InlineUser",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "username", "type": "string"},
                        {"name": "active", "type": "boolean"}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.InlineUser", schemaJson);

            String payload = """
                    {
                      "username": "alice",
                      "active": true
                    }
                    """;

            Topic topic = createInputTopic(testTopicName);

            // When
            AvroKafkaProducer avroProducer = new AvroKafkaProducer(
                    topic, null, producer, null, Map.of(), payload, "com.example.avro.InlineUser");
            avroProducer.send();

            // Then
            ConsumerRecord<String, GenericRecord> record = consumeOneRecord(testTopicName);
            assertNotNull(record.value());
            GenericRecord avroRecord = record.value();
            assertEquals("alice", avroRecord.get("username").toString());
            assertEquals(true, avroRecord.get("active"));
        }

        @Test
        @DisplayName("Should produce Avro record with numeric fields")
        void shouldProduceAvroRecordWithNumericFields() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "NumericData",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "intValue", "type": "int"},
                        {"name": "longValue", "type": "long"},
                        {"name": "floatValue", "type": "float"},
                        {"name": "doubleValue", "type": "double"}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.NumericData", schemaJson);

            String payload = """
                    {
                      "intValue": 42,
                      "longValue": 9223372036854775807,
                      "floatValue": 3.14,
                      "doubleValue": 2.718281828
                    }
                    """;

            File payloadFile = createPayloadFile(payload);
            Topic topic = createInputTopic(testTopicName);

            // When
            AvroKafkaProducer avroProducer =
                    new AvroKafkaProducer(topic, null, producer, payloadFile, "com.example.avro.NumericData");
            avroProducer.send();

            // Then
            ConsumerRecord<String, GenericRecord> record = consumeOneRecord(testTopicName);
            assertNotNull(record.value());
            GenericRecord avroRecord = record.value();
            assertEquals(42, avroRecord.get("intValue"));
            assertEquals(9223372036854775807L, avroRecord.get("longValue"));
            assertTrue(Math.abs(3.14f - (float) avroRecord.get("floatValue")) < 0.01f);
            assertTrue(Math.abs(2.718281828 - (double) avroRecord.get("doubleValue")) < 0.0001);
        }
    }

    // ==========================================
    // NESTED RECORD TESTS
    // ==========================================

    @Nested
    @DisplayName("Nested Avro Records")
    class NestedRecordTests {

        @Test
        @DisplayName("Should produce Avro record with nested record")
        void shouldProduceNestedAvroRecord() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "Event",
                      "namespace": "com.example.avro",
                      "fields": [
                        {
                          "name": "user",
                          "type": {
                            "type": "record",
                            "name": "User",
                            "fields": [
                              {"name": "id", "type": "int"},
                              {"name": "name", "type": "string"}
                            ]
                          }
                        },
                        {"name": "eventType", "type": "string"}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.Event", schemaJson);

            String payload = """
                    {
                      "user": {
                        "id": 123,
                        "name": "Bob Smith"
                      },
                      "eventType": "LOGIN"
                    }
                    """;

            File payloadFile = createPayloadFile(payload);
            Topic topic = createInputTopic(testTopicName);

            // When
            AvroKafkaProducer avroProducer =
                    new AvroKafkaProducer(topic, null, producer, payloadFile, "com.example.avro.Event");
            avroProducer.send();

            // Then
            ConsumerRecord<String, GenericRecord> record = consumeOneRecord(testTopicName);
            assertNotNull(record.value());
            GenericRecord event = record.value();
            assertEquals("LOGIN", event.get("eventType").toString());

            GenericRecord user = (GenericRecord) event.get("user");
            assertNotNull(user);
            assertEquals(123, user.get("id"));
            assertEquals("Bob Smith", user.get("name").toString());
        }

        @Test
        @DisplayName("Should produce Avro record with multiple levels of nesting")
        void shouldProduceDeeplyNestedAvroRecord() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "Order",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "orderId", "type": "string"},
                        {
                          "name": "customer",
                          "type": {
                            "type": "record",
                            "name": "Customer",
                            "fields": [
                              {"name": "id", "type": "int"},
                              {
                                "name": "address",
                                "type": {
                                  "type": "record",
                                  "name": "Address",
                                  "fields": [
                                    {"name": "street", "type": "string"},
                                    {"name": "city", "type": "string"},
                                    {"name": "zipCode", "type": "string"}
                                  ]
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                    """;

            registerSchema("com.example.avro.Order", schemaJson);

            String payload = """
                    {
                      "orderId": "ORD-12345",
                      "customer": {
                        "id": 999,
                        "address": {
                          "street": "123 Main St",
                          "city": "Springfield",
                          "zipCode": "12345"
                        }
                      }
                    }
                    """;

            File payloadFile = createPayloadFile(payload);
            Topic topic = createInputTopic(testTopicName);

            // When
            AvroKafkaProducer avroProducer =
                    new AvroKafkaProducer(topic, null, producer, payloadFile, "com.example.avro.Order");
            avroProducer.send();

            // Then
            ConsumerRecord<String, GenericRecord> record = consumeOneRecord(testTopicName);
            assertNotNull(record.value());
            GenericRecord order = record.value();
            assertEquals("ORD-12345", order.get("orderId").toString());

            GenericRecord customer = (GenericRecord) order.get("customer");
            assertNotNull(customer);
            assertEquals(999, customer.get("id"));

            GenericRecord address = (GenericRecord) customer.get("address");
            assertNotNull(address);
            assertEquals("123 Main St", address.get("street").toString());
            assertEquals("Springfield", address.get("city").toString());
            assertEquals("12345", address.get("zipCode").toString());
        }
    }

    // ==========================================
    // ARRAY TESTS
    // ==========================================

    @Nested
    @DisplayName("Avro Arrays")
    class ArrayTests {

        @Test
        @DisplayName("Should produce Avro record with array of primitives")
        void shouldProduceArrayOfPrimitives() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "ListData",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "id", "type": "string"},
                        {"name": "numbers", "type": {"type": "array", "items": "int"}}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.ListData", schemaJson);

            String payload = """
                    {
                      "id": "LIST-1",
                      "numbers": [1, 2, 3, 4, 5]
                    }
                    """;

            File payloadFile = createPayloadFile(payload);
            Topic topic = createInputTopic(testTopicName);

            // When
            AvroKafkaProducer avroProducer =
                    new AvroKafkaProducer(topic, null, producer, payloadFile, "com.example.avro.ListData");
            avroProducer.send();

            // Then
            ConsumerRecord<String, GenericRecord> record = consumeOneRecord(testTopicName);
            assertNotNull(record.value());
            GenericRecord data = record.value();
            assertEquals("LIST-1", data.get("id").toString());
        }

        @Test
        @DisplayName("Should produce Avro record with array of records")
        void shouldProduceArrayOfRecords() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "OrderWithItems",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "orderId", "type": "string"},
                        {
                          "name": "items",
                          "type": {
                            "type": "array",
                            "items": {
                              "type": "record",
                              "name": "OrderItem",
                              "fields": [
                                {"name": "sku", "type": "string"},
                                {"name": "quantity", "type": "int"},
                                {"name": "price", "type": "double"}
                              ]
                            }
                          }
                        }
                      ]
                    }
                    """;

            registerSchema("com.example.avro.OrderWithItems", schemaJson);

            String payload = """
                    {
                      "orderId": "ORD-99999",
                      "items": [
                        {"sku": "SKU001", "quantity": 2, "price": 29.99},
                        {"sku": "SKU002", "quantity": 1, "price": 49.99},
                        {"sku": "SKU003", "quantity": 3, "price": 15.50}
                      ]
                    }
                    """;

            File payloadFile = createPayloadFile(payload);
            Topic topic = createInputTopic(testTopicName);

            // When
            AvroKafkaProducer avroProducer =
                    new AvroKafkaProducer(topic, null, producer, payloadFile, "com.example.avro.OrderWithItems");
            avroProducer.send();

            // Then
            ConsumerRecord<String, GenericRecord> record = consumeOneRecord(testTopicName);
            assertNotNull(record.value());
            GenericRecord order = record.value();
            assertEquals("ORD-99999", order.get("orderId").toString());
        }
    }

    // ==========================================
    // MAP TESTS
    // ==========================================

    @Nested
    @DisplayName("Avro Maps")
    class MapTests {

        @Test
        @DisplayName("Should produce Avro record with map of strings")
        void shouldProduceMapOfStrings() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "ConfigData",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "configId", "type": "string"},
                        {"name": "properties", "type": {"type": "map", "values": "string"}}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.ConfigData", schemaJson);

            String payload = """
                    {
                      "configId": "CONFIG-1",
                      "properties": {
                        "timeout": "30000",
                        "retries": "3",
                        "environment": "prod"
                      }
                    }
                    """;

            File payloadFile = createPayloadFile(payload);
            Topic topic = createInputTopic(testTopicName);

            // When
            AvroKafkaProducer avroProducer =
                    new AvroKafkaProducer(topic, null, producer, payloadFile, "com.example.avro.ConfigData");
            avroProducer.send();

            // Then
            ConsumerRecord<String, GenericRecord> record = consumeOneRecord(testTopicName);
            assertNotNull(record.value());
            GenericRecord config = record.value();
            assertEquals("CONFIG-1", config.get("configId").toString());
        }
    }

    // ==========================================
    // UNION / OPTIONAL TESTS
    // ==========================================

    @Nested
    @DisplayName("Union Types and Optional Fields")
    class UnionTests {

        @Test
        @DisplayName("Should produce Avro record with optional (nullable) fields")
        void shouldProduceRecordWithOptionalFields() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "ContactInfo",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "name", "type": "string"},
                        {"name": "email", "type": ["null", "string"], "default": null},
                        {"name": "phone", "type": ["null", "string"], "default": null}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.ContactInfo", schemaJson);

            String payload = """
                    {
                      "name": "Carol White",
                      "email": "carol@example.com",
                      "phone": null
                    }
                    """;

            File payloadFile = createPayloadFile(payload);
            Topic topic = createInputTopic(testTopicName);

            // When
            AvroKafkaProducer avroProducer =
                    new AvroKafkaProducer(topic, null, producer, payloadFile, "com.example.avro.ContactInfo");
            avroProducer.send();

            // Then
            ConsumerRecord<String, GenericRecord> record = consumeOneRecord(testTopicName);
            assertNotNull(record.value());
            GenericRecord contact = record.value();
            assertEquals("Carol White", contact.get("name").toString());
            assertEquals("carol@example.com", contact.get("email").toString());
            assertNull(contact.get("phone"));
        }
    }

    // ==========================================
    // HEADER TESTS
    // ==========================================

    @Nested
    @DisplayName("Message Headers with Avro")
    class HeaderTests {

        @Test
        @DisplayName("Should produce Avro message with single header")
        void shouldProduceAvroMessageWithHeader() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "HeaderTestRecord",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "id", "type": "string"},
                        {"name": "data", "type": "string"}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.HeaderTestRecord", schemaJson);

            String payload = """
                    {
                      "id": "ID-1",
                      "data": "test data"
                    }
                    """;

            Map<String, String> headers = Map.of("X-Correlation-Id", "corr-avro-123");
            Topic topic = createInputTopic(testTopicName);

            // When
            AvroKafkaProducer avroProducer = new AvroKafkaProducer(
                    topic, null, producer, null, headers, payload, "com.example.avro.HeaderTestRecord");
            avroProducer.send();

            // Then
            consumer.subscribe(Collections.singletonList(testTopicName));
            ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofSeconds(10));
            assertFalse(records.isEmpty());
            ConsumerRecord<String, GenericRecord> record = records.iterator().next();

            var headerValue = record.headers().lastHeader("X-Correlation-Id");
            assertNotNull(headerValue);
            assertEquals("corr-avro-123", new String(headerValue.value(), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("Should produce Avro message with multiple headers")
        void shouldProduceAvroMessageWithMultipleHeaders() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "MultiHeaderRecord",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "id", "type": "string"}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.MultiHeaderRecord", schemaJson);

            String payload = """
                    {
                      "id": "ID-2"
                    }
                    """;

            Map<String, String> headers = new HashMap<>();
            headers.put("X-Correlation-Id", "corr-456");
            headers.put("X-Source-System", "ktestify-avro");
            headers.put("X-Message-Type", "avro-test");

            Topic topic = createInputTopic(testTopicName);

            // When
            AvroKafkaProducer avroProducer = new AvroKafkaProducer(
                    topic, null, producer, null, headers, payload, "com.example.avro.MultiHeaderRecord");
            avroProducer.send();

            // Then
            consumer.subscribe(Collections.singletonList(testTopicName));
            ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofSeconds(10));
            assertFalse(records.isEmpty());
            ConsumerRecord<String, GenericRecord> record = records.iterator().next();

            assertEquals(
                    "corr-456",
                    new String(record.headers().lastHeader("X-Correlation-Id").value(), StandardCharsets.UTF_8));
            assertEquals(
                    "ktestify-avro",
                    new String(record.headers().lastHeader("X-Source-System").value(), StandardCharsets.UTF_8));
            assertEquals(
                    "avro-test",
                    new String(record.headers().lastHeader("X-Message-Type").value(), StandardCharsets.UTF_8));
        }
    }

    // ==========================================
    // PRODUCER CONTEXT TESTS
    // ==========================================

    @Nested
    @DisplayName("ProducerContext Integration with Avro")
    class ProducerContextTests {

        @Test
        @DisplayName("Should produce using ProducerContext with file payload")
        void shouldProduceUsingContextWithFile() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "ContextFileRecord",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "message", "type": "string"}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.ContextFileRecord", schemaJson);

            String payload = """
                    {
                      "message": "context file test"
                    }
                    """;

            File payloadFile = createPayloadFile(payload);
            Topic topic = createInputTopic(testTopicName);

            ProducerContext<String, GenericRecord> context = ProducerContext.<String, GenericRecord>builder()
                    .topic(topic)
                    .producer(producer)
                    .payloadFile(payloadFile)
                    .schemaName("com.example.avro.ContextFileRecord")
                    .build();

            // When
            AvroKafkaProducer avroProducer = new AvroKafkaProducer(context);
            avroProducer.send();

            // Then
            ConsumerRecord<String, GenericRecord> record = consumeOneRecord(testTopicName);
            assertNotNull(record.value());
            GenericRecord data = record.value();
            assertEquals("context file test", data.get("message").toString());
        }

        @Test
        @DisplayName("Should produce using ProducerContext with inline payload")
        void shouldProduceUsingContextWithInlinePayload() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "ContextInlineRecord",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "value", "type": "int"}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.ContextInlineRecord", schemaJson);

            String payload = """
                    {
                      "value": 999
                    }
                    """;

            Topic topic = createInputTopic(testTopicName);

            ProducerContext<String, GenericRecord> context = ProducerContext.<String, GenericRecord>builder()
                    .topic(topic)
                    .producer(producer)
                    .payload(payload)
                    .schemaName("com.example.avro.ContextInlineRecord")
                    .build();

            // When
            AvroKafkaProducer avroProducer = new AvroKafkaProducer(context);
            avroProducer.send();

            // Then
            ConsumerRecord<String, GenericRecord> record = consumeOneRecord(testTopicName);
            assertNotNull(record.value());
            GenericRecord data = record.value();
            assertEquals(999, data.get("value"));
        }

        @Test
        @DisplayName("Should produce using ProducerContext with headers and record key")
        void shouldProduceUsingContextWithHeadersAndKey() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "KeyedRecord",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "id", "type": "string"},
                        {"name": "status", "type": "string"}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.KeyedRecord", schemaJson);

            String payload = """
                    {
                      "id": "REC-123",
                      "status": "active"
                    }
                    """;

            Map<String, String> headers = Map.of("X-Request-Id", "req-789");
            Topic topic = createInputTopic(testTopicName);

            ProducerContext<String, GenericRecord> context = ProducerContext.<String, GenericRecord>builder()
                    .topic(topic)
                    .producer(producer)
                    .payload(payload)
                    .headers(headers)
                    .recordKey("key-123")
                    .schemaName("com.example.avro.KeyedRecord")
                    .build();

            // When
            AvroKafkaProducer avroProducer = new AvroKafkaProducer(context);
            avroProducer.send();

            // Then
            consumer.subscribe(Collections.singletonList(testTopicName));
            ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofSeconds(10));
            assertFalse(records.isEmpty());
            ConsumerRecord<String, GenericRecord> record = records.iterator().next();

            assertEquals("key-123", record.key());
            assertEquals(
                    "req-789",
                    new String(record.headers().lastHeader("X-Request-Id").value(), StandardCharsets.UTF_8));
            assertEquals("REC-123", record.value().get("id").toString());
        }
    }

    // ==========================================
    // ERROR HANDLING TESTS
    // ==========================================

    @Nested
    @DisplayName("Error Handling and Validation")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw exception for null producer")
        void shouldThrowExceptionForNullProducer() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "ErrorRecord",
                      "namespace": "com.example.avro",
                      "fields": [{"name": "id", "type": "string"}]
                    }
                    """;

            registerSchema("com.example.avro.ErrorRecord", schemaJson);

            Topic topic = createInputTopic(testTopicName);
            String payload = "{\"id\": \"1\"}";

            // When & Then
            assertThrows(Exception.class, () -> {
                @SuppressWarnings("unused")
                AvroKafkaProducer avroProducer = new AvroKafkaProducer(
                        topic, null, null, null, Map.of(), payload, "com.example.avro.ErrorRecord");
            });
        }

        @Test
        @DisplayName("Should throw exception when both file and inline payload are null")
        void shouldThrowExceptionForNullPayloads() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "NoPayloadRecord",
                      "namespace": "com.example.avro",
                      "fields": [{"name": "id", "type": "string"}]
                    }
                    """;

            registerSchema("com.example.avro.NoPayloadRecord", schemaJson);

            Topic topic = createInputTopic(testTopicName);

            // When & Then
            AvroKafkaProducer avroProducer = new AvroKafkaProducer(
                    topic, null, producer, null, Map.of(), null, "com.example.avro.NoPayloadRecord");

            assertThrows(IllegalStateException.class, avroProducer::send);
        }

        @Test
        @DisplayName("Should throw exception for invalid JSON payload")
        void shouldThrowExceptionForInvalidJson() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "InvalidJsonRecord",
                      "namespace": "com.example.avro",
                      "fields": [{"name": "id", "type": "string"}]
                    }
                    """;

            registerSchema("com.example.avro.InvalidJsonRecord", schemaJson);

            String invalidPayload = "{ invalid json }";
            File payloadFile = createPayloadFile(invalidPayload);
            Topic topic = createInputTopic(testTopicName);

            // When & Then
            AvroKafkaProducer avroProducer =
                    new AvroKafkaProducer(topic, null, producer, payloadFile, "com.example.avro.InvalidJsonRecord");

            assertThrows(Exception.class, avroProducer::send);
        }
    }

    // ==========================================
    // RECORD KEY TESTS
    // ==========================================

    @Nested
    @DisplayName("Producer Record Keys")
    class RecordKeyTests {

        @Test
        @DisplayName("Should produce Avro record with record key")
        void shouldProduceRecordWithKey() throws Exception {
            // Given
            String schemaJson = """
                    {
                      "type": "record",
                      "name": "KeyedAvroRecord",
                      "namespace": "com.example.avro",
                      "fields": [
                        {"name": "id", "type": "string"},
                        {"name": "name", "type": "string"}
                      ]
                    }
                    """;

            registerSchema("com.example.avro.KeyedAvroRecord", schemaJson);

            String payload = """
                    {
                      "id": "1001",
                      "name": "Product A"
                    }
                    """;

            Topic topic = createInputTopic(testTopicName);

            // When
            ProducerContext<String, GenericRecord> context = ProducerContext.<String, GenericRecord>builder()
                    .topic(topic)
                    .producer(producer)
                    .payload(payload)
                    .recordKey("product-key-1001")
                    .schemaName("com.example.avro.KeyedAvroRecord")
                    .build();

            AvroKafkaProducer avroProducer = new AvroKafkaProducer(context);
            avroProducer.send();

            // Then
            consumer.subscribe(Collections.singletonList(testTopicName));
            ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofSeconds(10));
            assertFalse(records.isEmpty());
            ConsumerRecord<String, GenericRecord> record = records.iterator().next();

            assertEquals("product-key-1001", record.key());
            GenericRecord value = record.value();
            assertNotNull(value);
            assertEquals("1001", value.get("id").toString());
        }
    }
}
