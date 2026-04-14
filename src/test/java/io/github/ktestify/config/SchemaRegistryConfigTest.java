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

import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for SchemaRegistryConfig. Tests all getters, property conversions, authentication, SSL, and
 * edge cases.
 */
@DisplayName("SchemaRegistryConfig Tests")
class SchemaRegistryConfigTest {

    private SchemaRegistryConfig schemaRegistryConfig;

    @BeforeEach
    void setUp() {
        KtestifyConfig.reset();
        KtestifyConfig config = KtestifyConfig.load();
        schemaRegistryConfig = config.getSchemaRegistry();
    }

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    // ==========================================
    // BASIC PROPERTIES TESTS
    // ==========================================

    @Nested
    @DisplayName("Basic Properties")
    class BasicPropertiesTests {

        @Test
        @DisplayName("Should get URL")
        void shouldGetUrl() {
            assertEquals("http://localhost:8081", schemaRegistryConfig.getUrl());
        }

        @Test
        @DisplayName("Should get cache capacity")
        void shouldGetCacheCapacity() {
            assertEquals(50, schemaRegistryConfig.getCacheCapacity());
        }

        @Test
        @DisplayName("Should get auto register schemas")
        void shouldGetAutoRegisterSchemas() {
            assertTrue(schemaRegistryConfig.isAutoRegisterSchemas());
        }

        @Test
        @DisplayName("Should get compatibility level")
        void shouldGetCompatibilityLevel() {
            assertEquals("BACKWARD", schemaRegistryConfig.getCompatibilityLevel());
        }

        @Test
        @DisplayName("Should handle custom URL")
        void shouldHandleCustomUrl() {
            KtestifyConfig config = ConfigBuilder.create()
                    .schemaRegistryUrl("http://schema-registry:8085")
                    .build();

            assertEquals(
                    "http://schema-registry:8085", config.getSchemaRegistry().getUrl());
        }

        @Test
        @DisplayName("Should handle HTTPS URL")
        void shouldHandleHttpsUrl() {
            KtestifyConfig config = ConfigBuilder.create()
                    .schemaRegistryUrl("https://secure-sr.example.com:8443")
                    .build();

            assertEquals(
                    "https://secure-sr.example.com:8443",
                    config.getSchemaRegistry().getUrl());
        }

        @Test
        @DisplayName("Should handle auto register schemas disabled")
        void shouldHandleAutoRegisterSchemasDisabled() {
            KtestifyConfig config =
                    ConfigBuilder.create().autoRegisterSchemas(false).build();

            assertFalse(config.getSchemaRegistry().isAutoRegisterSchemas());
        }
    }

    // ==========================================
    // AUTHENTICATION TESTS
    // ==========================================

    @Nested
    @DisplayName("Authentication")
    class AuthenticationTests {

        @Test
        @DisplayName("Should return empty optional for basic auth credentials source by default")
        void shouldReturnEmptyOptionalForBasicAuthCredentialsSourceByDefault() {
            assertFalse(schemaRegistryConfig.getBasicAuthCredentialsSource().isPresent());
        }

        @Test
        @DisplayName("Should return empty optional for basic auth user info by default")
        void shouldReturnEmptyOptionalForBasicAuthUserInfoByDefault() {
            assertFalse(schemaRegistryConfig.getBasicAuthUserInfo().isPresent());
        }

        @Test
        @DisplayName("Should handle basic authentication configuration")
        void shouldHandleBasicAuthenticationConfiguration() {
            KtestifyConfig config = ConfigBuilder.create()
                    .schemaRegistryAuth("testuser", "testpassword")
                    .build();

            SchemaRegistryConfig sr = config.getSchemaRegistry();
            assertTrue(sr.getBasicAuthCredentialsSource().isPresent());
            assertEquals("USER_INFO", sr.getBasicAuthCredentialsSource().get());
            assertTrue(sr.getBasicAuthUserInfo().isPresent());
            assertEquals("testuser:testpassword", sr.getBasicAuthUserInfo().get());
        }

        @Test
        @DisplayName("Should handle authentication with special characters")
        void shouldHandleAuthenticationWithSpecialCharacters() {
            KtestifyConfig config = ConfigBuilder.create()
                    .schemaRegistryAuth("user@domain.com", "p@ss:w0rd!")
                    .build();

            SchemaRegistryConfig sr = config.getSchemaRegistry();
            assertTrue(sr.getBasicAuthUserInfo().isPresent());
            assertEquals("user@domain.com:p@ss:w0rd!", sr.getBasicAuthUserInfo().get());
        }

