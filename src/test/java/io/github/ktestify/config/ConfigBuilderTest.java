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

import static org.junit.jupiter.api.Assertions.*;

import com.typesafe.config.Config;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive unit tests for ConfigBuilder. Tests all builder methods, chaining, and edge cases. */
@DisplayName("ConfigBuilder Tests")
class ConfigBuilderTest {

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    // ==========================================
    // BUILDER CREATION TESTS
    // ==========================================

    @Nested
    @DisplayName("Builder Creation")
    class BuilderCreationTests {

        @Test
        @DisplayName("Should create new builder instance")
        void shouldCreateNewBuilderInstance() {
            ConfigBuilder builder = ConfigBuilder.create();
            assertNotNull(builder);
        }

        @Test
        @DisplayName("Should create independent builder instances")
        void shouldCreateIndependentBuilderInstances() {
            ConfigBuilder builder1 = ConfigBuilder.create();
            ConfigBuilder builder2 = ConfigBuilder.create();
            assertNotSame(builder1, builder2);
        }
    }

    // ==========================================
    // KAFKA COMMON SETTINGS TESTS
    // ==========================================

    @Nested
    @DisplayName("Kafka Common Settings")
    class KafkaCommonSettingsTests {

        @Test
        @DisplayName("Should set bootstrap servers")
        void shouldSetBootstrapServers() {
            KtestifyConfig config = ConfigBuilder.create()
                    .bootstrapServers("kafka1:9092,kafka2:9092")
                    .build();

            assertEquals("kafka1:9092,kafka2:9092", config.getKafka().getBootstrapServers());
        }

        @Test
        @DisplayName("Should set security protocol")
        void shouldSetSecurityProtocol() {
            KtestifyConfig config =
                    ConfigBuilder.create().securityProtocol("SASL_SSL").build();

            assertEquals("SASL_SSL", config.getKafka().getSecurityProtocol());
        }

        @Test
        @DisplayName("Should support method chaining for common settings")
        void shouldSupportMethodChainingForCommonSettings() {
            KtestifyConfig config = ConfigBuilder.create()
                    .bootstrapServers("localhost:9092")
                    .securityProtocol("PLAINTEXT")
                    .build();

            assertEquals("localhost:9092", config.getKafka().getBootstrapServers());
            assertEquals("PLAINTEXT", config.getKafka().getSecurityProtocol());
        }
    }

    // ==========================================
    // KAFKA SECURITY SETTINGS TESTS
    // ==========================================

    @Nested
    @DisplayName("Kafka Security Settings")
    class KafkaSecuritySettingsTests {

        @Test
        @DisplayName("Should set SASL mechanism")
        void shouldSetSaslMechanism() {
            KtestifyConfig config =
                    ConfigBuilder.create().saslMechanism("SCRAM-SHA-256").build();

            assertTrue(config.getKafka().getSaslMechanism().isPresent());
            assertEquals("SCRAM-SHA-256", config.getKafka().getSaslMechanism().get());
        }

        @Test
        @DisplayName("Should set SASL JAAS config")
        void shouldSetSaslJaasConfig() {
            String jaasConfig =
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"user\" password=\"pass\";";
            KtestifyConfig config =
                    ConfigBuilder.create().saslJaasConfig(jaasConfig).build();

            assertTrue(config.getKafka().getSaslJaasConfig().isPresent());
            assertEquals(jaasConfig, config.getKafka().getSaslJaasConfig().get());
        }

        @Test
        @DisplayName("Should set SSL truststore")
        void shouldSetSslTruststore() {
            KtestifyConfig config = ConfigBuilder.create()
                    .sslTruststore("/path/to/truststore.jks", "trustpass")
                    .build();

            assertTrue(config.getKafka().getSslTruststoreLocation().isPresent());
            assertEquals(
                    "/path/to/truststore.jks",
                    config.getKafka().getSslTruststoreLocation().get());
            assertTrue(config.getKafka().getSslTruststorePassword().isPresent());
            assertEquals(
                    "trustpass", config.getKafka().getSslTruststorePassword().get());
        }

