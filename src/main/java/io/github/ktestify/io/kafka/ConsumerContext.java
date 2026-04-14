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
package io.github.ktestify.io.kafka;

import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.models.Topic;
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
    private final String matchFilePath;
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
            String matchFilePath,
            Long readTimeout,
            Long consumerDeltaTime,
            boolean isBatchConsumer,
            int batchSize) {
        this.topic = topic;
        this.properties = properties;
        this.consumer = consumer;
        this.expectedRecordKey = expectedRecordKey;
        this.matchMethod = matchMethod;
        this.matchFilePath = matchFilePath;
        this.readTimeout = readTimeout;
        this.consumerDeltaTime = consumerDeltaTime;
        this.isBatchConsumer = isBatchConsumer;
        this.batchSize = batchSize;
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
        private String matchFilePath;
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

        public Builder<K, V> matchFilePath(String matchFilePath) {
            this.matchFilePath = matchFilePath;
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
                    matchFilePath,
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
