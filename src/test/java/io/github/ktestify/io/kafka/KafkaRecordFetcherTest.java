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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.exceptions.ConfigException;
import io.github.ktestify.exceptions.FetchException;
import io.github.ktestify.exceptions.FetchTimeoutException;
import io.github.ktestify.models.Topic;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InterruptException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KafkaRecordFetcher")
class KafkaRecordFetcherTest {

    private static final Topic OUTPUT_TOPIC =
            Topic.builder().topicName("orders-out").topicType(Topic.Type.OUTPUT).build();

    private static final TopicPartition PARTITION_0 = new TopicPartition(OUTPUT_TOPIC.getNamespacedTopic(), 0);

    @BeforeEach
    void setUp() {
        KafkaRecordFetcher.clearMatchedRecords();
    }

    @AfterEach
    void tearDown() {
        // Ensure no test leaks an interrupted status to subsequent tests
        Thread.interrupted();
    }

    @Test
    @DisplayName("clamps poll duration to remaining budget when read timeout is smaller than poll interval")
    void clampsPollDurationToRemainingBudget() {
        long pollIntervalMs = KtestifyConfig.getOrLoad().getFramework().getPollIntervalMillis();
        long shortTimeoutMs = Math.max(20L, Math.min(80L, pollIntervalMs / 2));

        @SuppressWarnings("unchecked")
        Consumer<String, String> mockConsumer = mock(Consumer.class);
        when(mockConsumer.assignment()).thenReturn(Set.of(PARTITION_0));
        when(mockConsumer.offsetsForTimes(anyMap(), any(Duration.class)))
                .thenReturn(Map.of(PARTITION_0, new OffsetAndTimestamp(0L, System.currentTimeMillis())));

        List<Duration> recordedDurations = new ArrayList<>();
        when(mockConsumer.poll(any(Duration.class))).thenAnswer(invocation -> {
            Duration timeout = invocation.getArgument(0);
            recordedDurations.add(timeout);
            Thread.sleep(Math.min(15L, Math.max(1L, timeout.toMillis())));
            return ConsumerRecords.empty();
        });

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(OUTPUT_TOPIC)
                .consumer(mockConsumer)
                .readTimeout(shortTimeoutMs)
                .consumerDeltaTime(1000L)
                .referenceTimestamp(System.currentTimeMillis())
                .build();

        KafkaRecordFetcher<String, String> fetcher = new KafkaRecordFetcher<>(ctx);
        assertThrows(FetchTimeoutException.class, fetcher::fetch);

        assertFalse(recordedDurations.isEmpty(), "Expected at least one poll call");
        for (Duration d : recordedDurations) {
            assertTrue(d.toNanos() > 0L, "Poll duration must be positive, got: " + d);
            assertTrue(
                    d.toMillis() <= shortTimeoutMs,
                    "Poll duration " + d.toMillis() + "ms exceeded read timeout " + shortTimeoutMs + "ms");
            assertTrue(
                    d.toMillis() < pollIntervalMs,
                    "Poll duration " + d.toMillis() + "ms should be clamped below pollInterval " + pollIntervalMs
                            + "ms");
        }
    }

    @Test
    @DisplayName("stops immediately with FetchTimeoutException when a poll consumes the remaining budget")
    void stopsImmediatelyWhenPollConsumesRemainingBudget() {
        long readTimeoutMs = 60L;

        @SuppressWarnings("unchecked")
        Consumer<String, String> mockConsumer = mock(Consumer.class);
        when(mockConsumer.assignment()).thenReturn(Set.of(PARTITION_0));
        when(mockConsumer.offsetsForTimes(anyMap(), any(Duration.class)))
                .thenReturn(Map.of(PARTITION_0, new OffsetAndTimestamp(0L, System.currentTimeMillis())));
        when(mockConsumer.poll(any(Duration.class))).thenAnswer(invocation -> {
            Thread.sleep(readTimeoutMs + 20L);
            return ConsumerRecords.empty();
        });

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(OUTPUT_TOPIC)
                .consumer(mockConsumer)
                .readTimeout(readTimeoutMs)
                .consumerDeltaTime(1000L)
                .referenceTimestamp(System.currentTimeMillis())
                .build();

        KafkaRecordFetcher<String, String> fetcher = new KafkaRecordFetcher<>(ctx);
        assertThrows(FetchTimeoutException.class, fetcher::fetch);

        verify(mockConsumer, times(1)).poll(any(Duration.class));
    }