        @Test
        @DisplayName("Should handle authentication with empty password")
        void shouldHandleAuthenticationWithEmptyPassword() {
            KtestifyConfig config =
                    ConfigBuilder.create().schemaRegistryAuth("user", "").build();

            SchemaRegistryConfig sr = config.getSchemaRegistry();
            assertTrue(sr.getBasicAuthUserInfo().isPresent());
            assertEquals("user:", sr.getBasicAuthUserInfo().get());
        }
    }

    // ==========================================
    // SSL PROPERTIES TESTS
    // ==========================================

    @Nested
    @DisplayName("SSL Properties")
    class SslPropertiesTests {

        @Test
        @DisplayName("Should return empty optional for SSL truststore location by default")
        void shouldReturnEmptyOptionalForSslTruststoreLocationByDefault() {
            assertFalse(schemaRegistryConfig.getSslTruststoreLocation().isPresent());
        }

        @Test
        @DisplayName("Should return empty optional for SSL truststore password by default")
        void shouldReturnEmptyOptionalForSslTruststorePasswordByDefault() {
            assertFalse(schemaRegistryConfig.getSslTruststorePassword().isPresent());
        }

        @Test
        @DisplayName("Should return empty optional for SSL keystore location by default")
        void shouldReturnEmptyOptionalForSslKeystoreLocationByDefault() {
            assertFalse(schemaRegistryConfig.getSslKeystoreLocation().isPresent());
        }

        @Test
        @DisplayName("Should return empty optional for SSL keystore password by default")
        void shouldReturnEmptyOptionalForSslKeystorePasswordByDefault() {
            assertFalse(schemaRegistryConfig.getSslKeystorePassword().isPresent());
        }

        @Test
        @DisplayName("Should handle SSL configuration via custom path")
        void shouldHandleSslConfigurationViaCustomPath() {
            KtestifyConfig config = ConfigBuilder.create()
                    .set("ktestify.schema-registry.ssl.truststore-location", "/path/to/sr-truststore.jks")
                    .set("ktestify.schema-registry.ssl.truststore-password", "trustpwd")
                    .set("ktestify.schema-registry.ssl.keystore-location", "/path/to/sr-keystore.jks")
                    .set("ktestify.schema-registry.ssl.keystore-password", "keystorepwd")
                    .build();

            SchemaRegistryConfig sr = config.getSchemaRegistry();
            assertTrue(sr.getSslTruststoreLocation().isPresent());
            assertEquals(
                    "/path/to/sr-truststore.jks", sr.getSslTruststoreLocation().get());
            assertTrue(sr.getSslTruststorePassword().isPresent());
            assertEquals("trustpwd", sr.getSslTruststorePassword().get());
            assertTrue(sr.getSslKeystoreLocation().isPresent());
            assertEquals("/path/to/sr-keystore.jks", sr.getSslKeystoreLocation().get());
            assertTrue(sr.getSslKeystorePassword().isPresent());
            assertEquals("keystorepwd", sr.getSslKeystorePassword().get());
        }
    }

    // ==========================================
    // PROPERTIES CONVERSION TESTS
    // ==========================================

    @Nested
    @DisplayName("Properties Conversion")
    class PropertiesConversionTests {

        @Test
        @DisplayName("Should convert to Properties object")
        void shouldConvertToPropertiesObject() {
            Properties props = schemaRegistryConfig.getProperties();

            assertNotNull(props);
            assertEquals("http://localhost:8081", props.get("schema.registry.url"));
            assertEquals("true", props.get("auto.register.schemas").toString());
            assertEquals("50", props.get("schema.registry.cache.capacity").toString());
        }

        @Test
        @DisplayName("Should include authentication in properties when configured")
        void shouldIncludeAuthenticationInPropertiesWhenConfigured() {
            KtestifyConfig config =
                    ConfigBuilder.create().schemaRegistryAuth("user", "pass").build();

            Properties props = config.getSchemaRegistry().getProperties();

            assertEquals("USER_INFO", props.get("basic.auth.credentials.source"));
            assertEquals("user:pass", props.get("basic.auth.user.info"));
        }

        @Test
        @DisplayName("Should not include authentication in properties when not configured")
        void shouldNotIncludeAuthenticationInPropertiesWhenNotConfigured() {
            Properties props = schemaRegistryConfig.getProperties();

            assertFalse(props.containsKey("basic.auth.credentials.source"));
            assertFalse(props.containsKey("basic.auth.user.info"));
        }

        @Test
        @DisplayName("Should include SSL properties when configured")
        void shouldIncludeSslPropertiesWhenConfigured() {
            KtestifyConfig config = ConfigBuilder.create()
                    .set("ktestify.schema-registry.ssl.truststore-location", "/trust.jks")
                    .set("ktestify.schema-registry.ssl.truststore-password", "trustpwd")
                    .build();

            Properties props = config.getSchemaRegistry().getProperties();

            assertEquals("/trust.jks", props.get("schema.registry.ssl.truststore.location"));
            assertEquals("trustpwd", props.get("schema.registry.ssl.truststore.password"));
        }

