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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive unit tests for ktestify configuration classes. Enhanced with additional test coverage for all
 * scenarios.
 */
@DisplayName("KtestifyConfig Tests")
class KtestifyConfigTest {

    @BeforeEach
    void setUp() {
        KtestifyConfig.reset();
    }

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    // ==========================================
    // BASIC LOADING TESTS
    // ==========================================

    @Nested
    @DisplayName("Configuration Loading")
    class ConfigurationLoadingTests {

        @Test
        @DisplayName("Should load default configuration")
        void shouldLoadDefaultConfiguration() {
            KtestifyConfig config = KtestifyConfig.load();

            assertNotNull(config);
            assertNotNull(config.getKafka());
            assertNotNull(config.getSchemaRegistry());
            assertNotNull(config.getFramework());
        }

        @Test
        @DisplayName("Should load configuration from Config object")
        void shouldLoadConfigurationFromConfigObject() {
            Config customConfig = ConfigFactory.parseString("""
                    ktestify {
                      kafka {
                        bootstrap-servers = "custom:9092"
                      }
                    }
                    """);

            KtestifyConfig config = KtestifyConfig.load(customConfig);
            assertEquals("custom:9092", config.getKafka().getBootstrapServers());
        }

        @Test
        @DisplayName("Should load configuration from file path")
        void shouldLoadConfigurationFromFilePath(@TempDir Path tempDir) throws IOException {
            String configContent = """
                    ktestify {
                      kafka {
                        bootstrap-servers = "file-based:9092"
                        security-protocol = "SSL"
                      }
                    }
                    """;

            Path configFile = tempDir.resolve("test-application.conf");
            Files.writeString(configFile, configContent);

            KtestifyConfig config = KtestifyConfig.load(configFile.toString());
            assertEquals("file-based:9092", config.getKafka().getBootstrapServers());
            assertEquals("SSL", config.getKafka().getSecurityProtocol());
        }

        @Test
        @DisplayName("Should merge file config with defaults")
        void shouldMergeFileConfigWithDefaults(@TempDir Path tempDir) throws IOException {
            String configContent = """
                    ktestify {
                      kafka {
                        bootstrap-servers = "override:9092"
                      }
                    }
                    """;

            Path configFile = tempDir.resolve("test-override.conf");
            Files.writeString(configFile, configContent);

            KtestifyConfig config = KtestifyConfig.load(configFile.toString());

            // Overridden value
            assertEquals("override:9092", config.getKafka().getBootstrapServers());

            // Default values preserved
            assertEquals("PLAINTEXT", config.getKafka().getSecurityProtocol());
            assertEquals("http://localhost:8081", config.getSchemaRegistry().getUrl());
        }
    }

    // ==========================================
    // KAFKA DEFAULTS TESTS
    // ==========================================

    @Nested
    @DisplayName("Kafka Configuration Defaults")
    class KafkaDefaultsTests {

        @Test
        void testKafkaDefaults() {
            KtestifyConfig config = KtestifyConfig.load();
            KafkaConfig kafka = config.getKafka();

            assertEquals("localhost:9092", kafka.getBootstrapServers());
            assertEquals("PLAINTEXT", kafka.getSecurityProtocol());
            assertEquals("all", kafka.getProducerAcks());
            assertEquals(3, kafka.getProducerRetries());
            assertEquals("ktestify-consumer-group", kafka.getConsumerGroupId());
            assertFalse(kafka.isConsumerEnableAutoCommit());
            assertEquals("earliest", kafka.getConsumerAutoOffsetReset());
        }

        @Test
        @DisplayName("Should return producer properties with all defaults")
        void shouldReturnProducerPropertiesWithAllDefaults() {
            KtestifyConfig config = KtestifyConfig.load();
            Properties props = config.getKafka().getProducerProperties();

            assertNotNull(props);
            assertEquals("localhost:9092", props.get("bootstrap.servers"));
            assertEquals("all", props.get("acks"));
            assertEquals("org.apache.kafka.common.serialization.StringSerializer", props.get("key.serializer"));
        }

        @Test
        @DisplayName("Should return consumer properties with all defaults")
        void shouldReturnConsumerPropertiesWithAllDefaults() {
            KtestifyConfig config = KtestifyConfig.load();
            Properties props = config.getKafka().getConsumerProperties();

            assertNotNull(props);
            assertEquals("localhost:9092", props.get("bootstrap.servers"));
            assertEquals("ktestify-consumer-group", props.get("group.id"));
            assertEquals("earliest", props.get("auto.offset.reset"));
        }
    }

