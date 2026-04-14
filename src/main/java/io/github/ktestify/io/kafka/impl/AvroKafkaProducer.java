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
package io.github.ktestify.io.kafka.impl;

import com.google.gson.JsonObject;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.github.ktestify.io.kafka.AbstractKafkaProducer;
import io.github.ktestify.io.kafka.ProducerContext;
import io.github.ktestify.models.Topic;
import io.github.ktestify.utils.serdes.AvroUtils;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class AvroKafkaProducer extends AbstractKafkaProducer<String, GenericRecord> {

    public AvroKafkaProducer(
            Topic topic,
            Map<String, String> properties,
            Producer<String, GenericRecord> producer,
            File payloadFile,
            String schemaName) {
        this(topic, properties, producer, payloadFile, Map.of(), null, schemaName);
    }

    public AvroKafkaProducer(
            Topic topic,
            Map<String, String> properties,
            Producer<String, GenericRecord> producer,
            File payloadFile,
            Map<String, String> headers,
            String payload,
            String schemaName) {
        this(ProducerContext.<String, GenericRecord>builder()
                .topic(topic)
                .properties(properties)
                .producer(producer)
                .payloadFile(payloadFile)
                .headers(headers)
                .payload(payload)
                .schemaName(schemaName)
                .build());
    }

    public AvroKafkaProducer(ProducerContext<String, GenericRecord> context) {
        super(context);
    }

    @Override
    protected void produce() {
        try {
            Schema schema = new Schema.Parser().parse(resolveSchema());
            JsonObject payload =
                    AvroUtils.getJsonElementFromFile(resolvePayload()).getAsJsonObject();
            GenericRecord recordValue = (GenericRecord) AvroUtils.convertJsonToAvro(payload, schema);
            ProducerRecord<String, GenericRecord> record = buildRecord(context.getRecordKey(), recordValue);
            RecordMetadata metadata = producer.send(record).get();
            logger.info(
                    "Produced avro record to {} partition {} offset {}",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while producing Avro message", e);

        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to produce Avro message", e.getCause());
        } catch (IOException | RestClientException e) {
            throw new RuntimeException("Failed to read schema or payload for Avro message", e);
        }
    }
}