        @Test
        @DisplayName("Should convert properties to map")
        void shouldConvertPropertiesToMap() {
            Map<String, String> map = schemaRegistryConfig.getPropertiesAsMap();

            assertNotNull(map);
            assertFalse(map.isEmpty());
            assertEquals("http://localhost:8081", map.get("schema.registry.url"));
            assertEquals("true", map.get("auto.register.schemas"));
        }

        @Test
        @DisplayName("Should preserve all properties in map conversion")
        void shouldPreserveAllPropertiesInMapConversion() {
            KtestifyConfig config = ConfigBuilder.create()
                    .schemaRegistryUrl("http://sr:8081")
                    .schemaRegistryAuth("user", "pass")
                    .autoRegisterSchemas(false)
                    .build();

            Map<String, String> map = config.getSchemaRegistry().getPropertiesAsMap();

            assertEquals("http://sr:8081", map.get("schema.registry.url"));
            assertEquals("false", map.get("auto.register.schemas"));
            assertEquals("USER_INFO", map.get("basic.auth.credentials.source"));
            assertEquals("user:pass", map.get("basic.auth.user.info"));
        }
    }

    // ==========================================
    // MERGE WITH KAFKA PROPERTIES TESTS
    // ==========================================

    @Nested
    @DisplayName("Merge with Kafka Properties")
    class MergeWithKafkaPropertiesTests {

        @Test
        @DisplayName("Should merge with Kafka properties")
        void shouldMergeWithKafkaProperties() {
            Properties kafkaProps = new Properties();
            kafkaProps.put("bootstrap.servers", "kafka:9092");
            kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            Properties merged = schemaRegistryConfig.mergeWithKafkaProperties(kafkaProps);

            assertNotNull(merged);
            // Kafka properties should be present
            assertEquals("kafka:9092", merged.get("bootstrap.servers"));
            assertEquals("org.apache.kafka.common.serialization.StringSerializer", merged.get("key.serializer"));
            // Schema Registry properties should be present
            assertEquals("http://localhost:8081", merged.get("schema.registry.url"));
            assertEquals("true", merged.get("auto.register.schemas").toString());
        }

        @Test
        @DisplayName("Should preserve Kafka properties in merge")
        void shouldPreserveKafkaPropertiesInMerge() {
            Properties kafkaProps = new Properties();
            kafkaProps.put("acks", "all");
            kafkaProps.put("retries", 3);

            Properties merged = schemaRegistryConfig.mergeWithKafkaProperties(kafkaProps);

            assertEquals("all", merged.get("acks"));
            assertEquals(3, merged.get("retries"));
        }

        @Test
        @DisplayName("Should not lose Schema Registry properties in merge")
        void shouldNotLoseSchemaRegistryPropertiesInMerge() {
            KtestifyConfig config =
                    ConfigBuilder.create().schemaRegistryAuth("user", "pass").build();

            Properties kafkaProps = new Properties();
            kafkaProps.put("bootstrap.servers", "kafka:9092");

            Properties merged = config.getSchemaRegistry().mergeWithKafkaProperties(kafkaProps);

            assertEquals("kafka:9092", merged.get("bootstrap.servers"));
            assertEquals("USER_INFO", merged.get("basic.auth.credentials.source"));
            assertEquals("user:pass", merged.get("basic.auth.user.info"));
        }

        @Test
        @DisplayName("Should handle empty Kafka properties")
        void shouldHandleEmptyKafkaProperties() {
            Properties emptyKafkaProps = new Properties();
            Properties merged = schemaRegistryConfig.mergeWithKafkaProperties(emptyKafkaProps);

            assertNotNull(merged);
            assertEquals("http://localhost:8081", merged.get("schema.registry.url"));
        }

        @Test
        @DisplayName("Should handle merge with authentication and SSL")
        void shouldHandleMergeWithAuthenticationAndSsl() {
            KtestifyConfig config = ConfigBuilder.create()
                    .schemaRegistryAuth("sruser", "srpass")
                    .set("ktestify.schema-registry.ssl.truststore-location", "/sr-trust.jks")
                    .build();

            Properties kafkaProps = new Properties();
            kafkaProps.put("security.protocol", "SSL");

            Properties merged = config.getSchemaRegistry().mergeWithKafkaProperties(kafkaProps);

            assertEquals("SSL", merged.get("security.protocol"));
            assertEquals("sruser:srpass", merged.get("basic.auth.user.info"));
            assertEquals("/sr-trust.jks", merged.get("schema.registry.ssl.truststore.location"));
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
            assertNotNull(schemaRegistryConfig.getRaw());
        }