    // ==========================================
    // SCHEMA REGISTRY DEFAULTS TESTS
    // ==========================================

    @Nested
    @DisplayName("Schema Registry Configuration Defaults")
    class SchemaRegistryDefaultsTests {

        @Test
        void testSchemaRegistryDefaults() {
            KtestifyConfig config = KtestifyConfig.load();
            SchemaRegistryConfig sr = config.getSchemaRegistry();

            assertEquals("http://localhost:8081", sr.getUrl());
            assertEquals(50, sr.getCacheCapacity());
            assertTrue(sr.isAutoRegisterSchemas());
            assertEquals("BACKWARD", sr.getCompatibilityLevel());
        }

        @Test
        @DisplayName("Should return schema registry properties")
        void shouldReturnSchemaRegistryProperties() {
            KtestifyConfig config = KtestifyConfig.load();
            Properties props = config.getSchemaRegistry().getProperties();

            assertNotNull(props);
            assertEquals("http://localhost:8081", props.get("schema.registry.url"));
            assertEquals("true", props.get("auto.register.schemas").toString());
        }
    }

    // ==========================================
    // FRAMEWORK DEFAULTS TESTS
    // ==========================================

    @Nested
    @DisplayName("Framework Configuration Defaults")
    class FrameworkDefaultsTests {

        @Test
        void testFrameworkDefaults() {
            KtestifyConfig config = KtestifyConfig.load();
            FrameworkConfig fw = config.getFramework();

            assertEquals(Duration.ofSeconds(10), fw.getDefaultReadTimeout());
            assertEquals(Duration.ofSeconds(20), fw.getConsumerDeltaTime());
            assertEquals(Duration.ofMillis(100), fw.getPollInterval());
            assertFalse(fw.isSnapshotMode());
            assertTrue(fw.isStrictMatching());
            assertEquals(3, fw.getMaxRetries());
        }

        @Test
        @DisplayName("Should have correct timeout helper methods")
        void shouldHaveCorrectTimeoutHelperMethods() {
            KtestifyConfig config = KtestifyConfig.load();
            FrameworkConfig fw = config.getFramework();

            assertEquals(10000L, fw.getDefaultReadTimeoutMillis());
            assertEquals(20L, fw.getConsumerDeltaTimeSeconds());
            assertEquals(100L, fw.getPollIntervalMillis());
            assertEquals(5000L, fw.getBufferTimeMillis());
        }
    }

    // ==========================================
    // CONFIG BUILDER TESTS
    // ==========================================

    @Nested
    @DisplayName("ConfigBuilder Integration")
    class ConfigBuilderTests {

        @Test
        @DisplayName("Should build config with custom settings")
        void shouldBuildConfigWithCustomSettings() {
            KtestifyConfig config = ConfigBuilder.create()
                    .bootstrapServers("kafka:9092")
                    .schemaRegistryUrl("http://schema-registry:8081")
                    .defaultReadTimeout(Duration.ofSeconds(30))
                    .consumerGroupId("test-group")
                    .build();

            assertEquals("kafka:9092", config.getKafka().getBootstrapServers());
            assertEquals(
                    "http://schema-registry:8081", config.getSchemaRegistry().getUrl());
            assertEquals(Duration.ofSeconds(30), config.getFramework().getDefaultReadTimeout());
            assertEquals("test-group", config.getKafka().getConsumerGroupId());
        }

        @Test
        @DisplayName("Should build config with multiple overrides")
        void shouldBuildConfigWithMultipleOverrides() {
            KtestifyConfig config = ConfigBuilder.create()
                    .bootstrapServers("broker1:9092,broker2:9092")
                    .securityProtocol("SASL_SSL")
                    .producerAcks("1")
                    .consumerGroupId("custom-group")
                    .schemaRegistryUrl("https://secure-sr:8443")
                    .snapshotMode(true)
                    .strictMatching(false)
                    .maxRetries(5)
                    .build();

            KafkaConfig kafka = config.getKafka();
            assertEquals("broker1:9092,broker2:9092", kafka.getBootstrapServers());
            assertEquals("SASL_SSL", kafka.getSecurityProtocol());
            assertEquals("1", kafka.getProducerAcks());
            assertEquals("custom-group", kafka.getConsumerGroupId());

            SchemaRegistryConfig sr = config.getSchemaRegistry();
            assertEquals("https://secure-sr:8443", sr.getUrl());

            FrameworkConfig fw = config.getFramework();
            assertTrue(fw.isSnapshotMode());
            assertFalse(fw.isStrictMatching());
            assertEquals(5, fw.getMaxRetries());
        }
    }

