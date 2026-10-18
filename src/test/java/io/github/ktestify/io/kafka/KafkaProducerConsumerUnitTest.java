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

import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.exceptions.ProducerException;
import io.github.ktestify.io.kafka.impl.AvroKafkaConsumer;
import io.github.ktestify.io.kafka.impl.RawKafkaConsumer;
import io.github.ktestify.io.kafka.impl.RawKafkaProducer;
import io.github.ktestify.match.BatchMatchStrategy;
import io.github.ktestify.match.KeyMatchStrategy;
import io.github.ktestify.match.RecordMatcherFactory;
import io.github.ktestify.models.Topic;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.clients.consumer.internals.AutoOffsetResetStrategy;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kafka Producer and Consumer Unit Tests")
class KafkaProducerConsumerUnitTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("RawKafkaProducer sends inline and file payloads with headers using MockProducer")
    void rawKafkaProducerSendsWithHeaders() throws Exception {
        MockProducer<String, String> mockProducer =
                new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
        Topic topic =
                Topic.builder().topicName("orders").topicType(Topic.Type.INPUT).build();

        File payloadFile = tempDir.resolve("payload.json").toFile();
        Files.writeString(payloadFile.toPath(), "{\"orderId\":\"ORD-1\"}", StandardCharsets.UTF_8);

        ProducerContext<String, String> ctx = ProducerContext.<String, String>builder()
                .topic(topic)
                .producer(mockProducer)
                .payloadFile(payloadFile)
                .recordKey("key-1")
                .headers(Map.of("traceId", "abc-123"))
                .build();

        new RawKafkaProducer(ctx).send();
        assertEquals(1, mockProducer.history().size());
        assertEquals("key-1", mockProducer.history().getFirst().key());

        RawKafkaProducer inlineProducer =
                new RawKafkaProducer(topic, Map.of(), mockProducer, null, Map.of(), "{\"inline\":true}");
        inlineProducer.send();
        assertEquals(2, mockProducer.history().size());
    }

    @Test
    @DisplayName("RawKafkaProducer wraps ExecutionException in ProducerException")
    void rawKafkaProducerHandlesSendFailure() {
        MockProducer<String, String> mockProducer =
                new MockProducer<>(false, null, new StringSerializer(), new StringSerializer());
        Topic topic =
                Topic.builder().topicName("orders").topicType(Topic.Type.INPUT).build();

        ProducerContext<String, String> ctx = ProducerContext.<String, String>builder()
                .topic(topic)
                .producer(mockProducer)
                .payload("{\"orderId\":\"ORD-1\"}")
                .build();

        RawKafkaProducer producer = new RawKafkaProducer(ctx);
        Thread completer = new Thread(() -> {
            while (mockProducer.history().isEmpty()) {
                Thread.onSpinWait();
            }
            mockProducer.errorNext(new RuntimeException("broker down"));
        });
        completer.start();

        assertThrows(ProducerException.class, producer::send);
    }

    @Test
    @DisplayName("RawKafkaConsumer fetches and matches records via MockConsumer")
    void rawKafkaConsumerFetchesAndMatches() throws Exception {
        KafkaRecordFetcher.clearMatchedRecords();
        TopicPartition tp = new TopicPartition("orders-out", 0);
        MockConsumer<String, String> mockConsumer = new MockConsumer<>(AutoOffsetResetStrategy.EARLIEST.name()) {
            @Override
            public Map<TopicPartition, OffsetAndTimestamp> offsetsForTimes(
                    Map<TopicPartition, Long> timestampsToSearch, Duration timeout) {
                return Map.of(tp, new OffsetAndTimestamp(0L, System.currentTimeMillis()));
            }
        };

        File expectedFile = tempDir.resolve("expected.json").toFile();
        Files.writeString(expectedFile.toPath(), "{\"orderId\":\"ORD-1\"}", StandardCharsets.UTF_8);

        mockConsumer.schedulePollTask(() -> {
            mockConsumer.rebalance(List.of(tp));
            mockConsumer.updateBeginningOffsets(Map.of(tp, 0L));
            mockConsumer.updateEndOffsets(Map.of(tp, 1L));
        });
        mockConsumer.schedulePollTask(() -> {
            mockConsumer.addRecord(new ConsumerRecord<>("orders-out", 0, 0L, "key-1", "{\"orderId\":\"ORD-1\"}"));
        });

        Topic topic = Topic.builder()
                .topicName("orders-out")
                .topicType(Topic.Type.OUTPUT)
                .build();

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(topic)
                .consumer(mockConsumer)
                .matchMethod(RecordMatcherFactory.METHOD_MATCH_FILE)
                .matchFilePath(expectedFile.getAbsolutePath())
                .expectedRecordKey("key-1")
                .keyMatchStrategy(KeyMatchStrategy.EXACT)
                .batchMatchStrategy(BatchMatchStrategy.POSITIONAL)
                .readTimeout(2000L)
                .consumerDeltaTime(10000L)
                .referenceTimestamp(System.currentTimeMillis())
                .build();

        RawKafkaConsumer consumer = new RawKafkaConsumer(ctx);
        assertTrue(consumer.call());
    }

    @Test
    @DisplayName("RawKafkaConsumer fetches and matches batch records with expectedRecordKeys via MockConsumer")
    void rawKafkaConsumerFetchesAndMatchesBatchWithExpectedRecordKeys() throws Exception {
        KafkaRecordFetcher.clearMatchedRecords();
        TopicPartition tp = new TopicPartition("batch-out", 0);
        MockConsumer<String, String> mockConsumer = new MockConsumer<>(AutoOffsetResetStrategy.EARLIEST.name()) {
            @Override
            public Map<TopicPartition, OffsetAndTimestamp> offsetsForTimes(
                    Map<TopicPartition, Long> timestampsToSearch, Duration timeout) {
                return Map.of(tp, new OffsetAndTimestamp(0L, System.currentTimeMillis()));
            }
        };

        File expectedFile1 = tempDir.resolve("expected1.json").toFile();
        File expectedFile2 = tempDir.resolve("expected2.json").toFile();
        Files.writeString(expectedFile1.toPath(), "{\"orderId\":\"ORD-1\"}", StandardCharsets.UTF_8);
        Files.writeString(expectedFile2.toPath(), "{\"orderId\":\"ORD-2\"}", StandardCharsets.UTF_8);

        mockConsumer.schedulePollTask(() -> {
            mockConsumer.rebalance(List.of(tp));
            mockConsumer.updateBeginningOffsets(Map.of(tp, 0L));
            mockConsumer.updateEndOffsets(Map.of(tp, 3L));
        });
        mockConsumer.schedulePollTask(() -> {
            mockConsumer.addRecord(
                    new ConsumerRecord<>("batch-out", 0, 0L, "unrelated-key", "{\"orderId\":\"OTHER\"}"));
            mockConsumer.addRecord(new ConsumerRecord<>("batch-out", 0, 1L, "key-2", "{\"orderId\":\"ORD-2\"}"));
            mockConsumer.addRecord(new ConsumerRecord<>("batch-out", 0, 2L, "key-1", "{\"orderId\":\"ORD-1\"}"));
        });

        Topic topic = Topic.builder()
                .topicName("batch-out")
                .topicType(Topic.Type.OUTPUT)
                .build();

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(topic)
                .consumer(mockConsumer)
                .matchMethod(RecordMatcherFactory.METHOD_MATCH_FILE)
                .matchFilePaths(List.of(expectedFile1.getAbsolutePath(), expectedFile2.getAbsolutePath()))
                .expectedRecordKeys(List.of("key-1", "key-2"))
                .keyMatchStrategy(KeyMatchStrategy.EXACT)
                .batchMatchStrategy(BatchMatchStrategy.LAZY)
                .isBatchConsumer(true)
                .batchSize(2)
                .readTimeout(2000L)
                .consumerDeltaTime(10000L)
                .referenceTimestamp(System.currentTimeMillis())
                .build();

        RawKafkaConsumer consumer = new RawKafkaConsumer(ctx);
        assertTrue(consumer.call());
    }

    @Test
    @DisplayName("AvroKafkaConsumer resolves default matcher and rejects INPUT topic")
    void avroKafkaConsumerContextValidation() {
        MockConsumer<String, GenericRecord> mockConsumer = new MockConsumer<>(AutoOffsetResetStrategy.EARLIEST.name());
        Topic outputTopic = Topic.builder()
                .topicName("avro-out")
                .topicType(Topic.Type.OUTPUT)
                .build();

        ConsumerContext<String, GenericRecord> ctx = ConsumerContext.<String, GenericRecord>builder()
                .topic(outputTopic)
                .consumer(mockConsumer)
                .matchMethod(RecordMatcherFactory.METHOD_MATCH_FILE)
                .matchFilePaths(List.of("dummy.json"))
                .excludedFields(List.of("ts"))
                .isBatchConsumer(false)
                .batchSize(1)
                .build();

        AvroKafkaConsumer avroConsumer = new AvroKafkaConsumer(ctx);
        assertNotNull(avroConsumer);

        assertThrows(
                ConsumerException.class,
                () -> ConsumerContext.<String, GenericRecord>builder()
                        .topic(outputTopic)
                        .consumer(null)
                        .build());
    }
}