        @Test
        @DisplayName("Should set SSL keystore")
        void shouldSetSslKeystore() {
            KtestifyConfig config = ConfigBuilder.create()
                    .sslKeystore("/path/to/keystore.jks", "keystorepass", "keypass")
                    .build();

            assertTrue(config.getKafka().getSslKeystoreLocation().isPresent());
            assertEquals(
                    "/path/to/keystore.jks",
                    config.getKafka().getSslKeystoreLocation().get());
            assertTrue(config.getKafka().getSslKeystorePassword().isPresent());
            assertEquals(
                    "keystorepass", config.getKafka().getSslKeystorePassword().get());
            assertTrue(config.getKafka().getSslKeyPassword().isPresent());
            assertEquals("keypass", config.getKafka().getSslKeyPassword().get());
        }

        @Test
        @DisplayName("Should chain all security settings")
        void shouldChainAllSecuritySettings() {
            KtestifyConfig config = ConfigBuilder.create()
                    .securityProtocol("SASL_SSL")
                    .saslMechanism("PLAIN")
                    .saslJaasConfig("test-jaas-config")
                    .sslTruststore("/trust.jks", "trustpwd")
                    .sslKeystore("/key.jks", "keystorepwd", "keypwd")
                    .build();

            assertEquals("SASL_SSL", config.getKafka().getSecurityProtocol());
            assertEquals("PLAIN", config.getKafka().getSaslMechanism().get());
            assertEquals(
                    "test-jaas-config", config.getKafka().getSaslJaasConfig().get());
        }
    }

    // ==========================================
    // KAFKA PRODUCER SETTINGS TESTS
    // ==========================================

    @Nested
    @DisplayName("Kafka Producer Settings")
    class KafkaProducerSettingsTests {

        @Test
        @DisplayName("Should set producer acks")
        void shouldSetProducerAcks() {
            KtestifyConfig config = ConfigBuilder.create().producerAcks("1").build();

            assertEquals("1", config.getKafka().getProducerAcks());
        }

        @Test
        @DisplayName("Should set producer retries")
        void shouldSetProducerRetries() {
            KtestifyConfig config = ConfigBuilder.create().producerRetries(5).build();

            assertEquals(5, config.getKafka().getProducerRetries());
        }

        @Test
        @DisplayName("Should chain producer settings")
        void shouldChainProducerSettings() {
            KtestifyConfig config =
                    ConfigBuilder.create().producerAcks("0").producerRetries(10).build();

            assertEquals("0", config.getKafka().getProducerAcks());
            assertEquals(10, config.getKafka().getProducerRetries());
        }
    }

    // ==========================================
    // KAFKA CONSUMER SETTINGS TESTS
    // ==========================================

    @Nested
    @DisplayName("Kafka Consumer Settings")
    class KafkaConsumerSettingsTests {

        @Test
        @DisplayName("Should set consumer group ID")
        void shouldSetConsumerGroupId() {
            KtestifyConfig config =
                    ConfigBuilder.create().consumerGroupId("my-test-group").build();

            assertEquals("my-test-group", config.getKafka().getConsumerGroupId());
        }

        @Test
        @DisplayName("Should set auto offset reset")
        void shouldSetAutoOffsetReset() {
            KtestifyConfig config =
                    ConfigBuilder.create().autoOffsetReset("latest").build();

            assertEquals("latest", config.getKafka().getConsumerAutoOffsetReset());
        }

        @Test
        @DisplayName("Should chain consumer settings")
        void shouldChainConsumerSettings() {
            KtestifyConfig config = ConfigBuilder.create()
                    .consumerGroupId("chain-group")
                    .autoOffsetReset("none")
                    .build();

            assertEquals("chain-group", config.getKafka().getConsumerGroupId());
            assertEquals("none", config.getKafka().getConsumerAutoOffsetReset());
        }
    }

    // ==========================================
    // SCHEMA REGISTRY SETTINGS TESTS
    // ==========================================