        @Test
        @DisplayName("Should access nested values via raw config")
        void shouldAccessNestedValuesViaRawConfig() {
            assertTrue(schemaRegistryConfig.getRaw().hasPath("url"));
            assertEquals("http://localhost:8081", schemaRegistryConfig.getRaw().getString("url"));
        }

        @Test
        @DisplayName("Should access auth config via raw config")
        void shouldAccessAuthConfigViaRawConfig() {
            assertTrue(schemaRegistryConfig.getRaw().hasPath("auth"));
            assertNotNull(schemaRegistryConfig.getRaw().getConfig("auth"));
        }
    }

    // ==========================================
    // EDGE CASES TESTS
    // ==========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty string values gracefully")
        void shouldHandleEmptyStringValuesGracefully() {
            // Default config has empty strings for optional fields
            assertFalse(schemaRegistryConfig.getBasicAuthCredentialsSource().isPresent());
            assertFalse(schemaRegistryConfig.getBasicAuthUserInfo().isPresent());
            assertFalse(schemaRegistryConfig.getSslTruststoreLocation().isPresent());
        }

        @Test
        @DisplayName("Should handle URL with path")
        void shouldHandleUrlWithPath() {
            KtestifyConfig config = ConfigBuilder.create()
                    .schemaRegistryUrl("http://localhost:8081/schema-registry")
                    .build();

            assertEquals(
                    "http://localhost:8081/schema-registry",
                    config.getSchemaRegistry().getUrl());
        }

        @Test
        @DisplayName("Should handle URL with query parameters")
        void shouldHandleUrlWithQueryParameters() {
            KtestifyConfig config = ConfigBuilder.create()
                    .schemaRegistryUrl("http://localhost:8081?param=value")
                    .build();

            assertEquals(
                    "http://localhost:8081?param=value",
                    config.getSchemaRegistry().getUrl());
        }

        @Test
        @DisplayName("Should handle large cache capacity")
        void shouldHandleLargeCacheCapacity() {
            KtestifyConfig config = ConfigBuilder.create()
                    .set("ktestify.schema-registry.cache-capacity", 10000)
                    .build();

            assertEquals(10000, config.getSchemaRegistry().getCacheCapacity());
        }

        @Test
        @DisplayName("Should handle zero cache capacity")
        void shouldHandleZeroCacheCapacity() {
            KtestifyConfig config = ConfigBuilder.create()
                    .set("ktestify.schema-registry.cache-capacity", 0)
                    .build();

            assertEquals(0, config.getSchemaRegistry().getCacheCapacity());
        }

        @Test
        @DisplayName("Should handle different compatibility levels")
        void shouldHandleDifferentCompatibilityLevels() {
            String[] levels = {
                "BACKWARD", "FORWARD", "FULL", "NONE", "BACKWARD_TRANSITIVE", "FORWARD_TRANSITIVE", "FULL_TRANSITIVE"
            };

            for (String level : levels) {
                KtestifyConfig config = ConfigBuilder.create()
                        .set("ktestify.schema-registry.compatibility-level", level)
                        .build();

                assertEquals(level, config.getSchemaRegistry().getCompatibilityLevel());
            }
        }

        @Test
        @DisplayName("Should maintain property types in conversion")
        void shouldMaintainPropertyTypesInConversion() {
            Properties props = schemaRegistryConfig.getProperties();

            // Cache capacity should be converted to string in properties
            assertEquals("50", props.get("schema.registry.cache.capacity").toString());
            // Boolean should be converted to string
            assertEquals("true", props.get("auto.register.schemas").toString());
        }

        @Test
        @DisplayName("Should handle complex authentication scenarios")
        void shouldHandleComplexAuthenticationScenarios() {
            KtestifyConfig config = ConfigBuilder.create()
                    .schemaRegistryUrl("https://sr.prod.example.com:8443")
                    .schemaRegistryAuth("prod-user", "c0mpl3x!P@ssw0rd#2024")
                    .set("ktestify.schema-registry.ssl.truststore-location", "/etc/ssl/sr-truststore.jks")
                    .set("ktestify.schema-registry.ssl.truststore-password", "tr0st!pwd")
                    .build();

            SchemaRegistryConfig sr = config.getSchemaRegistry();
            assertEquals("https://sr.prod.example.com:8443", sr.getUrl());
            assertEquals(
                    "prod-user:c0mpl3x!P@ssw0rd#2024", sr.getBasicAuthUserInfo().get());
            assertEquals(
                    "/etc/ssl/sr-truststore.jks", sr.getSslTruststoreLocation().get());
            assertEquals("tr0st!pwd", sr.getSslTruststorePassword().get());
        }
    }
}
