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
package io.github.ktestify.io.kafka.impl;

import io.github.ktestify.io.kafka.AbstractKafkaProducer;
import io.github.ktestify.io.kafka.ProducerContext;
import io.github.ktestify.models.Topic;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class RawKafkaProducer extends AbstractKafkaProducer<String, String> {

    public RawKafkaProducer(Topic topic, Map<String, String> properties, Producer<String, String> producer, File file) {
        this(topic, properties, producer, file, Map.of(), null);
    }

    public RawKafkaProducer(
            Topic topic,
            Map<String, String> properties,
            Producer<String, String> producer,
            File file,
            Map<String, String> headers,
            String payload) {
        this(ProducerContext.<String, String>builder()
                .topic(topic)
                .properties(properties)
                .producer(producer)
                .payloadFile(file)
                .headers(headers)
                .payload(payload)
                .build());
    }

    public RawKafkaProducer(ProducerContext<String, String> context) {
        super(context);
    }

    @Override
    protected void produce() {
        try {
            String payload = resolvePayload();
            String key = context.getRecordKey();
            ProducerRecord<String, String> record = buildRecord(key, payload);
            RecordMetadata metadata = producer.send(record).get();
            logger.info(
                    "Produced raw record to {} with key={} partition {} offset {} and timestamp {}",
                    metadata.topic(),
                    record.key(),
                    metadata.partition(),
                    metadata.offset(),
                    metadata.timestamp());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while producing raw message", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to produce raw message", e.getCause());
        }
    }
}