    @Nested
    @DisplayName("Schema Registry Settings")
    class SchemaRegistrySettingsTests {

        @Test
        @DisplayName("Should set schema registry URL")
        void shouldSetSchemaRegistryUrl() {
            KtestifyConfig config =
                    ConfigBuilder.create().schemaRegistryUrl("http://sr:8085").build();

            assertEquals("http://sr:8085", config.getSchemaRegistry().getUrl());
        }

        @Test
        @DisplayName("Should set schema registry authentication")
        void shouldSetSchemaRegistryAuth() {
            KtestifyConfig config = ConfigBuilder.create()
                    .schemaRegistryAuth("testuser", "testpass")
                    .build();

            assertTrue(
                    config.getSchemaRegistry().getBasicAuthCredentialsSource().isPresent());
            assertEquals(
                    "USER_INFO",
                    config.getSchemaRegistry().getBasicAuthCredentialsSource().get());
            assertTrue(config.getSchemaRegistry().getBasicAuthUserInfo().isPresent());
            assertEquals(
                    "testuser:testpass",
                    config.getSchemaRegistry().getBasicAuthUserInfo().get());
        }

        @Test
        @DisplayName("Should set auto register schemas")
        void shouldSetAutoRegisterSchemas() {
            KtestifyConfig config =
                    ConfigBuilder.create().autoRegisterSchemas(false).build();

            assertFalse(config.getSchemaRegistry().isAutoRegisterSchemas());
        }

        @Test
        @DisplayName("Should chain schema registry settings")
        void shouldChainSchemaRegistrySettings() {
            KtestifyConfig config = ConfigBuilder.create()
                    .schemaRegistryUrl("http://localhost:8081")
                    .schemaRegistryAuth("user", "pass")
                    .autoRegisterSchemas(true)
                    .build();

            assertEquals("http://localhost:8081", config.getSchemaRegistry().getUrl());
            assertTrue(config.getSchemaRegistry().isAutoRegisterSchemas());
        }
    }

    // ==========================================
    // FRAMEWORK SETTINGS TESTS
    // ==========================================

    @Nested
    @DisplayName("Framework Settings")
    class FrameworkSettingsTests {

        @Test
        @DisplayName("Should set default read timeout")
        void shouldSetDefaultReadTimeout() {
            KtestifyConfig config = ConfigBuilder.create()
                    .defaultReadTimeout(Duration.ofSeconds(45))
                    .build();

            assertEquals(Duration.ofSeconds(45), config.getFramework().getDefaultReadTimeout());
        }

        @Test
        @DisplayName("Should set consumer delta time")
        void shouldSetConsumerDeltaTime() {
            KtestifyConfig config = ConfigBuilder.create()
                    .consumerDeltaTime(Duration.ofSeconds(60))
                    .build();

            assertEquals(Duration.ofSeconds(60), config.getFramework().getConsumerDeltaTime());
        }

        @Test
        @DisplayName("Should set assets directory")
        void shouldSetAssetsDirectory() {
            KtestifyConfig config =
                    ConfigBuilder.create().assetsDirectory("/test/assets").build();

            assertTrue(config.getFramework().getAssetsDirectory().isPresent());
            assertEquals(
                    "/test/assets", config.getFramework().getAssetsDirectory().get());
        }

        @Test
        @DisplayName("Should set schemas directory")
        void shouldSetSchemasDirectory() {
            KtestifyConfig config =
                    ConfigBuilder.create().schemasDirectory("/test/schemas").build();

            assertTrue(config.getFramework().getSchemasDirectory().isPresent());
            assertEquals(
                    "/test/schemas", config.getFramework().getSchemasDirectory().get());
        }

        @Test
        @DisplayName("Should set output directory")
        void shouldSetOutputDirectory() {
            KtestifyConfig config =
                    ConfigBuilder.create().outputDirectory("/test/output").build();

            assertTrue(config.getFramework().getOutputDirectory().isPresent());
            assertEquals(
                    "/test/output", config.getFramework().getOutputDirectory().get());
        }

