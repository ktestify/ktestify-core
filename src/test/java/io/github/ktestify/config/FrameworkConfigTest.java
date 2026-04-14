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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for FrameworkConfig. Tests all getters, timeout conversions, path resolution, and edge
 * cases.
 */
@DisplayName("FrameworkConfig Tests")
class FrameworkConfigTest {

    private FrameworkConfig frameworkConfig;

    @BeforeEach
    void setUp() {
        KtestifyConfig.reset();
        KtestifyConfig config = KtestifyConfig.load();
        frameworkConfig = config.getFramework();
    }

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    // ==========================================
    // TIMEOUT PROPERTIES TESTS
    // ==========================================

    @Nested
    @DisplayName("Timeout Properties")
    class TimeoutPropertiesTests {

        @Test
        @DisplayName("Should get default read timeout")
        void shouldGetDefaultReadTimeout() {
            assertEquals(Duration.ofSeconds(10), frameworkConfig.getDefaultReadTimeout());
        }

        @Test
        @DisplayName("Should get consumer delta time")
        void shouldGetConsumerDeltaTime() {
            assertEquals(Duration.ofSeconds(20), frameworkConfig.getConsumerDeltaTime());
        }

        @Test
        @DisplayName("Should get poll interval")
        void shouldGetPollInterval() {
            assertEquals(Duration.ofMillis(100), frameworkConfig.getPollInterval());
        }

        @Test
        @DisplayName("Should get buffer time")
        void shouldGetBufferTime() {
            assertEquals(Duration.ofSeconds(5), frameworkConfig.getBufferTime());
        }

        @Test
        @DisplayName("Should convert default read timeout to milliseconds")
        void shouldConvertDefaultReadTimeoutToMilliseconds() {
            assertEquals(10000L, frameworkConfig.getDefaultReadTimeoutMillis());
        }

        @Test
        @DisplayName("Should convert consumer delta time to seconds")
        void shouldConvertConsumerDeltaTimeToSeconds() {
            assertEquals(20L, frameworkConfig.getConsumerDeltaTimeSeconds());
        }

        @Test
        @DisplayName("Should convert poll interval to milliseconds")
        void shouldConvertPollIntervalToMilliseconds() {
            assertEquals(100L, frameworkConfig.getPollIntervalMillis());
        }

        @Test
        @DisplayName("Should convert buffer time to milliseconds")
        void shouldConvertBufferTimeToMilliseconds() {
            assertEquals(5000L, frameworkConfig.getBufferTimeMillis());
        }

        @Test
        @DisplayName("Should handle custom timeout values")
        void shouldHandleCustomTimeoutValues() {
            KtestifyConfig config = ConfigBuilder.create()
                    .defaultReadTimeout(Duration.ofSeconds(30))
                    .consumerDeltaTime(Duration.ofSeconds(45))
                    .build();

            FrameworkConfig fw = config.getFramework();
            assertEquals(Duration.ofSeconds(30), fw.getDefaultReadTimeout());
            assertEquals(Duration.ofSeconds(45), fw.getConsumerDeltaTime());
            assertEquals(30000L, fw.getDefaultReadTimeoutMillis());
            assertEquals(45L, fw.getConsumerDeltaTimeSeconds());
        }

        @Test
        @DisplayName("Should handle very short timeouts")
        void shouldHandleVeryShortTimeouts() {
            KtestifyConfig config = ConfigBuilder.create()
                    .set("ktestify.framework.timeouts.poll-interval", "10ms")
                    .set("ktestify.framework.timeouts.buffer-time", "100ms")
                    .build();

            FrameworkConfig fw = config.getFramework();
            assertEquals(Duration.ofMillis(10), fw.getPollInterval());
            assertEquals(Duration.ofMillis(100), fw.getBufferTime());
        }

        @Test
        @DisplayName("Should handle very long timeouts")
        void shouldHandleVeryLongTimeouts() {
            KtestifyConfig config = ConfigBuilder.create()
                    .defaultReadTimeout(Duration.ofMinutes(5))
                    .consumerDeltaTime(Duration.ofMinutes(10))
                    .build();

            FrameworkConfig fw = config.getFramework();
            assertEquals(300000L, fw.getDefaultReadTimeoutMillis());
            assertEquals(600L, fw.getConsumerDeltaTimeSeconds());
        }

