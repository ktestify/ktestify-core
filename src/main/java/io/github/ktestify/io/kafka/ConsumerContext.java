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
package io.github.ktestify.io.kafka;

import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.models.Topic;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.kafka.clients.consumer.Consumer;

@Getter
public final class ConsumerContext<K, V> {

    private final Topic topic;
    private final Map<String, String> properties;
    private final Consumer<K, V> consumer;
    private final String expectedRecordKey;
    private final String matchMethod;
    private final List<String> matchFilePaths;
    private final List<String> excludedFields;
    private final Long readTimeout;
    private final Long consumerDeltaTime;
    private final boolean isBatchConsumer;
    private final int batchSize;

    private ConsumerContext(
            Topic topic,
            Map<String, String> properties,
            Consumer<K, V> consumer,
            String expectedRecordKey,
            String matchMethod,
            List<String> matchFilePaths,
            List<String> excludedFields,
            Long readTimeout,
            Long consumerDeltaTime,
            boolean isBatchConsumer,
            int batchSize) {
        this.topic = topic;
        this.properties = properties;
        this.consumer = consumer;
        this.expectedRecordKey = expectedRecordKey;
        this.matchMethod = matchMethod;
        this.matchFilePaths = matchFilePaths != null ? matchFilePaths : Collections.emptyList();
        this.excludedFields = excludedFields != null ? excludedFields : Collections.emptyList();
        this.readTimeout = readTimeout;
        this.consumerDeltaTime = consumerDeltaTime;
        this.isBatchConsumer = isBatchConsumer;
        this.batchSize = batchSize;
    }

    /**
     * Convenience accessor for single-record consumers. Returns the first element of {@link #matchFilePaths}, or
     * {@code null} if the list is empty.
     */
    public String getMatchFilePath() {
        return matchFilePaths != null && !matchFilePaths.isEmpty() ? matchFilePaths.get(0) : null;
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    public static final class Builder<K, V> {

        private Topic topic;
        private Map<String, String> properties;
        private Consumer<K, V> consumer;
        private String expectedRecordKey;
        private String matchMethod;
        private List<String> matchFilePaths;
        private List<String> excludedFields;
        private Long readTimeout;
        private Long consumerDeltaTime;
        private boolean isBatchConsumer;
        private int batchSize;

        public Builder<K, V> topic(Topic topic) {
            this.topic = topic;
            return this;
        }

        public Builder<K, V> properties(Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public Builder<K, V> consumer(Consumer<K, V> consumer) {
            this.consumer = consumer;
            return this;
        }

        public Builder<K, V> expectedRecordKey(String expectedRecordKey) {
            this.expectedRecordKey = expectedRecordKey;
            return this;
        }

        public Builder<K, V> matchMethod(String matchMethod) {
            this.matchMethod = matchMethod;
            return this;
        }

        /**
         * Sets the expected file paths for matching. For a single file, pass {@code List.of(path)}. For batch matching,
         * pass one path per expected record in order.
         */
        public Builder<K, V> matchFilePaths(List<String> matchFilePaths) {
            this.matchFilePaths = matchFilePaths;
            return this;
        }

        /** Convenience setter for single-record consumers. Wraps the path in a {@code List}. */
        public Builder<K, V> matchFilePath(String matchFilePath) {
            this.matchFilePaths = matchFilePath != null ? List.of(matchFilePath) : Collections.emptyList();
            return this;
        }

        /**
         * Sets the fields / keys to exclude from the comparison (e.g. timestamps, generated IDs). Populated from the
         * DataTable {@code excludedKeys} or {@code excludedElements} columns.
         */
        public Builder<K, V> excludedFields(List<String> excludedFields) {
            this.excludedFields = excludedFields;
            return this;
        }

        public Builder<K, V> readTimeout(Long readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder<K, V> consumerDeltaTime(Long consumerDeltaTime) {
            this.consumerDeltaTime = consumerDeltaTime;
            return this;
        }

        public Builder<K, V> isBatchConsumer(boolean isBatchConsumer) {
            this.isBatchConsumer = isBatchConsumer;
            return this;
        }

        public Builder<K, V> batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public ConsumerContext<K, V> build() {
            Topic validatedTopic = validateTopic(topic);

            // Use config-based properties if not provided
            Map<String, String> validatedProps = properties != null
                    ? properties
                    : KtestifyConfig.getOrLoad().getKafka().getConsumerPropertiesAsMap();

            // Consumer must still be provided (use KafkaClientFactory to create one)
            Consumer<K, V> validatedConsumer = requireNonNull(
                    consumer,
                    "Kafka consumer must be provided. Use KafkaClientFactory.createRawConsumer() or KafkaClientFactory.createAvroConsumer()");

            return new ConsumerContext<>(
                    validatedTopic,
                    validatedProps,
                    validatedConsumer,
                    expectedRecordKey,
                    matchMethod,
                    matchFilePaths,
                    excludedFields,
                    readTimeout,
                    consumerDeltaTime,
                    isBatchConsumer,
                    batchSize);
        }

        private static Topic validateTopic(Topic topic) {
            requireNonNull(topic, "Topic must be provided");
            String topicName = topic.getTopicName();
            if (topicName == null || topicName.isEmpty()) {
                throw new ConsumerException("No topic name was specified !");
            }
            if (topic.getTopicType() == Topic.Type.INPUT) {
                throw new ConsumerException("Topic type is INPUT. Cannot consume from an input topic !");
            }
            return topic;
        }

        private static <T> T requireNonNull(T value, String message) {
            if (value == null) {
                throw new ConsumerException(message);
            }
            return value;
        }
    }
}
