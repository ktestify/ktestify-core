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
package io.github.ktestify.models;

import static org.junit.jupiter.api.Assertions.*;

import com.typesafe.config.ConfigFactory;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.exceptions.ConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Topic#validateTopic(Topic)} — focusing on the namespace auto-injection logic:
 *
 * <pre>
 * if (topic.getTopicNamespace() == null || blank)
 *    AND KtestifyConfig.kafka.topic-namespace is present
 * → inject the config namespace
 * </pre>
 */
@DisplayName("Topic.validateTopic — namespace auto-injection")
class TopicTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Loads a KtestifyConfig with a non-blank topic-namespace value. */
    private void loadConfigWithNamespace(String namespace) {
        KtestifyConfig.load(ConfigFactory.parseString("ktestify.kafka.topic-namespace = \"" + namespace + "\"")
                .withFallback(ConfigFactory.load())
                .resolve());
    }

    /** Loads a KtestifyConfig with NO topic-namespace (empty string → Optional.empty). */
    private void loadConfigWithoutNamespace() {
        KtestifyConfig.load(ConfigFactory.parseString("ktestify.kafka.topic-namespace = \"\"")
                .withFallback(ConfigFactory.load())
                .resolve());
    }

    /** Minimal valid INPUT topic — no namespace attached. */
    private Topic inputTopicWithoutNamespace(String name) {
        return Topic.builder().topicName(name).topicType(Topic.Type.INPUT).build();
    }

    // -----------------------------------------------------------------------
    // Setup / teardown
    // -----------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        KtestifyConfig.reset();
    }

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("When config has a topic-namespace")
    class WithConfigNamespace {

        @Test
        @DisplayName("topic with NULL namespace → namespace is injected from config")
        void nullNamespace_isInjectedFromConfig() {
            loadConfigWithNamespace("my-ns");
            Topic topic = inputTopicWithoutNamespace("orders-in");

            Topic result = Topic.validateTopic(topic, Topic.Type.INPUT);

            assertNotNull(result.getTopicNamespace(), "Namespace should have been injected");
            assertEquals("my-ns", result.getTopicNamespace().getNamespace());
            assertEquals("my-ns.orders-in", result.getNamespacedTopic());
        }

        @Test
        @DisplayName("topic with BLANK namespace string → namespace is injected from config")
        void blankNamespaceString_isInjectedFromConfig() {
            loadConfigWithNamespace("prod-ns");
            Topic topic = inputTopicWithoutNamespace("orders-in");
            // Attach an explicit but blank namespace
            topic.setTopicNamespace(
                    Topic.TopicNamespace.builder().namespace("   ").build());

            Topic result = Topic.validateTopic(topic, Topic.Type.INPUT);

            assertEquals("prod-ns", result.getTopicNamespace().getNamespace());
            assertEquals("prod-ns.orders-in", result.getNamespacedTopic());
        }

        @Test
        @DisplayName("topic with EMPTY namespace string → namespace is injected from config")
        void emptyNamespaceString_isInjectedFromConfig() {
            loadConfigWithNamespace("ci-ns");
            Topic topic = inputTopicWithoutNamespace("payments-in");
            topic.setTopicNamespace(Topic.TopicNamespace.builder().namespace("").build());

            Topic result = Topic.validateTopic(topic, Topic.Type.INPUT);

            assertEquals("ci-ns", result.getTopicNamespace().getNamespace());
        }

        @Test
        @DisplayName("topic that ALREADY has a namespace → config namespace is NOT applied")
        void existingNamespace_isNotOverridden() {
            loadConfigWithNamespace("config-ns");
            Topic topic = inputTopicWithoutNamespace("orders-in");
            topic.setTopicNamespace(
                    Topic.TopicNamespace.builder().namespace("explicit-ns").build());

            Topic result = Topic.validateTopic(topic, Topic.Type.INPUT);

            assertEquals(
                    "explicit-ns",
                    result.getTopicNamespace().getNamespace(),
                    "Explicitly set namespace must not be overridden by config");
            assertEquals("explicit-ns.orders-in", result.getNamespacedTopic());
        }
    }

    @Nested
    @DisplayName("When config has NO topic-namespace")
    class WithoutConfigNamespace {

        @Test
        @DisplayName("topic with null namespace → stays null (no namespace on getNamespacedTopic)")
        void nullNamespace_staysNull() {
            loadConfigWithoutNamespace();
            Topic topic = inputTopicWithoutNamespace("orders-in");

            Topic result = Topic.validateTopic(topic, Topic.Type.INPUT);

            assertNull(result.getTopicNamespace(), "Namespace should remain null");
            assertEquals("orders-in", result.getNamespacedTopic());
        }

        @Test
        @DisplayName("topic with blank namespace → stays blank (config has nothing to inject)")
        void blankNamespace_staysUnchanged() {
            loadConfigWithoutNamespace();
            Topic topic = inputTopicWithoutNamespace("orders-in");
            topic.setTopicNamespace(
                    Topic.TopicNamespace.builder().namespace("  ").build());

            // Config has no namespace → the blank one is NOT replaced
            Topic result = Topic.validateTopic(topic, Topic.Type.INPUT);

            assertEquals("  ", result.getTopicNamespace().getNamespace());
        }
    }

    @Nested
    @DisplayName("validateTopic — general guard-rail assertions")
    class GeneralValidation {

        @BeforeEach
        void loadDefault() {
            loadConfigWithoutNamespace();
        }

        @Test
        @DisplayName("null topic → NullPointerException")
        void nullTopic_throwsNPE() {
            assertThrows(NullPointerException.class, () -> Topic.validateTopic(null, Topic.Type.INPUT));
        }

        @Test
        @DisplayName("topic with null name → ProducerException")
        void nullTopicName_throwsProducerException() {
            Topic topic = Topic.builder().topicType(Topic.Type.INPUT).build();
            assertThrows(ConfigException.class, () -> Topic.validateTopic(topic, Topic.Type.INPUT));
        }

        @Test
        @DisplayName("topic with empty name → ProducerException")
        void emptyTopicName_throwsProducerException() {
            Topic topic =
                    Topic.builder().topicName("").topicType(Topic.Type.INPUT).build();
            assertThrows(ConfigException.class, () -> Topic.validateTopic(topic, Topic.Type.INPUT));
        }

        @Test
        @DisplayName("OUTPUT topic → ProducerException (cannot produce to output)")
        void outputTopic_throwsProducerException() {
            Topic topic = Topic.builder()
                    .topicName("orders-out")
                    .topicType(Topic.Type.OUTPUT)
                    .build();
            assertThrows(ConfigException.class, () -> Topic.validateTopic(topic, Topic.Type.INPUT));
        }
    }
}