        @Test
        @DisplayName("Should handle zero timeout")
        void shouldHandleZeroTimeout() {
            KtestifyConfig config =
                    ConfigBuilder.create().defaultReadTimeout(Duration.ZERO).build();

            assertEquals(Duration.ZERO, config.getFramework().getDefaultReadTimeout());
            assertEquals(0L, config.getFramework().getDefaultReadTimeoutMillis());
        }
    }

    // ==========================================
    // DIRECTORY PROPERTIES TESTS
    // ==========================================

    @Nested
    @DisplayName("Directory Properties")
    class DirectoryPropertiesTests {

        @Test
        @DisplayName("Should return empty optional for assets directory by default")
        void shouldReturnEmptyOptionalForAssetsDirectoryByDefault() {
            assertFalse(frameworkConfig.getAssetsDirectory().isPresent());
        }

        @Test
        @DisplayName("Should return empty optional for schemas directory by default")
        void shouldReturnEmptyOptionalForSchemasDirectoryByDefault() {
            assertFalse(frameworkConfig.getSchemasDirectory().isPresent());
        }

        @Test
        @DisplayName("Should return empty optional for output directory by default")
        void shouldReturnEmptyOptionalForOutputDirectoryByDefault() {
            assertFalse(frameworkConfig.getOutputDirectory().isPresent());
        }

        @Test
        @DisplayName("Should handle configured assets directory")
        void shouldHandleConfiguredAssetsDirectory() {
            KtestifyConfig config =
                    ConfigBuilder.create().assetsDirectory("/test/assets").build();

            assertTrue(config.getFramework().getAssetsDirectory().isPresent());
            assertEquals(
                    "/test/assets", config.getFramework().getAssetsDirectory().get());
        }

        @Test
        @DisplayName("Should handle configured schemas directory")
        void shouldHandleConfiguredSchemasDirectory() {
            KtestifyConfig config =
                    ConfigBuilder.create().schemasDirectory("/test/schemas").build();

            assertTrue(config.getFramework().getSchemasDirectory().isPresent());
            assertEquals(
                    "/test/schemas", config.getFramework().getSchemasDirectory().get());
        }

        @Test
        @DisplayName("Should handle configured output directory")
        void shouldHandleConfiguredOutputDirectory() {
            KtestifyConfig config =
                    ConfigBuilder.create().outputDirectory("/test/output").build();

            assertTrue(config.getFramework().getOutputDirectory().isPresent());
            assertEquals(
                    "/test/output", config.getFramework().getOutputDirectory().get());
        }

        @Test
        @DisplayName("Should handle relative directory paths")
        void shouldHandleRelativeDirectoryPaths() {
            KtestifyConfig config = ConfigBuilder.create()
                    .assetsDirectory("./src/test/resources/assets")
                    .schemasDirectory("schemas/avro")
                    .outputDirectory("target/test-output")
                    .build();

            FrameworkConfig fw = config.getFramework();
            assertEquals("./src/test/resources/assets", fw.getAssetsDirectory().get());
            assertEquals("schemas/avro", fw.getSchemasDirectory().get());
            assertEquals("target/test-output", fw.getOutputDirectory().get());
        }

        @Test
        @DisplayName("Should handle absolute directory paths")
        void shouldHandleAbsoluteDirectoryPaths() {
            KtestifyConfig config = ConfigBuilder.create()
                    .assetsDirectory("/opt/ktestify/assets")
                    .schemasDirectory("/opt/ktestify/schemas")
                    .outputDirectory("/var/log/ktestify/output")
                    .build();

            FrameworkConfig fw = config.getFramework();
            assertEquals("/opt/ktestify/assets", fw.getAssetsDirectory().get());
            assertEquals("/opt/ktestify/schemas", fw.getSchemasDirectory().get());
            assertEquals("/var/log/ktestify/output", fw.getOutputDirectory().get());
        }
    }

    // ==========================================
    // EXECUTION PROPERTIES TESTS
    // ==========================================

    @Nested
    @DisplayName("Execution Properties")
    class ExecutionPropertiesTests {

        @Test
        @DisplayName("Should get snapshot mode")
        void shouldGetSnapshotMode() {
            assertFalse(frameworkConfig.isSnapshotMode());
        }

        @Test
        @DisplayName("Should get strict matching")
        void shouldGetStrictMatching() {
            assertTrue(frameworkConfig.isStrictMatching());
        }