    @Test
    @DisplayName("restores interrupt flag when Kafka poll throws InterruptException")
    void restoresInterruptFlagOnInterruptedPoll() {
        @SuppressWarnings("unchecked")
        Consumer<String, String> mockConsumer = mock(Consumer.class);
        when(mockConsumer.assignment()).thenReturn(Set.of(PARTITION_0));
        when(mockConsumer.offsetsForTimes(anyMap(), any(Duration.class)))
                .thenReturn(Map.of(PARTITION_0, new OffsetAndTimestamp(0L, System.currentTimeMillis())));
        when(mockConsumer.poll(any(Duration.class)))
                .thenThrow(new InterruptException(new InterruptedException("poll interrupted")));

        // Clear flag set by InterruptException construction so we verify the catch block restores it
        Thread.interrupted();

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(OUTPUT_TOPIC)
                .consumer(mockConsumer)
                .readTimeout(1000L)
                .consumerDeltaTime(1000L)
                .referenceTimestamp(System.currentTimeMillis())
                .build();

        KafkaRecordFetcher<String, String> fetcher = new KafkaRecordFetcher<>(ctx);
        FetchException ex = assertThrows(FetchException.class, fetcher::fetch);
        assertFalse(ex instanceof FetchTimeoutException, "Interrupted fetch should not be reported as timeout");
        assertTrue(Thread.currentThread().isInterrupted(), "Calling thread interrupt flag must be restored");
    }

    @Test
    @DisplayName("clamps assignment poll duration and throws FetchTimeoutException when no partition is assigned")
    void clampsAssignmentPollAndThrowsFetchTimeoutException() {
        long readTimeoutMs = 50L;

        @SuppressWarnings("unchecked")
        Consumer<String, String> mockConsumer = mock(Consumer.class);
        when(mockConsumer.assignment()).thenReturn(Collections.emptySet());

        List<Duration> recordedDurations = new ArrayList<>();
        when(mockConsumer.poll(any(Duration.class))).thenAnswer(invocation -> {
            Duration timeout = invocation.getArgument(0);
            recordedDurations.add(timeout);
            Thread.sleep(Math.min(20L, Math.max(1L, timeout.toMillis())));
            return ConsumerRecords.empty();
        });

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(OUTPUT_TOPIC)
                .consumer(mockConsumer)
                .readTimeout(readTimeoutMs)
                .consumerDeltaTime(1000L)
                .referenceTimestamp(System.currentTimeMillis())
                .build();

        KafkaRecordFetcher<String, String> fetcher = new KafkaRecordFetcher<>(ctx);
        assertThrows(FetchTimeoutException.class, fetcher::fetch);

        assertFalse(recordedDurations.isEmpty());
        for (Duration d : recordedDurations) {
            assertTrue(d.toMillis() <= readTimeoutMs, "Assignment poll exceeded read timeout: " + d);
        }
    }

    @Test
    @DisplayName("accepts zero and maximum valid consumerDeltaTime in properties")
    void acceptsZeroAndMaxValidConsumerDeltaTime() throws Exception {
        long referenceTs = 1_700_000_000_000L;

        assertEquals(referenceTs, captureSeekTimestamp(referenceTs, "0"));
        assertEquals(0L, captureSeekTimestamp(referenceTs, String.valueOf(referenceTs / 1000L)));
    }

    @Test
    @DisplayName("rejects negative consumerDeltaTime with ConfigException containing original input")
    void rejectsNegativeConsumerDeltaTime() {
        @SuppressWarnings("unchecked")
        Consumer<String, String> mockConsumer = mock(Consumer.class);
        when(mockConsumer.assignment()).thenReturn(Set.of(PARTITION_0));

        ConsumerContext<String, String> propCtx = ConsumerContext.<String, String>builder()
                .topic(OUTPUT_TOPIC)
                .consumer(mockConsumer)
                .properties(Map.of(KafkaRecordFetcher.CONSUMER_DELTA_TIME, "-5"))
                .readTimeout(500L)
                .referenceTimestamp(1_700_000_000_000L)
                .build();

        ConfigException propEx = assertThrows(ConfigException.class, () -> new KafkaRecordFetcher<>(propCtx).fetch());
        assertTrue(propEx.getMessage().contains("-5"), "Message should include original input: " + propEx.getMessage());

        ConsumerContext<String, String> ctxCtx = ConsumerContext.<String, String>builder()
                .topic(OUTPUT_TOPIC)
                .consumer(mockConsumer)
                .consumerDeltaTime(-100L)
                .readTimeout(500L)
                .referenceTimestamp(1_700_000_000_000L)
                .build();

        ConfigException ctxEx = assertThrows(ConfigException.class, () -> new KafkaRecordFetcher<>(ctxCtx).fetch());
        assertTrue(ctxEx.getMessage().contains("-100"), "Message should include original input: " + ctxEx.getMessage());
    }

