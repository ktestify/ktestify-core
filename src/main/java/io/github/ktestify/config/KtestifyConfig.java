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
import java.io.File;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * Main configuration class for ktestify framework.
 *
 * <p>Loads configuration from HOCON files with the following priority (highest to lowest):
 *
 * <ol>
 *   <li>System properties
 *   <li>Environment variables
 *   <li>application.conf (user-provided)
 *   <li>reference.conf (library defaults)
 * </ol>
 *
 * <p>Usage:
 *
 * <pre>
 * KtestifyConfig config = KtestifyConfig.load();
 * KafkaConfig kafka = config.getKafka();
 * String bootstrapServers = kafka.getBootstrapServers();
 * </pre>
 *
 * @since 0.2.51
 */
@Slf4j
@Getter
public final class KtestifyConfig {

    private static volatile KtestifyConfig instance;
    private static final Object LOCK = new Object();

    private final Config rawConfig;
    private final KafkaConfig kafka;
    private final SchemaRegistryConfig schemaRegistry;
    private final FrameworkConfig framework;

    private KtestifyConfig(Config config) {
        this.rawConfig = config;
        this.kafka = new KafkaConfig(config.getConfig("ktestify.kafka"));
        this.schemaRegistry = new SchemaRegistryConfig(config.getConfig("ktestify.schema-registry"));
        this.framework = new FrameworkConfig(config.getConfig("ktestify.framework"));
        applyLogLevels(config);
        applyJvmTruststore(config);
        log.info("KtestifyConfig loaded successfully");
        log.debug("Kafka bootstrap servers: {}", kafka.getBootstrapServers());
        log.debug("Schema Registry URL: {}", schemaRegistry.getUrl());
    }

    /**
     * Applies log levels defined in {@code ktestify.logging} HOCON section via Log4j2's {@link Configurator},
     * overriding the static defaults set in {@code log4j2.properties}.
     *
     * <p>Supported keys and their environment variable overrides:
     *
     * <ul>
     *   <li>{@code level} / {@code KTESTIFY_LOG_LEVEL} — {@code io.github.ktestify.*}
     *   <li>{@code root-level} / {@code KTESTIFY_ROOT_LOG_LEVEL} — root logger
     *   <li>{@code kafka-level} / {@code KTESTIFY_KAFKA_LOG_LEVEL} — {@code org.apache.kafka.*}
     *   <li>{@code testcontainers-level} / {@code KTESTIFY_TC_LOG_LEVEL} — Testcontainers + Docker Java
     *   <li>{@code confluent-level} / {@code KTESTIFY_CONFLUENT_LOG_LEVEL} — {@code io.confluent.*}
     * </ul>
     */
    private static void applyLogLevels(Config config) {
        Config lc = config.getConfig("ktestify.logging");

        Configurator.setLevel("io.github.ktestify", Level.toLevel(lc.getString("level"), Level.DEBUG));
        Configurator.setRootLevel(Level.toLevel(lc.getString("root-level"), Level.INFO));
        Configurator.setLevel("org.apache.kafka", Level.toLevel(lc.getString("kafka-level"), Level.WARN));

        Level tcLevel = Level.toLevel(lc.getString("testcontainers-level"), Level.INFO);
        Configurator.setLevel("org.testcontainers", tcLevel);
        Configurator.setLevel("tc", tcLevel);
        Configurator.setLevel("com.github.dockerjava", tcLevel);

        Configurator.setLevel("io.confluent", Level.toLevel(lc.getString("confluent-level"), Level.WARN));

        log.debug(
                "Log levels applied — ktestify={} root={} kafka={} confluent={}",
                lc.getString("level"),
                lc.getString("root-level"),
                lc.getString("kafka-level"),
                lc.getString("confluent-level"));
    }

