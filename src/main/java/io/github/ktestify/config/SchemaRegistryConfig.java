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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import lombok.Getter;

/**
 * Schema Registry configuration.
 *
 * <p>Provides access to Confluent Schema Registry settings including URL, authentication, and caching configuration.
 *
 * @since 0.2.51
 */
@Getter
public final class SchemaRegistryConfig {

    private final Config config;

    private final String url;
    private final int cacheCapacity;
    private final boolean autoRegisterSchemas;
    private final String compatibilityLevel;

    // Authentication (optional)
    private final Optional<String> basicAuthCredentialsSource;
    private final Optional<String> basicAuthUserInfo;

    // SSL (optional)
    private final Optional<String> sslTruststoreLocation;
    private final Optional<String> sslTruststorePassword;
    private final Optional<String> sslKeystoreLocation;
    private final Optional<String> sslKeystorePassword;

    SchemaRegistryConfig(Config config) {
        this.config = config;

        this.url = config.getString("url");
        this.cacheCapacity = config.getInt("cache-capacity");
        this.autoRegisterSchemas = config.getBoolean("auto-register-schemas");
        this.compatibilityLevel = config.getString("compatibility-level");

        // Authentication
        Config authConfig = config.getConfig("auth");
        this.basicAuthCredentialsSource = getOptionalString(authConfig, "basic-auth-credentials-source");
        this.basicAuthUserInfo = getOptionalString(authConfig, "basic-auth-user-info");

        // SSL
        Config sslConfig = config.getConfig("ssl");
        this.sslTruststoreLocation = getOptionalString(sslConfig, "truststore-location");
        this.sslTruststorePassword = getOptionalString(sslConfig, "truststore-password");
        this.sslKeystoreLocation = getOptionalString(sslConfig, "keystore-location");
        this.sslKeystorePassword = getOptionalString(sslConfig, "keystore-password");
    }

    private Optional<String> getOptionalString(Config config, String path) {
        if (config.hasPath(path) && !config.getIsNull(path)) {
            String value = config.getString(path);
            return value.isEmpty() ? Optional.empty() : Optional.of(value);
        }
        return Optional.empty();
    }

    /**
     * Creates a Properties object with Schema Registry settings. Compatible with Confluent's
     * KafkaAvroSerializer/Deserializer.
     *
     * @return Properties for Schema Registry client
     */
    public Properties getProperties() {
        Properties props = new Properties();
        props.put("schema.registry.url", url);
        props.put("auto.register.schemas", autoRegisterSchemas);
        props.put("schema.registry.cache.capacity", cacheCapacity);

        // Authentication
        basicAuthCredentialsSource.ifPresent(v -> props.put("basic.auth.credentials.source", v));
        basicAuthUserInfo.ifPresent(v -> props.put("basic.auth.user.info", v));

        // SSL
        sslTruststoreLocation.ifPresent(v -> props.put("schema.registry.ssl.truststore.location", v));
        sslTruststorePassword.ifPresent(v -> props.put("schema.registry.ssl.truststore.password", v));
        sslKeystoreLocation.ifPresent(v -> props.put("schema.registry.ssl.keystore.location", v));
        sslKeystorePassword.ifPresent(v -> props.put("schema.registry.ssl.keystore.password", v));

        return props;
    }

    /**
     * Converts Schema Registry properties to a Map for use with builders.
     *
     * @return Map of Schema Registry properties
     */
    public Map<String, String> getPropertiesAsMap() {
        Properties props = getProperties();
        Map<String, String> map = new HashMap<>();
        props.forEach((k, v) -> map.put(k.toString(), v.toString()));
        return map;
    }

    /**
     * Creates combined properties for Avro producer/consumer. Merges Kafka and Schema Registry properties.
     *
     * @param kafkaProps base Kafka properties
     * @return combined Properties for Avro serialization
     */
    public Properties mergeWithKafkaProperties(Properties kafkaProps) {
        Properties combined = new Properties();
        combined.putAll(kafkaProps);
        combined.putAll(getProperties());
        return combined;
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
