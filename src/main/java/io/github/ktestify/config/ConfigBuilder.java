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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for creating KtestifyConfig programmatically.
 *
 * <p>Useful for testing or when configuration needs to be created at runtime. All settings have sensible defaults from
 * reference.conf.
 *
 * <p>Usage:
 *
 * <pre>
 * KtestifyConfig config = ConfigBuilder.create()
 *     .bootstrapServers("kafka:9092")
 *     .schemaRegistryUrl("http://schema-registry:8081")
 *     .defaultReadTimeout(Duration.ofSeconds(30))
 *     .build();
 * </pre>
 *
 * @since 0.2.51
 */
public final class ConfigBuilder {

    private final Map<String, Object> overrides = new HashMap<>();

    private ConfigBuilder() {}

    /**
     * Creates a new ConfigBuilder instance.
     *
     * @return a new ConfigBuilder
     */
    public static ConfigBuilder create() {
        return new ConfigBuilder();
    }

    // ==========================================
    // Kafka Common Settings
    // ==========================================

    /**
     * Sets the Kafka bootstrap servers.
     *
     * @param bootstrapServers comma-separated list of host:port
     * @return this builder
     */
    public ConfigBuilder bootstrapServers(String bootstrapServers) {
        overrides.put("ktestify.kafka.bootstrap-servers", bootstrapServers);
        return this;
    }

    /**
     * Sets the Kafka security protocol.
     *
     * @param securityProtocol PLAINTEXT, SSL, SASL_PLAINTEXT, or SASL_SSL
     * @return this builder
     */
    public ConfigBuilder securityProtocol(String securityProtocol) {
        overrides.put("ktestify.kafka.security-protocol", securityProtocol);
        return this;
    }

    // ==========================================
    // Kafka Security Settings
    // ==========================================

    /**
     * Sets the SASL mechanism.
     *
     * @param saslMechanism PLAIN, SCRAM-SHA-256, SCRAM-SHA-512, or OAUTHBEARER
     * @return this builder
     */
    public ConfigBuilder saslMechanism(String saslMechanism) {
        overrides.put("ktestify.kafka.security.sasl-mechanism", saslMechanism);
        return this;
    }

    /**
     * Sets the SASL JAAS configuration.
     *
     * @param jaasConfig JAAS configuration string
     * @return this builder
     */
    public ConfigBuilder saslJaasConfig(String jaasConfig) {
        overrides.put("ktestify.kafka.security.sasl-jaas-config", jaasConfig);
        return this;
    }

    /**
     * Sets the SSL truststore location and password.
     *
     * @param location truststore file path
     * @param password truststore password
     * @return this builder
     */
    public ConfigBuilder sslTruststore(String location, String password) {
        overrides.put("ktestify.kafka.security.ssl-truststore-location", location);
        overrides.put("ktestify.kafka.security.ssl-truststore-password", password);
        return this;
    }

    /**
     * Sets the SSL keystore location, password, and key password.
     *
     * @param location keystore file path
     * @param password keystore password
     * @param keyPassword key password
     * @return this builder
     */
    public ConfigBuilder sslKeystore(String location, String password, String keyPassword) {
        overrides.put("ktestify.kafka.security.ssl-keystore-location", location);
        overrides.put("ktestify.kafka.security.ssl-keystore-password", password);
        overrides.put("ktestify.kafka.security.ssl-key-password", keyPassword);
        return this;
    }

    // ==========================================
    // Kafka Producer Settings
    // ==========================================

    /**
     * Sets the producer acks setting.
     *
     * @param acks 0, 1, or all
     * @return this builder
     */
    public ConfigBuilder producerAcks(String acks) {
        overrides.put("ktestify.kafka.producer.acks", acks);
        return this;
    }

    /**
     * Sets the producer retries.
     *
     * @param retries number of retries
     * @return this builder
     */
    public ConfigBuilder producerRetries(int retries) {
        overrides.put("ktestify.kafka.producer.retries", retries);
        return this;
    }

    // ==========================================
    // Kafka Consumer Settings
    // ==========================================

    /**
     * Sets the consumer group ID.
     *
     * @param groupId consumer group ID
     * @return this builder
     */
    public ConfigBuilder consumerGroupId(String groupId) {
        overrides.put("ktestify.kafka.consumer.group-id", groupId);
        return this;
    }

    /**
     * Sets the consumer auto offset reset strategy.
     *
     * @param autoOffsetReset earliest, latest, or none
     * @return this builder
     */
    public ConfigBuilder autoOffsetReset(String autoOffsetReset) {
        overrides.put("ktestify.kafka.consumer.auto-offset-reset", autoOffsetReset);
        return this;
    }

    // ==========================================
    // Schema Registry Settings
    // ==========================================