        @Test
        @DisplayName("Should get max retries")
        void shouldGetMaxRetries() {
            assertEquals(3, frameworkConfig.getMaxRetries());
        }

        @Test
        @DisplayName("Should handle enabled snapshot mode")
        void shouldHandleEnabledSnapshotMode() {
            KtestifyConfig config = ConfigBuilder.create().snapshotMode(true).build();

            assertTrue(config.getFramework().isSnapshotMode());
        }

        @Test
        @DisplayName("Should handle disabled strict matching")
        void shouldHandleDisabledStrictMatching() {
            KtestifyConfig config = ConfigBuilder.create().strictMatching(false).build();

            assertFalse(config.getFramework().isStrictMatching());
        }

        @Test
        @DisplayName("Should handle custom max retries")
        void shouldHandleCustomMaxRetries() {
            KtestifyConfig config = ConfigBuilder.create().maxRetries(10).build();

            assertEquals(10, config.getFramework().getMaxRetries());
        }

        @Test
        @DisplayName("Should handle zero retries")
        void shouldHandleZeroRetries() {
            KtestifyConfig config = ConfigBuilder.create().maxRetries(0).build();

            assertEquals(0, config.getFramework().getMaxRetries());
        }

        @Test
        @DisplayName("Should handle large retry count")
        void shouldHandleLargeRetryCount() {
            KtestifyConfig config = ConfigBuilder.create().maxRetries(100).build();

            assertEquals(100, config.getFramework().getMaxRetries());
        }

        @Test
        @DisplayName("Should handle all execution settings together")
        void shouldHandleAllExecutionSettingsTogether() {
            KtestifyConfig config = ConfigBuilder.create()
                    .snapshotMode(true)
                    .strictMatching(false)
                    .maxRetries(5)
                    .build();

            FrameworkConfig fw = config.getFramework();
            assertTrue(fw.isSnapshotMode());
            assertFalse(fw.isStrictMatching());
            assertEquals(5, fw.getMaxRetries());
        }
    }

    // ==========================================
    // REPORTING PROPERTIES TESTS
    // ==========================================

    @Nested
    @DisplayName("Reporting Properties")
    class ReportingPropertiesTests {

        @Test
        @DisplayName("Should get enable reporting")
        void shouldGetEnableReporting() {
            assertTrue(frameworkConfig.isEnableReporting());
        }

        @Test
        @DisplayName("Should get report format")
        void shouldGetReportFormat() {
            assertEquals("html", frameworkConfig.getReportFormat());
        }

        @Test
        @DisplayName("Should handle disabled reporting")
        void shouldHandleDisabledReporting() {
            KtestifyConfig config = ConfigBuilder.create()
                    .set("ktestify.framework.reporting.enabled", false)
                    .build();

            assertFalse(config.getFramework().isEnableReporting());
        }

        @Test
        @DisplayName("Should handle different report formats")
        void shouldHandleDifferentReportFormats() {
            String[] formats = {"html", "json", "cucumber"};

            for (String format : formats) {
                KtestifyConfig config = ConfigBuilder.create()
                        .set("ktestify.framework.reporting.format", format)
                        .build();

                assertEquals(format, config.getFramework().getReportFormat());
            }
        }
    }

    // ==========================================
    // PATH RESOLUTION TESTS
    // ==========================================

    @Nested
    @DisplayName("Path Resolution")
    class PathResolutionTests {

        @Test
        @DisplayName("Should resolve asset path without configured directory")
        void shouldResolveAssetPathWithoutConfiguredDirectory() {
            Path resolved = frameworkConfig.resolveAssetPath("test-payload.json");
            assertEquals(Paths.get("test-payload.json"), resolved);
        }

        @Test
        @DisplayName("Should resolve asset path with configured directory")
        void shouldResolveAssetPathWithConfiguredDirectory() {
            KtestifyConfig config =
                    ConfigBuilder.create().assetsDirectory("/test/assets").build();

            Path resolved = config.getFramework().resolveAssetPath("payload.json");
            assertEquals(Paths.get("/test/assets", "payload.json"), resolved);
        }

        @Test
        @DisplayName("Should resolve schema path without configured directory")
        void shouldResolveSchemaPathWithoutConfiguredDirectory() {
            Path resolved = frameworkConfig.resolveSchemaPath("user.avsc");
            assertEquals(Paths.get("user.avsc"), resolved);
        }