    // ==========================================
    // SINGLETON BEHAVIOR TESTS
    // ==========================================

    @Nested
    @DisplayName("Singleton Behavior")
    class SingletonBehaviorTests {

        @Test
        @DisplayName("Should maintain singleton instance")
        void shouldMaintainSingletonInstance() {
            KtestifyConfig config1 = KtestifyConfig.load();
            KtestifyConfig config2 = KtestifyConfig.getOrLoad();

            assertSame(config1, config2);
        }

        @Test
        @DisplayName("Should return same instance on multiple getInstance calls")
        void shouldReturnSameInstanceOnMultipleGetInstanceCalls() {
            KtestifyConfig.load();
            KtestifyConfig config1 = KtestifyConfig.getInstance();
            KtestifyConfig config2 = KtestifyConfig.getInstance();

            assertSame(config1, config2);
        }

        @Test
        @DisplayName("Should throw exception when getInstance called before load")
        void shouldThrowExceptionWhenGetInstanceCalledBeforeLoad() {
            assertThrows(IllegalStateException.class, KtestifyConfig::getInstance);
        }

        @Test
        @DisplayName("Should check if config is loaded")
        void shouldCheckIfConfigIsLoaded() {
            assertFalse(KtestifyConfig.isLoaded());
            KtestifyConfig.load();
            assertTrue(KtestifyConfig.isLoaded());
        }

        @Test
        @DisplayName("Should load automatically with getOrLoad when not loaded")
        void shouldLoadAutomaticallyWithGetOrLoadWhenNotLoaded() {
            assertFalse(KtestifyConfig.isLoaded());
            KtestifyConfig config = KtestifyConfig.getOrLoad();
            assertNotNull(config);
            assertTrue(KtestifyConfig.isLoaded());
        }

        @Test
        @DisplayName("Should reset singleton instance")
        void shouldResetSingletonInstance() {
            KtestifyConfig.load();
            assertTrue(KtestifyConfig.isLoaded());

            KtestifyConfig.reset();
            assertFalse(KtestifyConfig.isLoaded());
        }

        @Test
        @DisplayName("Should allow reloading after reset")
        void shouldAllowReloadingAfterReset() {
            KtestifyConfig config1 = KtestifyConfig.load();
            assertNotNull(config1);

            KtestifyConfig.reset();

            KtestifyConfig config2 = KtestifyConfig.load();
            assertNotNull(config2);
            assertNotSame(config1, config2);
        }

        @Test
        @DisplayName("Should reload with different config after reset")
        void shouldReloadWithDifferentConfigAfterReset() {
            KtestifyConfig config1 = KtestifyConfig.load();
            assertEquals("localhost:9092", config1.getKafka().getBootstrapServers());

            KtestifyConfig.reset();

            KtestifyConfig config2 =
                    ConfigBuilder.create().bootstrapServers("different:9092").build();
            assertEquals("different:9092", config2.getKafka().getBootstrapServers());
        }
    }

    // ==========================================
    // STRING GETTER TESTS
    // ==========================================

    @Nested
    @DisplayName("String Value Getters")
    class StringGetterTests {

        @Test
        @DisplayName("Should get string value as optional")
        void shouldGetStringValueAsOptional() {
            KtestifyConfig config = KtestifyConfig.load();

            assertTrue(config.getString("ktestify.kafka.bootstrap-servers").isPresent());
            assertFalse(config.getString("non.existent.path").isPresent());
        }

        @Test
        @DisplayName("Should get string value with default")
        void shouldGetStringValueWithDefault() {
            KtestifyConfig config = KtestifyConfig.load();

            assertEquals("localhost:9092", config.getString("ktestify.kafka.bootstrap-servers", "default"));
            assertEquals("default", config.getString("non.existent.path", "default"));
        }

        @Test
        @DisplayName("Should retrieve nested configuration values")
        void shouldRetrieveNestedConfigurationValues() {
            KtestifyConfig config = KtestifyConfig.load();

            assertTrue(config.getString("ktestify.kafka.producer.acks").isPresent());
            assertEquals("all", config.getString("ktestify.kafka.producer.acks").get());
        }