    /**
     * Sets the Schema Registry URL.
     *
     * @param url Schema Registry URL
     * @return this builder
     */
    public ConfigBuilder schemaRegistryUrl(String url) {
        overrides.put("ktestify.schema-registry.url", url);
        return this;
    }

    /**
     * Sets Schema Registry basic authentication.
     *
     * @param username username
     * @param password password
     * @return this builder
     */
    public ConfigBuilder schemaRegistryAuth(String username, String password) {
        overrides.put("ktestify.schema-registry.auth.basic-auth-credentials-source", "USER_INFO");
        overrides.put("ktestify.schema-registry.auth.basic-auth-user-info", username + ":" + password);
        return this;
    }

    /**
     * Sets whether to auto-register schemas.
     *
     * @param autoRegister true to auto-register schemas
     * @return this builder
     */
    public ConfigBuilder autoRegisterSchemas(boolean autoRegister) {
        overrides.put("ktestify.schema-registry.auto-register-schemas", autoRegister);
        return this;
    }

    // ==========================================
    // Framework Settings
    // ==========================================

    /**
     * Sets the default read timeout.
     *
     * @param timeout read timeout duration
     * @return this builder
     */
    public ConfigBuilder defaultReadTimeout(Duration timeout) {
        overrides.put("ktestify.framework.timeouts.default-read-timeout", timeout.toMillis() + "ms");
        return this;
    }

    /**
     * Sets the consumer delta time.
     *
     * @param deltaTime delta time duration
     * @return this builder
     */
    public ConfigBuilder consumerDeltaTime(Duration deltaTime) {
        overrides.put("ktestify.framework.timeouts.consumer-delta-time", deltaTime.toMillis() + "ms");
        return this;
    }

    /**
     * Sets the assets directory.
     *
     * @param directory path to assets directory
     * @return this builder
     */
    public ConfigBuilder assetsDirectory(String directory) {
        overrides.put("ktestify.framework.directories.assets", directory);
        return this;
    }

    /**
     * Sets the schemas directory.
     *
     * @param directory path to schemas directory
     * @return this builder
     */
    public ConfigBuilder schemasDirectory(String directory) {
        overrides.put("ktestify.framework.directories.schemas", directory);
        return this;
    }

    /**
     * Sets the output directory.
     *
     * @param directory path to output directory
     * @return this builder
     */
    public ConfigBuilder outputDirectory(String directory) {
        overrides.put("ktestify.framework.directories.output", directory);
        return this;
    }

    /**
     * Enables or disables snapshot mode.
     *
     * @param enabled true to enable snapshot mode
     * @return this builder
     */
    public ConfigBuilder snapshotMode(boolean enabled) {
        overrides.put("ktestify.framework.execution.snapshot-mode", enabled);
        return this;
    }

    /**
     * Enables or disables strict matching.
     *
     * @param enabled true for strict matching
     * @return this builder
     */
    public ConfigBuilder strictMatching(boolean enabled) {
        overrides.put("ktestify.framework.execution.strict-matching", enabled);
        return this;
    }

    /**
     * Sets the maximum number of retries.
     *
     * @param maxRetries maximum retries
     * @return this builder
     */
    public ConfigBuilder maxRetries(int maxRetries) {
        overrides.put("ktestify.framework.execution.max-retries", maxRetries);
        return this;
    }

    // ==========================================
    // Build Methods
    // ==========================================

    /**
     * Sets a custom configuration value.
     *
     * @param path the configuration path
     * @param value the configuration value
     * @return this builder
     */
    public ConfigBuilder set(String path, Object value) {
        overrides.put(path, value);
        return this;
    }

    /**
     * Builds the KtestifyConfig from the configured values. Values not explicitly set will use defaults from
     * reference.conf.
     *
     * @return the built KtestifyConfig
     */
    public KtestifyConfig build() {
        Config baseConfig = ConfigFactory.load();
        Config overrideConfig = ConfigFactory.empty();

        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            overrideConfig = overrideConfig.withValue(entry.getKey(), ConfigValueFactory.fromAnyRef(entry.getValue()));
        }

        Config finalConfig = overrideConfig.withFallback(baseConfig).resolve();
        return KtestifyConfig.load(finalConfig);
    }

    /**
     * Creates a Config object without loading it into the singleton. Useful for testing or creating multiple
     * configurations.
     *
     * @return the built Config object
     */
    public Config buildConfig() {
        Config baseConfig = ConfigFactory.load();
        Config overrideConfig = ConfigFactory.empty();

        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            overrideConfig = overrideConfig.withValue(entry.getKey(), ConfigValueFactory.fromAnyRef(entry.getValue()));
        }

        return overrideConfig.withFallback(baseConfig).resolve();
    }
}
