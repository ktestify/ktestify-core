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
package io.github.ktestify.io.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.github.ktestify.config.KafkaConfig;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.config.SchemaRegistryConfig;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating Kafka producers and consumers using ktestify configuration.
 *
 * <p>This factory provides convenient methods to create pre-configured Kafka clients using settings from
 * {@link KtestifyConfig}.
 *
 * <p>Usage:
 *
 * <pre>
 * // Using default configuration
 * Producer&lt;String, String&gt; producer = KafkaClientFactory.createRawProducer();
 * Consumer&lt;String, String&gt; consumer = KafkaClientFactory.createRawConsumer();
 *
 * // Using custom configuration
 * KtestifyConfig config = ConfigBuilder.create()
 *     .bootstrapServers("kafka:9092")
 *     .build();
 * Producer&lt;String, String&gt; producer = KafkaClientFactory.createRawProducer(config);
 * </pre>
 *
 * @since 0.2.51
 */
public final class KafkaClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaClientFactory.class);

    private KafkaClientFactory() {
        // Utility class
    }

    // ==========================================
    // RAW (String) PRODUCERS
    // ==========================================

    /**
     * Creates a raw (String key/value) Kafka producer using default configuration.
     *
     * @return a new Kafka producer
     */
    public static Producer<String, String> createRawProducer() {
        return createRawProducer(KtestifyConfig.getOrLoad());
    }

    /**
     * Creates a raw (String key/value) Kafka producer using the provided configuration.
     *
     * @param config the ktestify configuration
     * @return a new Kafka producer
     */
    public static Producer<String, String> createRawProducer(KtestifyConfig config) {
        Properties props = config.getKafka().getProducerProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        LOG.debug(
                "Creating raw producer with bootstrap servers: {}", props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        return new KafkaProducer<>(props);
    }

    /**
     * Creates a raw producer with additional property overrides.
     *
     * @param overrides additional properties to override
     * @return a new Kafka producer
     */
    public static Producer<String, String> createRawProducer(Map<String, Object> overrides) {
        Properties props = KtestifyConfig.getOrLoad().getKafka().getProducerProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.putAll(overrides);

        LOG.debug("Creating raw producer with overrides: {}", overrides.keySet());
        return new KafkaProducer<>(props);
    }

    // ==========================================
    // AVRO PRODUCERS
    // ==========================================

    /**
     * Creates an Avro Kafka producer using default configuration.
     *
     * @return a new Kafka producer for Avro records
     */
    public static Producer<String, GenericRecord> createAvroProducer() {
        return createAvroProducer(KtestifyConfig.getOrLoad());
    }

    /**
     * Creates an Avro Kafka producer using the provided configuration.
     *
     * @param config the ktestify configuration
     * @return a new Kafka producer for Avro records
     */
    public static Producer<String, GenericRecord> createAvroProducer(KtestifyConfig config) {
        KafkaConfig kafkaConfig = config.getKafka();
        SchemaRegistryConfig srConfig = config.getSchemaRegistry();

        Properties props = kafkaConfig.getProducerProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());

        // Add Schema Registry configuration
        props.putAll(srConfig.getProperties());

        LOG.debug(
                "Creating Avro producer with bootstrap servers: {} and schema registry: {}",
                props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG),
                srConfig.getUrl());
        return new KafkaProducer<>(props);
    }

    /**
     * Creates an Avro producer with additional property overrides.
     *
     * @param overrides additional properties to override
     * @return a new Kafka producer for Avro records
     */
    public static Producer<String, GenericRecord> createAvroProducer(Map<String, Object> overrides) {
        KtestifyConfig config = KtestifyConfig.getOrLoad();
        Properties props = config.getKafka().getProducerProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.putAll(config.getSchemaRegistry().getProperties());
        props.putAll(overrides);

        LOG.debug("Creating Avro producer with overrides: {}", overrides.keySet());
        return new KafkaProducer<>(props);
    }

    // ==========================================
    // RAW (String) CONSUMERS
    // ==========================================

    /**
     * Creates a raw (String key/value) Kafka consumer using default configuration. Uses a unique consumer group ID to
     * ensure independent consumption.
     *
     * @return a new Kafka consumer
     */
    public static Consumer<String, String> createRawConsumer() {
        return createRawConsumer(KtestifyConfig.getOrLoad());
    }

    /**
     * Creates a raw (String key/value) Kafka consumer using the provided configuration. Uses a unique consumer group ID
     * to ensure independent consumption.
     *
     * @param config the ktestify configuration
     * @return a new Kafka consumer
     */
    public static Consumer<String, String> createRawConsumer(KtestifyConfig config) {
        return createRawConsumer(config, generateUniqueGroupId(config.getKafka().getConsumerGroupId()));
    }

    /**
     * Creates a raw consumer with a specific group ID.
     *
     * @param groupId the consumer group ID
     * @return a new Kafka consumer
     */
    public static Consumer<String, String> createRawConsumer(String groupId) {
        return createRawConsumer(KtestifyConfig.getOrLoad(), groupId);
    }

    /**
     * Creates a raw consumer with the provided configuration and group ID.
     *
     * @param config the ktestify configuration
     * @param groupId the consumer group ID
     * @return a new Kafka consumer
     */
    public static Consumer<String, String> createRawConsumer(KtestifyConfig config, String groupId) {
        Properties props = config.getKafka().getConsumerProperties(groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        LOG.debug(
                "Creating raw consumer with bootstrap servers: {} and group ID: {}",
                props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG),
                groupId);
        return new KafkaConsumer<>(props);
    }

    /**
     * Creates a raw consumer with additional property overrides.
     *
     * @param overrides additional properties to override
     * @return a new Kafka consumer
     */
    public static Consumer<String, String> createRawConsumer(Map<String, Object> overrides) {
        KtestifyConfig config = KtestifyConfig.getOrLoad();
        String groupId = generateUniqueGroupId(config.getKafka().getConsumerGroupId());
        Properties props = config.getKafka().getConsumerProperties(groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.putAll(overrides);

        LOG.debug("Creating raw consumer with overrides: {}", overrides.keySet());
        return new KafkaConsumer<>(props);
    }

    // ==========================================
    // AVRO CONSUMERS
    // ==========================================

    /**
     * Creates an Avro Kafka consumer using default configuration. Uses a unique consumer group ID to ensure independent
     * consumption.
     *
     * @return a new Kafka consumer for Avro records
     */
    public static Consumer<String, GenericRecord> createAvroConsumer() {
        return createAvroConsumer(KtestifyConfig.getOrLoad());
    }

    /**
     * Creates an Avro Kafka consumer using the provided configuration. Uses a unique consumer group ID to ensure
     * independent consumption.
     *
     * @param config the ktestify configuration
     * @return a new Kafka consumer for Avro records
     */
    public static Consumer<String, GenericRecord> createAvroConsumer(KtestifyConfig config) {
        return createAvroConsumer(
                config, generateUniqueGroupId(config.getKafka().getConsumerGroupId()));
    }

    /**
     * Creates an Avro consumer with a specific group ID.
     *
     * @param groupId the consumer group ID
     * @return a new Kafka consumer for Avro records
     */
    public static Consumer<String, GenericRecord> createAvroConsumer(String groupId) {
        return createAvroConsumer(KtestifyConfig.getOrLoad(), groupId);
    }

    /**
     * Creates an Avro consumer with the provided configuration and group ID.
     *
     * @param config the ktestify configuration
     * @param groupId the consumer group ID
     * @return a new Kafka consumer for Avro records
     */
    public static Consumer<String, GenericRecord> createAvroConsumer(KtestifyConfig config, String groupId) {
        KafkaConfig kafkaConfig = config.getKafka();
        SchemaRegistryConfig srConfig = config.getSchemaRegistry();

        Properties props = kafkaConfig.getConsumerProperties(groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());

        // Add Schema Registry configuration
        props.putAll(srConfig.getProperties());

        LOG.debug(
                "Creating Avro consumer with bootstrap servers: {}, group ID: {}, schema registry: {}",
                props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG),
                groupId,
                srConfig.getUrl());
        return new KafkaConsumer<>(props);
    }

    /**
     * Creates an Avro consumer with additional property overrides.
     *
     * @param overrides additional properties to override
     * @return a new Kafka consumer for Avro records
     */
    public static Consumer<String, GenericRecord> createAvroConsumer(Map<String, Object> overrides) {
        KtestifyConfig config = KtestifyConfig.getOrLoad();
        String groupId = generateUniqueGroupId(config.getKafka().getConsumerGroupId());
        Properties props = config.getKafka().getConsumerProperties(groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.putAll(config.getSchemaRegistry().getProperties());
        props.putAll(overrides);

        LOG.debug("Creating Avro consumer with overrides: {}", overrides.keySet());
        return new KafkaConsumer<>(props);
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================

    /**
     * Generates a unique consumer group ID by appending a UUID suffix. This ensures each test run uses an independent
     * consumer group.
     *
     * @param baseGroupId the base group ID from configuration
     * @return a unique group ID
     */
    private static String generateUniqueGroupId(String baseGroupId) {
        return baseGroupId + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Gets the Kafka properties from current configuration. Useful for inspecting or debugging configuration.
     *
     * @return the producer properties
     */
    public static Properties getProducerProperties() {
        return KtestifyConfig.getOrLoad().getKafka().getProducerProperties();
    }

    /**
     * Gets the consumer properties from current configuration. Useful for inspecting or debugging configuration.
     *
     * @return the consumer properties
     */
    public static Properties getConsumerProperties() {
        return KtestifyConfig.getOrLoad().getKafka().getConsumerProperties();
    }
}