    @Test
    @DisplayName("rejects overflowing consumerDeltaTime seconds-to-ms conversion with ConfigException")
    void rejectsOverflowingConsumerDeltaTime() {
        String overflowingSeconds = String.valueOf((Long.MAX_VALUE / 1000L) + 1L);

        @SuppressWarnings("unchecked")
        Consumer<String, String> mockConsumer = mock(Consumer.class);
        when(mockConsumer.assignment()).thenReturn(Set.of(PARTITION_0));

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(OUTPUT_TOPIC)
                .consumer(mockConsumer)
                .properties(Map.of(KafkaRecordFetcher.CONSUMER_DELTA_TIME, overflowingSeconds))
                .readTimeout(500L)
                .referenceTimestamp(1_700_000_000_000L)
                .build();

        ConfigException ex = assertThrows(ConfigException.class, () -> new KafkaRecordFetcher<>(ctx).fetch());
        assertTrue(
                ex.getMessage().contains(overflowingSeconds),
                "Message should include original input: " + ex.getMessage());
    }

    @Test
    @DisplayName("rejects consumerDeltaTime that produces a negative seek timestamp")
    void rejectsConsumerDeltaTimeProducingNegativeTimestamp() {
        long referenceTs = 1_700_000_000_000L;
        String tooLargeSeconds = String.valueOf((referenceTs / 1000L) + 1L);

        @SuppressWarnings("unchecked")
        Consumer<String, String> mockConsumer = mock(Consumer.class);
        when(mockConsumer.assignment()).thenReturn(Set.of(PARTITION_0));

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(OUTPUT_TOPIC)
                .consumer(mockConsumer)
                .properties(Map.of(KafkaRecordFetcher.CONSUMER_DELTA_TIME, tooLargeSeconds))
                .readTimeout(500L)
                .referenceTimestamp(referenceTs)
                .build();

        ConfigException ex = assertThrows(ConfigException.class, () -> new KafkaRecordFetcher<>(ctx).fetch());
        assertTrue(
                ex.getMessage().contains(tooLargeSeconds), "Message should include original input: " + ex.getMessage());
    }

    private long captureSeekTimestamp(long referenceTs, String deltaSeconds) throws Exception {
        KafkaRecordFetcher.clearMatchedRecords();
        @SuppressWarnings("unchecked")
        Consumer<String, String> mockConsumer = mock(Consumer.class);
        when(mockConsumer.assignment()).thenReturn(Set.of(PARTITION_0));

        AtomicReference<Long> capturedTimestamp = new AtomicReference<>();
        when(mockConsumer.offsetsForTimes(anyMap(), any(Duration.class))).thenAnswer(invocation -> {
            Map<TopicPartition, Long> map = invocation.getArgument(0);
            capturedTimestamp.set(map.get(PARTITION_0));
            return Map.of(PARTITION_0, new OffsetAndTimestamp(0L, referenceTs));
        });
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(OUTPUT_TOPIC.getNamespacedTopic(), 0, 0L, "k1", "v1");
        when(mockConsumer.poll(any(Duration.class)))
                .thenReturn(new ConsumerRecords<>(Map.of(PARTITION_0, List.of(record))));

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(OUTPUT_TOPIC)
                .consumer(mockConsumer)
                .properties(Map.of(KafkaRecordFetcher.CONSUMER_DELTA_TIME, deltaSeconds))
                .readTimeout(500L)
                .referenceTimestamp(referenceTs)
                .build();

        new KafkaRecordFetcher<>(ctx).fetch();
        assertNotNull(capturedTimestamp.get(), "Expected offsetsForTimes to be invoked");
        return capturedTimestamp.get();
    }
}
