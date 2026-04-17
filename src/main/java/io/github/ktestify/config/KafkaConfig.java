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
package io.github.ktestify.config;

import com.typesafe.config.Config;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import lombok.Getter;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;

/**
 * Kafka-specific configuration.
 *
 * <p>Provides access to Kafka producer, consumer, and common settings. Supports both standard Kafka configuration
 * properties and ktestify-specific settings.
 *
 * @since 0.2.51
 */
@Getter
public final class KafkaConfig {

    private final Config config;

    // Common settings
    private final String bootstrapServers;
    private final String securityProtocol;
    private final Optional<String> topicNamespace;

    // Producer defaults
    private final String producerAcks;
    private final int producerRetries;
    private final int producerBatchSize;
    private final Duration producerLingerMs;
    private final int producerBufferMemory;
    private final String producerKeySerializer;
    private final String producerValueSerializer;

    // Consumer defaults
    private final String consumerGroupId;
    private final boolean consumerEnableAutoCommit;
    private final String consumerAutoOffsetReset;
    private final Duration consumerSessionTimeout;
    private final Duration consumerHeartbeatInterval;
    private final int consumerMaxPollRecords;
    private final String consumerKeyDeserializer;
    private final String consumerValueDeserializer;

    // Security settings
    private final Optional<String> saslMechanism;
    private final Optional<String> saslJaasConfig;
    private final Optional<String> sslTruststoreLocation;
    private final Optional<String> sslTruststorePassword;
    private final Optional<String> sslKeystoreLocation;
    private final Optional<String> sslKeystorePassword;
    private final Optional<String> sslKeyPassword;

    KafkaConfig(Config config) {
        this.config = config;

        // Common
        this.bootstrapServers = config.getString("bootstrap-servers");
        this.securityProtocol = config.getString("security-protocol");
        this.topicNamespace = getOptionalString(config, "topic-namespace");

        // Producer
        Config producerConfig = config.getConfig("producer");
        this.producerAcks = producerConfig.getString("acks");
        this.producerRetries = producerConfig.getInt("retries");
        this.producerBatchSize = producerConfig.getInt("batch-size");
        this.producerLingerMs = producerConfig.getDuration("linger-ms");
        this.producerBufferMemory = producerConfig.getInt("buffer-memory");
        this.producerKeySerializer = producerConfig.getString("key-serializer");
        this.producerValueSerializer = producerConfig.getString("value-serializer");

        // Consumer
        Config consumerConfig = config.getConfig("consumer");
        this.consumerGroupId = consumerConfig.getString("group-id");
        this.consumerEnableAutoCommit = consumerConfig.getBoolean("enable-auto-commit");
        this.consumerAutoOffsetReset = consumerConfig.getString("auto-offset-reset");
        this.consumerSessionTimeout = consumerConfig.getDuration("session-timeout");
        this.consumerHeartbeatInterval = consumerConfig.getDuration("heartbeat-interval");
        this.consumerMaxPollRecords = consumerConfig.getInt("max-poll-records");
        this.consumerKeyDeserializer = consumerConfig.getString("key-deserializer");
        this.consumerValueDeserializer = consumerConfig.getString("value-deserializer");

        // Security (optional)
        Config securityConfig = config.getConfig("security");
        this.saslMechanism = getOptionalString(securityConfig, "sasl-mechanism");
        this.saslJaasConfig = getOptionalString(securityConfig, "sasl-jaas-config");
        this.sslTruststoreLocation = getOptionalString(securityConfig, "ssl-truststore-location");
        this.sslTruststorePassword = getOptionalString(securityConfig, "ssl-truststore-password");
        this.sslKeystoreLocation = getOptionalString(securityConfig, "ssl-keystore-location");
        this.sslKeystorePassword = getOptionalString(securityConfig, "ssl-keystore-password");
        this.sslKeyPassword = getOptionalString(securityConfig, "ssl-key-password");
    }

    private Optional<String> getOptionalString(Config config, String path) {
        if (config.hasPath(path) && !config.getIsNull(path)) {
            String value = config.getString(path);
            return value.isEmpty() ? Optional.empty() : Optional.of(value);
        }
        return Optional.empty();
    }

    /**
     * Creates a Properties object with common Kafka settings. This includes bootstrap servers and security
     * configuration.
     *
     * @return Properties with common Kafka settings
     */
    public Properties getCommonProperties() {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);

        // Add security properties if configured
        saslMechanism.ifPresent(v -> props.put(SaslConfigs.SASL_MECHANISM, v));
        saslJaasConfig.ifPresent(v -> props.put(SaslConfigs.SASL_JAAS_CONFIG, v));
        sslTruststoreLocation.ifPresent(v -> props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, v));
        sslTruststorePassword.ifPresent(v -> props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, v));
        sslKeystoreLocation.ifPresent(v -> props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, v));
        sslKeystorePassword.ifPresent(v -> props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, v));
        sslKeyPassword.ifPresent(v -> props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, v));

        return props;
    }

    /**
     * Creates a Properties object with default producer settings. Includes common properties plus producer-specific
     * configuration.
     *
     * @return Properties for Kafka producer
     */
    public Properties getProducerProperties() {
        Properties props = getCommonProperties();
        props.put(ProducerConfig.ACKS_CONFIG, producerAcks);
        props.put(ProducerConfig.RETRIES_CONFIG, producerRetries);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, producerBatchSize);
        props.put(ProducerConfig.LINGER_MS_CONFIG, producerLingerMs.toMillis());
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, producerBufferMemory);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerKeySerializer);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerValueSerializer);
        return props;
    }

    /**
     * Creates a Properties object with default consumer settings. Includes common properties plus consumer-specific
     * configuration.
     *
     * @return Properties for Kafka consumer
     */
    public Properties getConsumerProperties() {
        Properties props = getCommonProperties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerEnableAutoCommit);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerAutoOffsetReset);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, (int) consumerSessionTimeout.toMillis());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, (int) consumerHeartbeatInterval.toMillis());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerMaxPollRecords);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, consumerKeyDeserializer);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, consumerValueDeserializer);
        return props;
    }

    /**
     * Creates a Properties object with consumer settings using a specific group ID.
     *
     * @param groupId the consumer group ID to use
     * @return Properties for Kafka consumer with specified group ID
     */
    public Properties getConsumerProperties(String groupId) {
        Properties props = getConsumerProperties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        return props;
    }

    /**
     * Converts producer properties to a Map for use with builders.
     *
     * @return Map of producer properties
     */
    public Map<String, String> getProducerPropertiesAsMap() {
        Properties props = getProducerProperties();
        Map<String, String> map = new HashMap<>();
        props.forEach((k, v) -> map.put(k.toString(), v.toString()));
        return map;
    }

    /**
     * Converts consumer properties to a Map for use with builders.
     *
     * @return Map of consumer properties
     */
    public Map<String, String> getConsumerPropertiesAsMap() {
        Properties props = getConsumerProperties();
        Map<String, String> map = new HashMap<>();
        props.forEach((k, v) -> map.put(k.toString(), v.toString()));
        return map;
    }

    /**
     * Gets the raw underlying Config object for advanced usage.
     *
     * @return the raw Config object
     */
    public Config getRaw() {
        return config;
    }
}
