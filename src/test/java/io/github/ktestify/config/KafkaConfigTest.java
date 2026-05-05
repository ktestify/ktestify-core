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
package io.github.ktestify.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive unit tests for KafkaConfig. Tests all getters, property conversions, and edge cases. */
@DisplayName("KafkaConfig Tests")
class KafkaConfigTest {

    private KafkaConfig kafkaConfig;

    @BeforeEach
    void setUp() {
        KtestifyConfig.reset();
        KtestifyConfig config = KtestifyConfig.load();
        kafkaConfig = config.getKafka();
    }

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    // ==========================================
    // COMMON PROPERTIES TESTS
    // ==========================================

    @Nested
    @DisplayName("Common Properties")
    class CommonPropertiesTests {

        @Test
        @DisplayName("Should get bootstrap servers")
        void shouldGetBootstrapServers() {
            assertEquals("localhost:9092", kafkaConfig.getBootstrapServers());
        }

        @Test
        @DisplayName("Should get security protocol")
        void shouldGetSecurityProtocol() {
            assertEquals("PLAINTEXT", kafkaConfig.getSecurityProtocol());
        }

        @Test
        @DisplayName("Should return common properties")
        void shouldReturnCommonProperties() {
            Properties props = kafkaConfig.getCommonProperties();

            assertNotNull(props);
            assertEquals("localhost:9092", props.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG));
            assertEquals("PLAINTEXT", props.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        }

        @Test
        @DisplayName("Should handle custom bootstrap servers")
        void shouldHandleCustomBootstrapServers() {
            KtestifyConfig config = ConfigBuilder.create()
                    .bootstrapServers("kafka1:9092,kafka2:9092,kafka3:9092")
                    .build();

            assertEquals(
                    "kafka1:9092,kafka2:9092,kafka3:9092", config.getKafka().getBootstrapServers());
        }

        @Test
        @DisplayName("Should handle different security protocols")
        void shouldHandleDifferentSecurityProtocols() {
            String[] protocols = {"PLAINTEXT", "SSL", "SASL_PLAINTEXT", "SASL_SSL"};

            for (String protocol : protocols) {
                KtestifyConfig config =
                        ConfigBuilder.create().securityProtocol(protocol).build();
                assertEquals(protocol, config.getKafka().getSecurityProtocol());
            }
        }
    }

    // ==========================================
    // PRODUCER PROPERTIES TESTS
    // ==========================================

    @Nested
    @DisplayName("Producer Properties")
    class ProducerPropertiesTests {

        @Test
        @DisplayName("Should get producer acks")
        void shouldGetProducerAcks() {
            assertEquals("all", kafkaConfig.getProducerAcks());
        }

        @Test
        @DisplayName("Should get producer retries")
        void shouldGetProducerRetries() {
            assertEquals(3, kafkaConfig.getProducerRetries());
        }

        @Test
        @DisplayName("Should get producer batch size")
        void shouldGetProducerBatchSize() {
            assertEquals(16384, kafkaConfig.getProducerBatchSize());
        }

        @Test
        @DisplayName("Should get producer linger ms")
        void shouldGetProducerLingerMs() {
            assertEquals(Duration.ofMillis(1), kafkaConfig.getProducerLingerMs());
        }

        @Test
        @DisplayName("Should get producer buffer memory")
        void shouldGetProducerBufferMemory() {
            assertEquals(33554432, kafkaConfig.getProducerBufferMemory());
        }

        @Test
        @DisplayName("Should get producer key serializer")
        void shouldGetProducerKeySerializer() {
            assertEquals(
                    "org.apache.kafka.common.serialization.StringSerializer", kafkaConfig.getProducerKeySerializer());
        }

        @Test
        @DisplayName("Should get producer value serializer")
        void shouldGetProducerValueSerializer() {
            assertEquals(
                    "org.apache.kafka.common.serialization.StringSerializer", kafkaConfig.getProducerValueSerializer());
        }