        @Test
        @DisplayName("Should set snapshot mode")
        void shouldSetSnapshotMode() {
            KtestifyConfig config = ConfigBuilder.create().snapshotMode(true).build();

            assertTrue(config.getFramework().isSnapshotMode());
        }

        @Test
        @DisplayName("Should set strict matching")
        void shouldSetStrictMatching() {
            KtestifyConfig config = ConfigBuilder.create().strictMatching(false).build();

            assertFalse(config.getFramework().isStrictMatching());
        }

        @Test
        @DisplayName("Should set max retries")
        void shouldSetMaxRetries() {
            KtestifyConfig config = ConfigBuilder.create().maxRetries(5).build();

            assertEquals(5, config.getFramework().getMaxRetries());
        }

        @Test
        @DisplayName("Should chain all framework settings")
        void shouldChainAllFrameworkSettings() {
            KtestifyConfig config = ConfigBuilder.create()
                    .defaultReadTimeout(Duration.ofSeconds(30))
                    .consumerDeltaTime(Duration.ofSeconds(15))
                    .assetsDirectory("/assets")
                    .schemasDirectory("/schemas")
                    .outputDirectory("/output")
                    .snapshotMode(true)
                    .strictMatching(false)
                    .maxRetries(10)
                    .build();

            assertEquals(Duration.ofSeconds(30), config.getFramework().getDefaultReadTimeout());
            assertEquals(Duration.ofSeconds(15), config.getFramework().getConsumerDeltaTime());
            assertEquals("/assets", config.getFramework().getAssetsDirectory().get());
            assertEquals("/schemas", config.getFramework().getSchemasDirectory().get());
            assertEquals("/output", config.getFramework().getOutputDirectory().get());
            assertTrue(config.getFramework().isSnapshotMode());
            assertFalse(config.getFramework().isStrictMatching());
            assertEquals(10, config.getFramework().getMaxRetries());
        }
    }

    // ==========================================
    // CUSTOM PATH TESTS
    // ==========================================

    @Nested
    @DisplayName("Custom Path Settings")
    class CustomPathTests {

        @Test
        @DisplayName("Should set custom configuration path")
        void shouldSetCustomConfigurationPath() {
            KtestifyConfig config = ConfigBuilder.create()
                    .set("custom.path.value", "test-value")
                    .build();

            assertTrue(config.getString("custom.path.value").isPresent());
            assertEquals("test-value", config.getString("custom.path.value").get());
        }

        @Test
        @DisplayName("Should set custom integer value")
        void shouldSetCustomIntegerValue() {
            KtestifyConfig config = ConfigBuilder.create().set("custom.int", 42).build();

            assertTrue(config.getRaw().hasPath("custom.int"));
            assertEquals(42, config.getRaw().getInt("custom.int"));
        }

        @Test
        @DisplayName("Should set custom boolean value")
        void shouldSetCustomBooleanValue() {
            KtestifyConfig config =
                    ConfigBuilder.create().set("custom.bool", true).build();

            assertTrue(config.getRaw().hasPath("custom.bool"));
            assertTrue(config.getRaw().getBoolean("custom.bool"));
        }

        @Test
        @DisplayName("Should override default values with custom paths")
        void shouldOverrideDefaultValuesWithCustomPaths() {
            KtestifyConfig config = ConfigBuilder.create()
                    .set("ktestify.kafka.bootstrap-servers", "custom:9092")
                    .build();

            assertEquals("custom:9092", config.getKafka().getBootstrapServers());
        }
    }

    // ==========================================
    // BUILD CONFIG TESTS
    // ==========================================

    @Nested
    @DisplayName("Build Config Methods")
    class BuildConfigTests {

        @Test
        @DisplayName("Should build KtestifyConfig")
        void shouldBuildKtestifyConfig() {
            KtestifyConfig config =
                    ConfigBuilder.create().bootstrapServers("test:9092").build();

            assertNotNull(config);
            assertEquals("test:9092", config.getKafka().getBootstrapServers());
        }

