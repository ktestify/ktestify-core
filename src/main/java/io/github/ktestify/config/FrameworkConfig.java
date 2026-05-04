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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import lombok.Getter;

/**
 * Framework-specific configuration.
 *
 * <p>Provides access to ktestify framework settings such as timeouts, polling intervals, asset directories, and test
 * execution parameters.
 *
 * @since 0.2.51
 */
@Getter
public final class FrameworkConfig {

    private final Config config;

    // Timeouts
    private final Duration defaultReadTimeout;
    private final Duration consumerDeltaTime;
    private final Duration pollInterval;
    private final Duration bufferTime;

    // Directories
    private final Optional<String> assetsDirectory;
    private final Optional<String> schemasDirectory;
    private final Optional<String> outputDirectory;

    // Execution settings
    private final boolean snapshotMode;
    private final boolean strictMatching;
    private final int maxRetries;

    // Reporting
    private final boolean enableReporting;
    private final String reportFormat;
    private final String reportOutputDirectory;

    FrameworkConfig(Config config) {
        this.config = config;

        // Timeouts
        Config timeoutsConfig = config.getConfig("timeouts");
        this.defaultReadTimeout = timeoutsConfig.getDuration("default-read-timeout");
        this.consumerDeltaTime = timeoutsConfig.getDuration("consumer-delta-time");
        this.pollInterval = timeoutsConfig.getDuration("poll-interval");
        this.bufferTime = timeoutsConfig.getDuration("buffer-time");

        // Directories
        Config dirConfig = config.getConfig("directories");
        this.assetsDirectory = getOptionalString(dirConfig, "assets");
        this.schemasDirectory = getOptionalString(dirConfig, "schemas");
        this.outputDirectory = getOptionalString(dirConfig, "output");

        // Execution
        Config execConfig = config.getConfig("execution");
        this.snapshotMode = execConfig.getBoolean("snapshot-mode");
        this.strictMatching = execConfig.getBoolean("strict-matching");
        this.maxRetries = execConfig.getInt("max-retries");

        // Reporting
        Config reportConfig = config.getConfig("reporting");
        this.enableReporting = reportConfig.getBoolean("enabled");
        this.reportFormat = reportConfig.getString("format");
        this.reportOutputDirectory = reportConfig.getString("output");
    }

    private Optional<String> getOptionalString(Config config, String path) {
        if (config.hasPath(path) && !config.getIsNull(path)) {
            String value = config.getString(path);
            return value.isEmpty() ? Optional.empty() : Optional.of(value);
        }
        return Optional.empty();
    }

    /**
     * Gets the default read timeout in milliseconds.
     *
     * @return timeout in milliseconds
     */
    public long getDefaultReadTimeoutMillis() {
        return defaultReadTimeout.toMillis();
    }

    /**
     * Gets the consumer delta time in seconds.
     *
     * @return delta time in seconds
     */
    public long getConsumerDeltaTimeSeconds() {
        return consumerDeltaTime.toSeconds();
    }

    /**
     * Gets the poll interval in milliseconds.
     *
     * @return poll interval in milliseconds
     */
    public long getPollIntervalMillis() {
        return pollInterval.toMillis();
    }

    /**
     * Gets the buffer time in milliseconds.
     *
     * @return buffer time in milliseconds
     */
    public long getBufferTimeMillis() {
        return bufferTime.toMillis();
    }

    /**
     * Resolves a file path relative to the assets directory.
     *
     * @param filename the filename to resolve
     * @return the resolved Path, or just the filename if no assets directory is configured
     */
    public Path resolveAssetPath(String filename) {
        return assetsDirectory.map(dir -> Paths.get(dir, filename)).orElse(Paths.get(filename));
    }

    /**
     * Resolves a schema file path relative to the schemas directory.
     *
     * @param schemaName the schema filename to resolve
     * @return the resolved Path, or just the schema name if no schemas directory is configured
     */
    public Path resolveSchemaPath(String schemaName) {
        return schemasDirectory.map(dir -> Paths.get(dir, schemaName)).orElse(Paths.get(schemaName));
    }

    /**
     * Resolves an output file path relative to the output directory.
     *
     * @param filename the filename to resolve
     * @return the resolved Path, or just the filename if no output directory is configured
     */
    public Path resolveOutputPath(String filename) {
        return outputDirectory.map(dir -> Paths.get(dir, filename)).orElse(Paths.get(filename));
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