        @Test
        @DisplayName("Should return complete producer properties")
        void shouldReturnCompleteProducerProperties() {
            Properties props = kafkaConfig.getProducerProperties();

            assertNotNull(props);
            assertEquals("localhost:9092", props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
            assertEquals("all", props.get(ProducerConfig.ACKS_CONFIG));
            assertEquals(3, props.get(ProducerConfig.RETRIES_CONFIG));
            assertEquals(16384, props.get(ProducerConfig.BATCH_SIZE_CONFIG));
            assertEquals(1L, props.get(ProducerConfig.LINGER_MS_CONFIG));
            assertEquals(33554432, props.get(ProducerConfig.BUFFER_MEMORY_CONFIG));
            assertEquals(
                    "org.apache.kafka.common.serialization.StringSerializer",
                    props.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
            assertEquals(
                    "org.apache.kafka.common.serialization.StringSerializer",
                    props.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
        }

        @Test
        @DisplayName("Should convert producer properties to map")
        void shouldConvertProducerPropertiesToMap() {
            Map<String, String> map = kafkaConfig.getProducerPropertiesAsMap();

            assertNotNull(map);
            assertFalse(map.isEmpty());
            assertEquals("localhost:9092", map.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
            assertEquals("all", map.get(ProducerConfig.ACKS_CONFIG));
        }

        @Test
        @DisplayName("Should handle custom producer acks values")
        void shouldHandleCustomProducerAcksValues() {
            String[] acksValues = {"0", "1", "all"};

            for (String acks : acksValues) {
                KtestifyConfig config =
                        ConfigBuilder.create().producerAcks(acks).build();
                assertEquals(acks, config.getKafka().getProducerAcks());
            }
        }

        @Test
        @DisplayName("Should handle custom producer retries")
        void shouldHandleCustomProducerRetries() {
            KtestifyConfig config = ConfigBuilder.create().producerRetries(10).build();

            assertEquals(10, config.getKafka().getProducerRetries());
        }
    }

    // ==========================================
    // CONSUMER PROPERTIES TESTS
    // ==========================================

    @Nested
    @DisplayName("Consumer Properties")
    class ConsumerPropertiesTests {

        @Test
        @DisplayName("Should get consumer group ID")
        void shouldGetConsumerGroupId() {
            assertEquals("ktestify-consumer-group", kafkaConfig.getConsumerGroupId());
        }

        @Test
        @DisplayName("Should get consumer enable auto commit")
        void shouldGetConsumerEnableAutoCommit() {
            assertFalse(kafkaConfig.isConsumerEnableAutoCommit());
        }

        @Test
        @DisplayName("Should get consumer auto offset reset")
        void shouldGetConsumerAutoOffsetReset() {
            assertEquals("earliest", kafkaConfig.getConsumerAutoOffsetReset());
        }

        @Test
        @DisplayName("Should get consumer session timeout")
        void shouldGetConsumerSessionTimeout() {
            assertEquals(Duration.ofSeconds(30), kafkaConfig.getConsumerSessionTimeout());
        }

        @Test
        @DisplayName("Should get consumer heartbeat interval")
        void shouldGetConsumerHeartbeatInterval() {
            assertEquals(Duration.ofSeconds(10), kafkaConfig.getConsumerHeartbeatInterval());
        }

        @Test
        @DisplayName("Should get consumer max poll records")
        void shouldGetConsumerMaxPollRecords() {
            assertEquals(500, kafkaConfig.getConsumerMaxPollRecords());
        }

        @Test
        @DisplayName("Should get consumer key deserializer")
        void shouldGetConsumerKeyDeserializer() {
            assertEquals(
                    "org.apache.kafka.common.serialization.StringDeserializer",
                    kafkaConfig.getConsumerKeyDeserializer());
        }

        @Test
        @DisplayName("Should get consumer value deserializer")
        void shouldGetConsumerValueDeserializer() {
            assertEquals(
                    "org.apache.kafka.common.serialization.StringDeserializer",
                    kafkaConfig.getConsumerValueDeserializer());
        }

        @Test
        @DisplayName("Should return complete consumer properties")
        void shouldReturnCompleteConsumerProperties() {
            Properties props = kafkaConfig.getConsumerProperties();

            assertNotNull(props);
            assertEquals("localhost:9092", props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
            assertEquals("ktestify-consumer-group", props.get(ConsumerConfig.GROUP_ID_CONFIG));
            assertEquals(false, props.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
            assertEquals("earliest", props.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
            assertEquals(30000, props.get(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG));
            assertEquals(10000, props.get(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG));
            assertEquals(500, props.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG));
        }

        @Test
        @DisplayName("Should return consumer properties with custom group ID")
        void shouldReturnConsumerPropertiesWithCustomGroupId() {
            Properties props = kafkaConfig.getConsumerProperties("custom-group-id");

            assertNotNull(props);
            assertEquals("custom-group-id", props.get(ConsumerConfig.GROUP_ID_CONFIG));
            assertEquals("earliest", props.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        }

        @Test
        @DisplayName("Should convert consumer properties to map")
        void shouldConvertConsumerPropertiesToMap() {
            Map<String, String> map = kafkaConfig.getConsumerPropertiesAsMap();

            assertNotNull(map);
            assertFalse(map.isEmpty());
            assertEquals("localhost:9092", map.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
            assertEquals("ktestify-consumer-group", map.get(ConsumerConfig.GROUP_ID_CONFIG));
        }

        @Test
        @DisplayName("Should handle different auto offset reset values")
        void shouldHandleDifferentAutoOffsetResetValues() {
            String[] resetValues = {"earliest", "latest", "none"};

            for (String reset : resetValues) {
                KtestifyConfig config =
                        ConfigBuilder.create().autoOffsetReset(reset).build();
                assertEquals(reset, config.getKafka().getConsumerAutoOffsetReset());
            }
        }

        @Test
        @DisplayName("Should handle custom consumer group ID")
        void shouldHandleCustomConsumerGroupId() {
            KtestifyConfig config =
                    ConfigBuilder.create().consumerGroupId("my-custom-group").build();

            assertEquals("my-custom-group", config.getKafka().getConsumerGroupId());
        }
    }

    // ==========================================
    // SECURITY PROPERTIES TESTS
    // ==========================================

    @Nested
    @DisplayName("Security Properties")
    class SecurityPropertiesTests {

        @Test
        @DisplayName("Should return empty optional for SASL mechanism by default")
        void shouldReturnEmptyOptionalForSaslMechanismByDefault() {
            assertFalse(kafkaConfig.getSaslMechanism().isPresent());
        }

        @Test
        @DisplayName("Should return empty optional for SASL JAAS config by default")
        void shouldReturnEmptyOptionalForSaslJaasConfigByDefault() {
            assertFalse(kafkaConfig.getSaslJaasConfig().isPresent());
        }

        @Test
        @DisplayName("Should return empty optional for SSL truststore location by default")
        void shouldReturnEmptyOptionalForSslTruststoreLocationByDefault() {
            assertFalse(kafkaConfig.getSslTruststoreLocation().isPresent());
        }

        @Test
        @DisplayName("Should return empty optional for SSL truststore password by default")
        void shouldReturnEmptyOptionalForSslTruststorePasswordByDefault() {
            assertFalse(kafkaConfig.getSslTruststorePassword().isPresent());
        }

        @Test
        @DisplayName("Should return empty optional for SSL keystore location by default")
        void shouldReturnEmptyOptionalForSslKeystoreLocationByDefault() {
            assertFalse(kafkaConfig.getSslKeystoreLocation().isPresent());
        }

        @Test
        @DisplayName("Should return empty optional for SSL keystore password by default")
        void shouldReturnEmptyOptionalForSslKeystorePasswordByDefault() {
            assertFalse(kafkaConfig.getSslKeystorePassword().isPresent());
        }

        @Test
        @DisplayName("Should return empty optional for SSL key password by default")
        void shouldReturnEmptyOptionalForSslKeyPasswordByDefault() {
            assertFalse(kafkaConfig.getSslKeyPassword().isPresent());
        }

        @Test
        @DisplayName("Should handle SASL mechanism configuration")
        void shouldHandleSaslMechanismConfiguration() {
            String[] mechanisms = {"PLAIN", "SCRAM-SHA-256", "SCRAM-SHA-512", "OAUTHBEARER"};

            for (String mechanism : mechanisms) {
                KtestifyConfig config =
                        ConfigBuilder.create().saslMechanism(mechanism).build();

                assertTrue(config.getKafka().getSaslMechanism().isPresent());
                assertEquals(mechanism, config.getKafka().getSaslMechanism().get());
            }
        }

        @Test
        @DisplayName("Should handle SASL JAAS config")
        void shouldHandleSaslJaasConfig() {
            String jaasConfig =
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"admin-secret\";";
            KtestifyConfig config =
                    ConfigBuilder.create().saslJaasConfig(jaasConfig).build();

            assertTrue(config.getKafka().getSaslJaasConfig().isPresent());
            assertEquals(jaasConfig, config.getKafka().getSaslJaasConfig().get());
        }

        @Test
        @DisplayName("Should handle SSL truststore configuration")
        void shouldHandleSslTruststoreConfiguration() {
            KtestifyConfig config = ConfigBuilder.create()
                    .sslTruststore("/path/to/truststore.jks", "truststore-password")
                    .build();

            assertTrue(config.getKafka().getSslTruststoreLocation().isPresent());
            assertEquals(
                    "/path/to/truststore.jks",
                    config.getKafka().getSslTruststoreLocation().get());
            assertTrue(config.getKafka().getSslTruststorePassword().isPresent());
            assertEquals(
                    "truststore-password",
                    config.getKafka().getSslTruststorePassword().get());
        }

        @Test
        @DisplayName("Should handle SSL keystore configuration")
        void shouldHandleSslKeystoreConfiguration() {
            KtestifyConfig config = ConfigBuilder.create()
                    .sslKeystore("/path/to/keystore.jks", "keystore-password", "key-password")
                    .build();

            assertTrue(config.getKafka().getSslKeystoreLocation().isPresent());
            assertEquals(
                    "/path/to/keystore.jks",
                    config.getKafka().getSslKeystoreLocation().get());
            assertTrue(config.getKafka().getSslKeystorePassword().isPresent());
            assertEquals(
                    "keystore-password",
                    config.getKafka().getSslKeystorePassword().get());
            assertTrue(config.getKafka().getSslKeyPassword().isPresent());
            assertEquals("key-password", config.getKafka().getSslKeyPassword().get());
        }

        @Test
        @DisplayName("Should include security properties in common properties when configured")
        void shouldIncludeSecurityPropertiesInCommonPropertiesWhenConfigured() {
            KtestifyConfig config = ConfigBuilder.create()
                    .securityProtocol("SASL_SSL")
                    .saslMechanism("PLAIN")
                    .saslJaasConfig("test-jaas")
                    .sslTruststore("/trust.jks", "trustpwd")
                    .build();

            Properties props = config.getKafka().getCommonProperties();

            assertEquals("SASL_SSL", props.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
            assertEquals("PLAIN", props.get(SaslConfigs.SASL_MECHANISM));
            assertEquals("test-jaas", props.get(SaslConfigs.SASL_JAAS_CONFIG));
            assertEquals("/trust.jks", props.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
            assertEquals("trustpwd", props.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
        }

        @Test
        @DisplayName("Should include security properties in producer properties")
        void shouldIncludeSecurityPropertiesInProducerProperties() {
            KtestifyConfig config =
                    ConfigBuilder.create().saslMechanism("SCRAM-SHA-256").build();

            Properties props = config.getKafka().getProducerProperties();
            assertEquals("SCRAM-SHA-256", props.get(SaslConfigs.SASL_MECHANISM));
        }

        @Test
        @DisplayName("Should include security properties in consumer properties")
        void shouldIncludeSecurityPropertiesInConsumerProperties() {
            KtestifyConfig config =
                    ConfigBuilder.create().sslTruststore("/trust.jks", "pwd").build();

            Properties props = config.getKafka().getConsumerProperties();
            assertEquals("/trust.jks", props.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
        }

        @Test
        @DisplayName("Should not include security properties when not configured")
        void shouldNotIncludeSecurityPropertiesWhenNotConfigured() {
            Properties props = kafkaConfig.getCommonProperties();

            assertFalse(props.containsKey(SaslConfigs.SASL_MECHANISM));
            assertFalse(props.containsKey(SaslConfigs.SASL_JAAS_CONFIG));
            assertFalse(props.containsKey(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
        }
    }

    // ==========================================
    // RAW CONFIG TESTS
    // ==========================================

    @Nested
    @DisplayName("Raw Config Access")
    class RawConfigTests {

        @Test
        @DisplayName("Should return raw config object")
        void shouldReturnRawConfigObject() {
            assertNotNull(kafkaConfig.getRaw());
        }

        @Test
        @DisplayName("Should access nested values via raw config")
        void shouldAccessNestedValuesViaRawConfig() {
            assertTrue(kafkaConfig.getRaw().hasPath("bootstrap-servers"));
            assertEquals("localhost:9092", kafkaConfig.getRaw().getString("bootstrap-servers"));
        }
    }

    // ==========================================
    // EDGE CASES AND VALIDATION TESTS
    // ==========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty security values gracefully")
        void shouldHandleEmptySecurityValuesGracefully() {
            // Default config has empty strings for security which should result in empty optionals
            assertFalse(kafkaConfig.getSaslMechanism().isPresent());
            assertFalse(kafkaConfig.getSaslJaasConfig().isPresent());
        }

        @Test
        @DisplayName("Should preserve duration precision in properties")
        void shouldPreserveDurationPrecisionInProperties() {
            Properties props = kafkaConfig.getProducerProperties();
            assertEquals(1L, props.get(ProducerConfig.LINGER_MS_CONFIG));
        }

        @Test
        @DisplayName("Should handle large timeout values")
        void shouldHandleLargeTimeoutValues() {
            assertEquals(30000, kafkaConfig.getConsumerSessionTimeout().toMillis());
            assertEquals(10000, kafkaConfig.getConsumerHeartbeatInterval().toMillis());
        }

        @Test
        @DisplayName("Should maintain property type consistency")
        void shouldMaintainPropertyTypeConsistency() {
            Properties producerProps = kafkaConfig.getProducerProperties();
            Properties consumerProps = kafkaConfig.getConsumerProperties();

            // Integers should remain integers
            assertTrue(producerProps.get(ProducerConfig.RETRIES_CONFIG) instanceof Integer);
            assertTrue(consumerProps.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG) instanceof Integer);
        }

        @Test
        @DisplayName("Should handle consumer properties with and without custom group ID")
        void shouldHandleConsumerPropertiesWithAndWithoutCustomGroupId() {
            Properties defaultProps = kafkaConfig.getConsumerProperties();
            Properties customProps = kafkaConfig.getConsumerProperties("override-group");

            assertEquals("ktestify-consumer-group", defaultProps.get(ConsumerConfig.GROUP_ID_CONFIG));
            assertEquals("override-group", customProps.get(ConsumerConfig.GROUP_ID_CONFIG));

            // Other properties should remain the same
            assertEquals(
                    defaultProps.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG),
                    customProps.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        }

        @Test
        @DisplayName("Should handle map conversions without data loss")
        void shouldHandleMapConversionsWithoutDataLoss() {
            Map<String, String> producerMap = kafkaConfig.getProducerPropertiesAsMap();
            Map<String, String> consumerMap = kafkaConfig.getConsumerPropertiesAsMap();

            assertFalse(producerMap.isEmpty());
            assertFalse(consumerMap.isEmpty());

            // Verify key properties are present
            assertTrue(producerMap.containsKey(ProducerConfig.ACKS_CONFIG));
            assertTrue(consumerMap.containsKey(ConsumerConfig.GROUP_ID_CONFIG));
        }
    }
}