        @Test
        @DisplayName("Should build Config without loading singleton")
        void shouldBuildConfigWithoutLoadingSingleton() {
            Config config = ConfigBuilder.create().bootstrapServers("test:9092").buildConfig();

            assertNotNull(config);
            assertTrue(config.hasPath("ktestify.kafka.bootstrap-servers"));
            assertEquals("test:9092", config.getString("ktestify.kafka.bootstrap-servers"));

            // Singleton should not be loaded
            assertFalse(KtestifyConfig.isLoaded());
        }

        @Test
        @DisplayName("Should build multiple configs independently")
        void shouldBuildMultipleConfigsIndependently() {
            Config config1 =
                    ConfigBuilder.create().bootstrapServers("kafka1:9092").buildConfig();

            Config config2 =
                    ConfigBuilder.create().bootstrapServers("kafka2:9092").buildConfig();

            assertEquals("kafka1:9092", config1.getString("ktestify.kafka.bootstrap-servers"));
            assertEquals("kafka2:9092", config2.getString("ktestify.kafka.bootstrap-servers"));
        }
    }

    // ==========================================
    // COMPREHENSIVE CHAINING TESTS
    // ==========================================

    @Nested
    @DisplayName("Comprehensive Chaining")
    class ComprehensiveChainingTests {

        @Test
        @DisplayName("Should chain all available builder methods")
        void shouldChainAllAvailableBuilderMethods() {
            KtestifyConfig config = ConfigBuilder.create()
                    // Kafka common
                    .bootstrapServers("kafka:9092")
                    .securityProtocol("SASL_SSL")
                    // Kafka security
                    .saslMechanism("PLAIN")
                    .saslJaasConfig("jaas-config")
                    .sslTruststore("/trust.jks", "trustpass")
                    .sslKeystore("/key.jks", "keypass", "keypwd")
                    // Kafka producer
                    .producerAcks("all")
                    .producerRetries(5)
                    // Kafka consumer
                    .consumerGroupId("test-group")
                    .autoOffsetReset("earliest")
                    // Schema Registry
                    .schemaRegistryUrl("http://sr:8081")
                    .schemaRegistryAuth("user", "pass")
                    .autoRegisterSchemas(false)
                    // Framework
                    .defaultReadTimeout(Duration.ofSeconds(30))
                    .consumerDeltaTime(Duration.ofSeconds(15))
                    .assetsDirectory("/assets")
                    .schemasDirectory("/schemas")
                    .outputDirectory("/output")
                    .snapshotMode(true)
                    .strictMatching(false)
                    .maxRetries(10)
                    // Custom
                    .set("custom.value", "test")
                    .build();

            assertNotNull(config);
            assertEquals("kafka:9092", config.getKafka().getBootstrapServers());
            assertEquals("test-group", config.getKafka().getConsumerGroupId());
            assertEquals("http://sr:8081", config.getSchemaRegistry().getUrl());
            assertEquals(Duration.ofSeconds(30), config.getFramework().getDefaultReadTimeout());
        }

        @Test
        @DisplayName("Should preserve all settings in long chain")
        void shouldPreserveAllSettingsInLongChain() {
            ConfigBuilder builder = ConfigBuilder.create()
                    .bootstrapServers("b1:9092")
                    .securityProtocol("SSL")
                    .producerAcks("1")
                    .consumerGroupId("group1")
                    .schemaRegistryUrl("http://sr1:8081")
                    .defaultReadTimeout(Duration.ofSeconds(20))
                    .snapshotMode(true);

            KtestifyConfig config = builder.build();

            assertEquals("b1:9092", config.getKafka().getBootstrapServers());
            assertEquals("SSL", config.getKafka().getSecurityProtocol());
            assertEquals("1", config.getKafka().getProducerAcks());
            assertEquals("group1", config.getKafka().getConsumerGroupId());
            assertEquals("http://sr1:8081", config.getSchemaRegistry().getUrl());
            assertEquals(Duration.ofSeconds(20), config.getFramework().getDefaultReadTimeout());
            assertTrue(config.getFramework().isSnapshotMode());
        }
    }

