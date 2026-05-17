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
import io.github.ktestify.exceptions.ProducerException;
import io.github.ktestify.models.Topic;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import org.apache.kafka.clients.producer.Producer;

@Getter
public final class ProducerContext<K, V> {

    private final Topic topic;
    private final Map<String, String> properties;
    private final Producer<K, V> producer;
    private final File payloadFile;
    private final String recordKey;
    private final Map<String, String> headers;
    private final String payload;
    private final String schemaName;
    private final String schemaVersion;

    private ProducerContext(
            Topic topic,
            Map<String, String> properties,
            Producer<K, V> producer,
            File payloadFile,
            String recordKey,
            Map<String, String> headers,
            String payload,
            String schemaName,
            String schemaVersion) {
        this.topic = topic;
        this.properties = properties;
        this.producer = producer;
        this.payloadFile = payloadFile;
        this.recordKey = recordKey;
        this.headers = headers;
        this.payload = payload;
        this.schemaName = schemaName;
        this.schemaVersion = schemaVersion;
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    public static final class Builder<K, V> {

        private Topic topic;
        private Map<String, String> properties;
        private Producer<K, V> producer;
        private File payloadFile;
        private String recordKey;
        private Map<String, String> headers = Collections.emptyMap();
        private String payload;
        private String schemaName;
        private String schemaVersion;

        public Builder<K, V> topic(Topic topic) {
            this.topic = topic;
            return this;
        }

        public Builder<K, V> properties(Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public Builder<K, V> producer(Producer<K, V> producer) {
            this.producer = producer;
            return this;
        }

        public Builder<K, V> payloadFile(File payloadFile) {
            this.payloadFile = payloadFile;
            return this;
        }

        public Builder<K, V> recordKey(String recordKey) {
            this.recordKey = recordKey;
            return this;
        }

        public Builder<K, V> headers(Map<String, String> headers) {
            this.headers = headers == null ? Collections.emptyMap() : headers;
            return this;
        }

        public Builder<K, V> payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder<K, V> schemaName(String schemaName) {
            this.schemaName = schemaName;
            return this;
        }

        public Builder<K, V> schemaVersion(String schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public ProducerContext<K, V> build() {
            Topic validatedTopic = Topic.validateTopic(topic, Topic.Type.INPUT);

            // Use config-based properties if not provided
            Map<String, String> validatedProps = properties != null
                    ? properties
                    : KtestifyConfig.getOrLoad().getKafka().getProducerPropertiesAsMap();

            // Producer must still be provided (use KafkaClientFactory to create one)
            Producer<K, V> validatedProducer = requireNonNull(
                    producer,
                    "Kafka producer must be provided. Use KafkaClientFactory.createRawProducer() or KafkaClientFactory.createAvroProducer()");

            File validatedFile = validateFile(payloadFile);
            Map<String, String> validatedHeaders = headers.isEmpty() ? Collections.emptyMap() : Map.copyOf(headers);
            return new ProducerContext<>(
                    validatedTopic,
                    validatedProps,
                    validatedProducer,
                    validatedFile,
                    recordKey,
                    validatedHeaders,
                    payload,
                    schemaName,
                    schemaVersion);
        }

        private static File validateFile(File file) {
            if (file == null) {
                return null;
            }
            if (!file.exists()) {
                throw new ProducerException("The specified file does not exist: " + file.getAbsolutePath());
            }
            if (!file.isFile()) {
                throw new ProducerException("The specified file is not a regular file: " + file.getAbsolutePath());
            }
            if (file.length() == 0) {
                throw new ProducerException("The specified file is empty: " + file.getAbsolutePath());
            }
            return file;
        }

        private static <T> T requireNonNull(T value, String message) {
            if (value == null) {
                throw new ProducerException(message);
            }
            return value;
        }
    }
}