        @Test
        @DisplayName("Should handle custom paths via builder")
        void shouldHandleCustomPathsViaBuilder() {
            KtestifyConfig config = ConfigBuilder.create()
                    .set("custom.test.value", "custom-value")
                    .build();

            assertTrue(config.getString("custom.test.value").isPresent());
            assertEquals("custom-value", config.getString("custom.test.value").get());
        }
    }

    // ==========================================
    // RAW CONFIG ACCESS TESTS
    // ==========================================

    @Nested
    @DisplayName("Raw Config Access")
    class RawConfigAccessTests {

        @Test
        @DisplayName("Should get raw config object")
        void shouldGetRawConfigObject() {
            KtestifyConfig config = KtestifyConfig.load();
            Config raw = config.getRaw();

            assertNotNull(raw);
            assertTrue(raw.hasPath("ktestify"));
        }

        @Test
        @DisplayName("Should access all sections via raw config")
        void shouldAccessAllSectionsViaRawConfig() {
            KtestifyConfig config = KtestifyConfig.load();
            Config raw = config.getRaw();

            assertTrue(raw.hasPath("ktestify.kafka"));
            assertTrue(raw.hasPath("ktestify.schema-registry"));
            assertTrue(raw.hasPath("ktestify.framework"));
        }

        @Test
        @DisplayName("Should access kafka raw config")
        void shouldAccessKafkaRawConfig() {
            KtestifyConfig config = KtestifyConfig.load();
            Config kafkaRaw = config.getKafka().getRaw();

            assertNotNull(kafkaRaw);
            assertTrue(kafkaRaw.hasPath("bootstrap-servers"));
        }

        @Test
        @DisplayName("Should access schema registry raw config")
        void shouldAccessSchemaRegistryRawConfig() {
            KtestifyConfig config = KtestifyConfig.load();
            Config srRaw = config.getSchemaRegistry().getRaw();

            assertNotNull(srRaw);
            assertTrue(srRaw.hasPath("url"));
        }

        @Test
        @DisplayName("Should access framework raw config")
        void shouldAccessFrameworkRawConfig() {
            KtestifyConfig config = KtestifyConfig.load();
            Config fwRaw = config.getFramework().getRaw();

            assertNotNull(fwRaw);
            assertTrue(fwRaw.hasPath("timeouts"));
        }
    }

    // ==========================================
    // THREAD SAFETY TESTS
    // ==========================================

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent load calls safely")
        void shouldHandleConcurrentLoadCallsSafely() throws InterruptedException {
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            KtestifyConfig[] configs = new KtestifyConfig[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    configs[index] = KtestifyConfig.load();
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // All should be same instance
            for (int i = 1; i < threadCount; i++) {
                assertSame(configs[0], configs[i]);
            }
        }

        @Test
        @DisplayName("Should handle concurrent getInstance calls safely")
        void shouldHandleConcurrentGetInstanceCallsSafely() throws InterruptedException {
            KtestifyConfig.load();

            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            KtestifyConfig[] configs = new KtestifyConfig[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    configs[index] = KtestifyConfig.getInstance();
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // All should be same instance
            for (int i = 1; i < threadCount; i++) {
                assertSame(configs[0], configs[i]);
            }
        }
    }

    // ==========================================
    // EDGE CASES AND VALIDATION TESTS
    // ==========================================

    @Nested
    @DisplayName("Edge Cases and Validation")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle config with only required fields")
        void shouldHandleConfigWithOnlyRequiredFields() {
            Config minimalConfig = ConfigFactory.parseString("""
                    ktestify {
                      kafka {
                        bootstrap-servers = "minimal:9092"
                      }
                    }
                    """);

            KtestifyConfig config = KtestifyConfig.load(minimalConfig);
            assertNotNull(config);
            assertEquals("minimal:9092", config.getKafka().getBootstrapServers());
        }

        @Test
        @DisplayName("Should handle config with all fields customized")
        void shouldHandleConfigWithAllFieldsCustomized() {
            KtestifyConfig config = ConfigBuilder.create()
                    .bootstrapServers("complete:9092")
                    .securityProtocol("SASL_SSL")
                    .saslMechanism("PLAIN")
                    .producerAcks("0")
                    .producerRetries(10)
                    .consumerGroupId("complete-group")
                    .autoOffsetReset("latest")
                    .schemaRegistryUrl("http://complete-sr:8081")
                    .autoRegisterSchemas(false)
                    .defaultReadTimeout(Duration.ofSeconds(60))
                    .consumerDeltaTime(Duration.ofSeconds(30))
                    .assetsDirectory("/complete/assets")
                    .schemasDirectory("/complete/schemas")
                    .outputDirectory("/complete/output")
                    .snapshotMode(true)
                    .strictMatching(false)
                    .maxRetries(20)
                    .build();

            assertNotNull(config);
            assertEquals("complete:9092", config.getKafka().getBootstrapServers());
            assertEquals("complete-group", config.getKafka().getConsumerGroupId());
            assertEquals("http://complete-sr:8081", config.getSchemaRegistry().getUrl());
            assertEquals(Duration.ofSeconds(60), config.getFramework().getDefaultReadTimeout());
        }
    }