    // ==========================================
    // EDGE CASES AND SPECIAL VALUES
    // ==========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty string values")
        void shouldHandleEmptyStringValues() {
            KtestifyConfig config = ConfigBuilder.create().bootstrapServers("").build();

            assertEquals("", config.getKafka().getBootstrapServers());
        }

        @Test
        @DisplayName("Should handle special characters in values")
        void shouldHandleSpecialCharactersInValues() {
            KtestifyConfig config = ConfigBuilder.create()
                    .consumerGroupId("group-with-special-chars_@#$%")
                    .build();

            assertEquals("group-with-special-chars_@#$%", config.getKafka().getConsumerGroupId());
        }

        @Test
        @DisplayName("Should handle zero duration")
        void shouldHandleZeroDuration() {
            KtestifyConfig config =
                    ConfigBuilder.create().defaultReadTimeout(Duration.ZERO).build();

            assertEquals(Duration.ZERO, config.getFramework().getDefaultReadTimeout());
        }

        @Test
        @DisplayName("Should handle very long duration")
        void shouldHandleVeryLongDuration() {
            KtestifyConfig config = ConfigBuilder.create()
                    .defaultReadTimeout(Duration.ofDays(365))
                    .build();

            assertEquals(Duration.ofDays(365), config.getFramework().getDefaultReadTimeout());
        }

        @Test
        @DisplayName("Should handle zero retries")
        void shouldHandleZeroRetries() {
            KtestifyConfig config =
                    ConfigBuilder.create().producerRetries(0).maxRetries(0).build();

            assertEquals(0, config.getKafka().getProducerRetries());
            assertEquals(0, config.getFramework().getMaxRetries());
        }

        @Test
        @DisplayName("Should handle negative retries")
        void shouldHandleNegativeRetries() {
            KtestifyConfig config = ConfigBuilder.create().producerRetries(-1).build();

            assertEquals(-1, config.getKafka().getProducerRetries());
        }

        @Test
        @DisplayName("Should handle URLs with different protocols")
        void shouldHandleUrlsWithDifferentProtocols() {
            KtestifyConfig config = ConfigBuilder.create()
                    .schemaRegistryUrl("https://secure-sr:8443")
                    .build();

            assertEquals("https://secure-sr:8443", config.getSchemaRegistry().getUrl());
        }

        @Test
        @DisplayName("Should handle multiple bootstrap servers")
        void shouldHandleMultipleBootstrapServers() {
            String servers = "broker1:9092,broker2:9092,broker3:9092";
            KtestifyConfig config =
                    ConfigBuilder.create().bootstrapServers(servers).build();

            assertEquals(servers, config.getKafka().getBootstrapServers());
        }
    }

    // ==========================================
    // DEFAULTS PRESERVATION TESTS
    // ==========================================

    @Nested
    @DisplayName("Defaults Preservation")
    class DefaultsPreservationTests {

        @Test
        @DisplayName("Should preserve defaults for unset values")
        void shouldPreserveDefaultsForUnsetValues() {
            KtestifyConfig config =
                    ConfigBuilder.create().bootstrapServers("custom:9092").build();

            // Custom value set
            assertEquals("custom:9092", config.getKafka().getBootstrapServers());

            // Defaults preserved
            assertEquals("all", config.getKafka().getProducerAcks());
            assertEquals("ktestify-consumer-group", config.getKafka().getConsumerGroupId());
            assertEquals("http://localhost:8081", config.getSchemaRegistry().getUrl());
        }

        @Test
        @DisplayName("Should allow partial overrides")
        void shouldAllowPartialOverrides() {
            KtestifyConfig config = ConfigBuilder.create().producerAcks("1").build();

            assertEquals("1", config.getKafka().getProducerAcks());
            assertEquals(3, config.getKafka().getProducerRetries()); // default
        }
    }
}
