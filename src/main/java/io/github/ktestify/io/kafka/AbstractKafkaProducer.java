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

import io.github.ktestify.io.core.AbstractProducer;
import io.github.ktestify.models.Topic;
import io.github.ktestify.utils.FileUtils;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractKafkaProducer<K, V> extends AbstractProducer {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Producer<K, V> producer;
    protected final File payloadFile;
    protected final ProducerContext<K, V> context;
    protected final Map<String, String> headers;
    protected final String inlinePayload;

    protected AbstractKafkaProducer(ProducerContext<K, V> context) {
        super(context.getProperties());
        this.context = context;
        this.producer = context.getProducer();
        this.payloadFile = context.getPayloadFile();
        this.headers = context.getHeaders();
        this.inlinePayload = context.getPayload();
    }

    protected AbstractKafkaProducer(Topic topic, Map<String, String> properties, Producer<K, V> producer, File file) {
        this(ProducerContext.<K, V>builder()
                .topic(topic)
                .properties(properties)
                .producer(producer)
                .payloadFile(file)
                .build());
    }

    protected AbstractKafkaProducer(
            Topic topic,
            Map<String, String> properties,
            Producer<K, V> producer,
            File file,
            Map<String, String> headers,
            String payload,
            String schemaName) {
        this(ProducerContext.<K, V>builder()
                .topic(topic)
                .properties(properties)
                .producer(producer)
                .payloadFile(file)
                .headers(headers)
                .payload(payload)
                .schemaName(schemaName)
                .build());
    }

    protected AbstractKafkaProducer(ProducerContext<K, V> context, Map<String, String> headers, String payload) {
        this(ProducerContext.<K, V>builder()
                .topic(context.getTopic())
                .properties(context.getProperties())
                .producer(context.getProducer())
                .payloadFile(context.getPayloadFile())
                .headers(headers)
                .payload(payload)
                .schemaName(context.getSchemaName())
                .build());
    }

    protected Topic getTopic() {
        return context.getTopic();
    }

    protected Map<String, String> getProducerProperties() {
        return context.getProperties();
    }

    protected Producer<K, V> getKafkaProducer() {
        return producer;
    }

    protected File getPayloadFile() {
        return payloadFile;
    }

    protected ProducerRecord<K, V> buildRecord(K key, V value) {
        var record = new ProducerRecord<>(getTopic().getTopicName(), key, value);
        if (!headers.isEmpty()) {
            for (Header header : headers.entrySet().stream()
                    .map(e -> (Header) new RecordHeader(e.getKey(), e.getValue().getBytes(StandardCharsets.UTF_8)))
                    .toList()) {
                record.headers().add(header);
            }
        }
        return record;
    }

    protected String resolvePayload() {
        if (inlinePayload != null && !inlinePayload.isEmpty()) {
            return inlinePayload;
        }
        if (payloadFile != null) {
            return FileUtils.getFileContent(payloadFile);
        }
        throw new IllegalStateException("No payload content was provided");
    }

    protected String resolveSchema() throws IOException, RestClientException {
        SchemaRegistryClient client = SchemaRegistryClientFactory.createClient();
        if (!StringUtils.isBlank(context.getSchemaName())) {
            return client.getLatestSchemaMetadata(context.getSchemaName() + "-value")
                    .getSchema();
        }
        String subject = context.getTopic().getNamespacedTopic() + "-value";
        if (context.getSchemaVersion() == null || context.getSchemaVersion().isEmpty()) {
            return client.getLatestSchemaMetadata(subject).getSchema();
        } else {
            return client.getSchemaMetadata(subject, Integer.parseInt(context.getSchemaVersion()))
                    .getSchema();
        }
    }
}