    /**
     * Applies the JVM-level truststore defined in {@code ktestify.jvm.truststore} HOCON section by setting
     * the corresponding {@code javax.net.ssl.*} system properties. This truststore is shared by all plugins
     * and underlying libraries that don't provide their own SSL configuration.
     *
     * <p>If {@code location} is blank, no system properties are set and the JVM's built-in cacerts are used.
     *
     * <p>Supported keys and their environment variable overrides:
     *
     * <ul>
     *   <li>{@code location} / {@code KTESTIFY_JVM_TRUSTSTORE_LOCATION}
     *   <li>{@code password} / {@code KTESTIFY_JVM_TRUSTSTORE_PASSWORD}
     *   <li>{@code type} / {@code KTESTIFY_JVM_TRUSTSTORE_TYPE} — {@code JKS} or {@code PKCS12}
     * </ul>
     *
     * @param config the resolved Config object containing {@code ktestify.jvm.truststore}
     */
    public static void applyJvmTruststore(Config config) {
        String location = config.getString("ktestify.jvm.truststore.location");
        if (location == null || location.isBlank()) {
            log.debug("No JVM truststore location configured; keeping JVM default cacerts");
            return;
        }

        System.setProperty("javax.net.ssl.trustStore", location);
        System.setProperty(
                "javax.net.ssl.trustStoreType", config.getString("ktestify.jvm.truststore.type"));

        String password = config.getString("ktestify.jvm.truststore.password");
        if (password != null && !password.isBlank()) {
            System.setProperty("javax.net.ssl.trustStorePassword", password);
        }

        log.info(
                "JVM truststore applied — location={} type={}",
                location,
                config.getString("ktestify.jvm.truststore.type"));
    }

    /**
     * Loads the default configuration using the standard HOCON loading mechanism. Configuration is loaded from:
     *
     * <ul>
     *   <li>System properties
     *   <li>application.conf from classpath
     *   <li>reference.conf from classpath (defaults)
     * </ul>
     *
     * @return the loaded configuration instance
     */
    public static KtestifyConfig load() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    ConfigFactory.invalidateCaches();
                    Config config = ConfigFactory.load();
                    instance = new KtestifyConfig(config);
                }
            }
        }
        return instance;
    }

    /**
     * Loads configuration from a specific file path. The file configuration is merged with defaults from
     * reference.conf.
     *
     * @param configFilePath path to the configuration file
     * @return the loaded configuration instance
     */
    public static KtestifyConfig load(String configFilePath) {
        synchronized (LOCK) {
            Config fileConfig = ConfigFactory.parseFile(new File(configFilePath));
            Config config = fileConfig.withFallback(ConfigFactory.load()).resolve();
            instance = new KtestifyConfig(config);
            return instance;
        }
    }

    /**
     * Loads configuration from an existing Config object. Useful for programmatic configuration or testing.
     *
     * @param config the Config object to use
     * @return the loaded configuration instance
     */
    public static KtestifyConfig load(Config config) {
        synchronized (LOCK) {
            Config resolvedConfig = config.withFallback(ConfigFactory.load()).resolve();
            instance = new KtestifyConfig(resolvedConfig);
            return instance;
        }
    }

    /**
     * Returns the current configuration instance.
     *
     * @return the current configuration instance
     * @throws IllegalStateException if configuration has not been loaded
     */
    public static KtestifyConfig getInstance() {
        if (instance == null) {
            throw new IllegalStateException("KtestifyConfig has not been loaded. Call load() first.");
        }
        return instance;
    }

    /**
     * Returns the current configuration instance if loaded, otherwise loads the default configuration.
     *
     * @return the configuration instance
     */
    public static KtestifyConfig getOrLoad() {
        if (instance == null) {
            return load();
        }
        return instance;
    }

    /** Resets the configuration instance. Useful for testing. */
    public static void reset() {
        synchronized (LOCK) {
            instance = null;
            log.debug("KtestifyConfig instance has been reset");
        }
    }

    /**
     * Checks if the configuration has been loaded.
     *
     * @return true if configuration is loaded, false otherwise
     */
    public static boolean isLoaded() {
        return instance != null;
    }

    /**
     * Gets a configuration value by path.
     *
     * @param path the configuration path
     * @return Optional containing the value if present
     */
    public Optional<String> getString(String path) {
        if (rawConfig.hasPath(path)) {
            return Optional.of(rawConfig.getString(path));
        }
        return Optional.empty();
    }

    /**
     * Gets a configuration value by path with a default value.
     *
     * @param path the configuration path
     * @param defaultValue the default value if path is not found
     * @return the configuration value or default
     */
    public String getString(String path, String defaultValue) {
        return getString(path).orElse(defaultValue);
    }

    /**
     * Gets the raw underlying Config object for advanced usage.
     *
     * @return the raw Config object
     */
    public Config getRaw() {
        return rawConfig;
    }
}