    @Test
    void testGetProducerProperties() {
        KtestifyConfig config = KtestifyConfig.load();
        Properties props = config.getKafka().getProducerProperties();

        assertNotNull(props);
        assertEquals("localhost:9092", props.get("bootstrap.servers"));
        assertEquals("all", props.get("acks"));
        assertEquals("org.apache.kafka.common.serialization.StringSerializer", props.get("key.serializer"));
    }

    @Test
    void testGetConsumerProperties() {
        KtestifyConfig config = KtestifyConfig.load();
        Properties props = config.getKafka().getConsumerProperties();

        assertNotNull(props);
        assertEquals("localhost:9092", props.get("bootstrap.servers"));
        assertEquals("ktestify-consumer-group", props.get("group.id"));
        assertEquals("earliest", props.get("auto.offset.reset"));
    }

    @Test
    void testConfigBuilder() {
        KtestifyConfig config = ConfigBuilder.create()
                .bootstrapServers("kafka:9092")
                .schemaRegistryUrl("http://schema-registry:8081")
                .defaultReadTimeout(Duration.ofSeconds(30))
                .consumerGroupId("test-group")
                .build();

        assertEquals("kafka:9092", config.getKafka().getBootstrapServers());
        assertEquals("http://schema-registry:8081", config.getSchemaRegistry().getUrl());
        assertEquals(Duration.ofSeconds(30), config.getFramework().getDefaultReadTimeout());
        assertEquals("test-group", config.getKafka().getConsumerGroupId());
    }

    @Test
    void testSingletonBehavior() {
        KtestifyConfig config1 = KtestifyConfig.load();
        KtestifyConfig config2 = KtestifyConfig.getOrLoad();

        assertSame(config1, config2);
    }

    @Test
    void testGetInstanceWithoutLoad() {
        assertThrows(IllegalStateException.class, KtestifyConfig::getInstance);
    }

    @Test
    void testIsLoaded() {
        assertFalse(KtestifyConfig.isLoaded());
        KtestifyConfig.load();
        assertTrue(KtestifyConfig.isLoaded());
    }

    @Test
    void testGetStringOptional() {
        KtestifyConfig config = KtestifyConfig.load();

        assertTrue(config.getString("ktestify.kafka.bootstrap-servers").isPresent());
        assertFalse(config.getString("non.existent.path").isPresent());
    }

    @Test
    void testGetStringWithDefault() {
        KtestifyConfig config = KtestifyConfig.load();

        assertEquals("localhost:9092", config.getString("ktestify.kafka.bootstrap-servers", "default"));
        assertEquals("default", config.getString("non.existent.path", "default"));
    }

    @Test
    void testSchemaRegistryProperties() {
        KtestifyConfig config = KtestifyConfig.load();
        Properties props = config.getSchemaRegistry().getProperties();

        assertNotNull(props);
        assertEquals("http://localhost:8081", props.get("schema.registry.url"));
        assertEquals("true", props.get("auto.register.schemas").toString());
    }

    @Test
    void testFrameworkTimeoutHelpers() {
        KtestifyConfig config = KtestifyConfig.load();
        FrameworkConfig fw = config.getFramework();

        assertEquals(10000L, fw.getDefaultReadTimeoutMillis());
        assertEquals(20L, fw.getConsumerDeltaTimeSeconds());
        assertEquals(100L, fw.getPollIntervalMillis());
        assertEquals(5000L, fw.getBufferTimeMillis());
    }
}