        @Test
        @DisplayName("Should resolve schema path with configured directory")
        void shouldResolveSchemaPathWithConfiguredDirectory() {
            KtestifyConfig config =
                    ConfigBuilder.create().schemasDirectory("/schemas").build();

            Path resolved = config.getFramework().resolveSchemaPath("user.avsc");
            assertEquals(Paths.get("/schemas", "user.avsc"), resolved);
        }

        @Test
        @DisplayName("Should resolve output path without configured directory")
        void shouldResolveOutputPathWithoutConfiguredDirectory() {
            Path resolved = frameworkConfig.resolveOutputPath("result.json");
            assertEquals(Paths.get("result.json"), resolved);
        }

        @Test
        @DisplayName("Should resolve output path with configured directory")
        void shouldResolveOutputPathWithConfiguredDirectory() {
            KtestifyConfig config =
                    ConfigBuilder.create().outputDirectory("/output").build();

            Path resolved = config.getFramework().resolveOutputPath("result.json");
            assertEquals(Paths.get("/output", "result.json"), resolved);
        }

        @Test
        @DisplayName("Should handle nested path resolution")
        void shouldHandleNestedPathResolution() {
            KtestifyConfig config =
                    ConfigBuilder.create().assetsDirectory("/base/assets").build();

            Path resolved = config.getFramework().resolveAssetPath("nested/dir/file.json");
            assertEquals(Paths.get("/base/assets", "nested/dir/file.json"), resolved);
        }

        @Test
        @DisplayName("Should handle path with special characters")
        void shouldHandlePathWithSpecialCharacters() {
            KtestifyConfig config =
                    ConfigBuilder.create().assetsDirectory("/assets").build();

            Path resolved = config.getFramework().resolveAssetPath("test-file_v2.0.json");
            assertEquals(Paths.get("/assets", "test-file_v2.0.json"), resolved);
        }

        @Test
        @DisplayName("Should handle empty filename")
        void shouldHandleEmptyFilename() {
            KtestifyConfig config =
                    ConfigBuilder.create().assetsDirectory("/assets").build();

            Path resolved = config.getFramework().resolveAssetPath("");
            assertEquals(Paths.get("/assets", ""), resolved);
        }

        @Test
        @DisplayName("Should resolve all path types independently")
        void shouldResolveAllPathTypesIndependently() {
            KtestifyConfig config = ConfigBuilder.create()
                    .assetsDirectory("/assets")
                    .schemasDirectory("/schemas")
                    .outputDirectory("/output")
                    .build();

            FrameworkConfig fw = config.getFramework();
            assertEquals(Paths.get("/assets", "file1.json"), fw.resolveAssetPath("file1.json"));
            assertEquals(Paths.get("/schemas", "file2.avsc"), fw.resolveSchemaPath("file2.avsc"));
            assertEquals(Paths.get("/output", "file3.json"), fw.resolveOutputPath("file3.json"));
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
            assertNotNull(frameworkConfig.getRaw());
        }

        @Test
        @DisplayName("Should access nested values via raw config")
        void shouldAccessNestedValuesViaRawConfig() {
            assertTrue(frameworkConfig.getRaw().hasPath("timeouts"));
            assertTrue(frameworkConfig.getRaw().hasPath("execution"));
            assertTrue(frameworkConfig.getRaw().hasPath("directories"));
        }

        @Test
        @DisplayName("Should access timeout config via raw config")
        void shouldAccessTimeoutConfigViaRawConfig() {
            var timeoutsConfig = frameworkConfig.getRaw().getConfig("timeouts");
            assertEquals(Duration.ofSeconds(10), timeoutsConfig.getDuration("default-read-timeout"));
        }
    }

    // ==========================================
    // EDGE CASES TESTS
    // ==========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty string directory values")
        void shouldHandleEmptyStringDirectoryValues() {
            // Default config has empty strings which should result in empty optionals
            assertFalse(frameworkConfig.getAssetsDirectory().isPresent());
            assertFalse(frameworkConfig.getSchemasDirectory().isPresent());
            assertFalse(frameworkConfig.getOutputDirectory().isPresent());
        }

        @Test
        @DisplayName("Should handle all settings configured at once")
        void shouldHandleAllSettingsConfiguredAtOnce() {
            KtestifyConfig config = ConfigBuilder.create()
                    .defaultReadTimeout(Duration.ofSeconds(25))
                    .consumerDeltaTime(Duration.ofSeconds(15))
                    .assetsDirectory("/assets")
                    .schemasDirectory("/schemas")
                    .outputDirectory("/output")
                    .snapshotMode(true)
                    .strictMatching(false)
                    .maxRetries(7)
                    .build();

            FrameworkConfig fw = config.getFramework();
            assertEquals(Duration.ofSeconds(25), fw.getDefaultReadTimeout());
            assertEquals(Duration.ofSeconds(15), fw.getConsumerDeltaTime());
            assertEquals("/assets", fw.getAssetsDirectory().get());
            assertEquals("/schemas", fw.getSchemasDirectory().get());
            assertEquals("/output", fw.getOutputDirectory().get());
            assertTrue(fw.isSnapshotMode());
            assertFalse(fw.isStrictMatching());
            assertEquals(7, fw.getMaxRetries());
        }

        @Test
        @DisplayName("Should preserve millisecond precision in timeouts")
        void shouldPreserveMillisecondPrecisionInTimeouts() {
            KtestifyConfig config = ConfigBuilder.create()
                    .set("ktestify.framework.timeouts.default-read-timeout", "12345ms")
                    .build();

            assertEquals(12345L, config.getFramework().getDefaultReadTimeoutMillis());
        }

        @Test
        @DisplayName("Should handle timeout conversions correctly")
        void shouldHandleTimeoutConversionsCorrectly() {
            KtestifyConfig config = ConfigBuilder.create()
                    .defaultReadTimeout(Duration.ofMinutes(2))
                    .consumerDeltaTime(Duration.ofMinutes(3))
                    .build();

            FrameworkConfig fw = config.getFramework();
            assertEquals(120000L, fw.getDefaultReadTimeoutMillis());
            assertEquals(180L, fw.getConsumerDeltaTimeSeconds());
        }

        @Test
        @DisplayName("Should handle negative retry values")
        void shouldHandleNegativeRetryValues() {
            KtestifyConfig config = ConfigBuilder.create().maxRetries(-1).build();

            assertEquals(-1, config.getFramework().getMaxRetries());
        }

        @Test
        @DisplayName("Should maintain boolean flags correctly")
        void shouldMaintainBooleanFlagsCorrectly() {
            KtestifyConfig config1 = ConfigBuilder.create()
                    .snapshotMode(true)
                    .strictMatching(true)
                    .build();

            KtestifyConfig config2 = ConfigBuilder.create()
                    .snapshotMode(false)
                    .strictMatching(false)
                    .build();

            assertTrue(config1.getFramework().isSnapshotMode());
            assertTrue(config1.getFramework().isStrictMatching());
            assertFalse(config2.getFramework().isSnapshotMode());
            assertFalse(config2.getFramework().isStrictMatching());
        }

        @Test
        @DisplayName("Should handle Windows-style paths")
        void shouldHandleWindowsStylePaths() {
            KtestifyConfig config = ConfigBuilder.create()
                    .assetsDirectory("C:\\Users\\test\\assets")
                    .schemasDirectory("D:\\schemas")
                    .build();

            FrameworkConfig fw = config.getFramework();
            assertEquals("C:\\Users\\test\\assets", fw.getAssetsDirectory().get());
            assertEquals("D:\\schemas", fw.getSchemasDirectory().get());
        }

        @Test
        @DisplayName("Should handle Unix-style paths")
        void shouldHandleUnixStylePaths() {
            KtestifyConfig config = ConfigBuilder.create()
                    .assetsDirectory("/home/user/ktestify/assets")
                    .outputDirectory("/var/log/ktestify/output")
                    .build();

            FrameworkConfig fw = config.getFramework();
            assertEquals("/home/user/ktestify/assets", fw.getAssetsDirectory().get());
            assertEquals("/var/log/ktestify/output", fw.getOutputDirectory().get());
        }

        @Test
        @DisplayName("Should handle paths with dots")
        void shouldHandlePathsWithDots() {
            KtestifyConfig config = ConfigBuilder.create()
                    .assetsDirectory("../../test-assets")
                    .schemasDirectory("./schemas/v1.0")
                    .build();

            FrameworkConfig fw = config.getFramework();
            assertEquals("../../test-assets", fw.getAssetsDirectory().get());
            assertEquals("./schemas/v1.0", fw.getSchemasDirectory().get());
        }
    }
}
