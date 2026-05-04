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

import static io.github.ktestify.constants.LogMessagesConstants.*;

import io.github.ktestify.config.FrameworkConfig;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.exceptions.FetchException;
import io.github.ktestify.io.core.RecordFetcher;
import io.github.ktestify.models.ConsumedRecord;
import io.github.ktestify.models.MatchedRecord;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

/**
 * Kafka implementation of {@link RecordFetcher}.
 *
 * <p>Handles all Kafka-specific mechanics:
 *
 * <ul>
 *   <li>Subscribing to the topic and waiting for partition assignment
 *   <li>Seeking to the correct offset based on a configurable delta time
 *   <li>Polling until at least one record matching the key-filter is available or the read-timeout expires
 *   <li>Deduplication via a {@link MatchedRecord} registry (shared across consumers in the same test execution)
 * </ul>
 *
 * <p>This class knows <strong>nothing</strong> about matching or assertions. It produces {@link ConsumedRecord} objects
 * and hands them to whoever called {@link #fetch()} — typically an {@code AbstractKafkaConsumer} subclass.
 *
 * @param <K> the Kafka record key type
 * @param <V> the Kafka record value type
 * @since 0.3.0
 */
@Slf4j
public class KafkaRecordFetcher<K, V> implements RecordFetcher<V> {

    /**
     * Shared, thread-safe registry of records already matched within one test execution. Prevents two concurrent
     * consumers from matching the same Kafka record.
     *
     * <p>This set is intentionally static so that it is shared across all {@code KafkaRecordFetcher} instances within
     * the same JVM / test run. Call {@link #clearMatchedRecords()} between independent test scenarios.
     */
    private static final Set<MatchedRecord> MATCHED_RECORDS = ConcurrentHashMap.newKeySet();

    private final ConsumerContext<K, V> context;
    private final Consumer<K, V> kafkaConsumer;
    private final FrameworkConfig frameworkConfig;
    private final long readTimeoutMs;
    private final Map<String, String> properties;

    private volatile boolean closed = false;

    public static final String CONSUMER_DELTA_TIME = "consumerDeltaTime";

    /**
     * Creates a new fetcher from a {@link ConsumerContext}.
     *
     * @param context the consumer context holding topic, consumer, timeouts, and filters
     */
    public KafkaRecordFetcher(ConsumerContext<K, V> context) {
        this.context = context;
        this.kafkaConsumer = context.getConsumer();
        this.properties = context.getProperties();
        this.frameworkConfig = KtestifyConfig.getOrLoad().getFramework();
        this.readTimeoutMs = resolveReadTimeout();

        log.debug(
                "KafkaRecordFetcher created for topic '{}', readTimeout={}ms",
                context.getTopic().getNamespacedTopic(),
                readTimeoutMs);
    }

    // =========================================================================
    // RecordFetcher contract
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Blocks until at least one record that passes the key-filter is found, or {@code readTimeoutMs} elapses.
     *
     * @throws FetchException if the timeout expires without a matching record, or if the Kafka consumer is woken up
     *     externally
     */
    @Override
    public List<ConsumedRecord<V>> fetch() throws FetchException {
        try {
            subscribeAndAwaitAssignment();
            long delta = calculateDeltaTime();
            seekToOffset(delta);
            return pollUntilRecordFound();
        } catch (WakeupException e) {
            throw new FetchException(
                    "Kafka consumer was woken up for shutdown on topic '"
                            + context.getTopic().getNamespacedTopic() + "'",
                    e);
        }
    }

    /** Releases the Kafka consumer subscription and closes the underlying client. Safe to call more than once. */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            log.debug(
                    "Closing KafkaRecordFetcher for topic '{}'",
                    context.getTopic().getNamespacedTopic());
            try {
                kafkaConsumer.unsubscribe();
            } finally {
                kafkaConsumer.close();
            }
        }
    }

    // =========================================================================
    // Public utility
    // =========================================================================

    /**
     * Clears the shared deduplication registry.
     *
     * <p>Call this at the start of each independent test scenario (e.g. in a {@code @Before} / {@code Before} hook) to
     * ensure records from a previous scenario are not filtered out.
     */
    public static void clearMatchedRecords() {
        MATCHED_RECORDS.clear();
        log.debug("KafkaRecordFetcher: matched-records registry cleared.");
    }

    // =========================================================================
    // Private — Kafka mechanics (previously inside AbstractKafkaConsumer)
    // =========================================================================

    /** Subscribes to the configured topic and blocks until Kafka assigns partitions. */
    private void subscribeAndAwaitAssignment() {
        String namespacedTopic = context.getTopic().getNamespacedTopic();
        kafkaConsumer.subscribe(Collections.singletonList(namespacedTopic));
        log.info(MESSAGE_CONSUMER_SUBSCRIBED_TO_TOPIC, namespacedTopic);

        long pollIntervalMs = frameworkConfig.getPollIntervalMillis();
        while (kafkaConsumer.assignment().isEmpty()) {
            kafkaConsumer.poll(Duration.ofMillis(pollIntervalMs));
        }
        log.debug("Partition assignment received for topic '{}'.", namespacedTopic);
    }

    /**
     * Calculates the earliest timestamp to read from.
     *
     * <p>Priority order:
     *
     * <ol>
     *   <li>Explicit {@code consumerDeltaTime} set on the {@link ConsumerContext} (milliseconds)
     *   <li>{@code consumerDeltaTime} property in the properties map (seconds)
     *   <li>Framework default from {@code reference.conf}
     * </ol>
     */
    private long calculateDeltaTime() {
        // 1. Explicit value from context (already in ms)
        if (context.getConsumerDeltaTime() != null) {
            long delta = System.currentTimeMillis() - context.getConsumerDeltaTime();
            log.debug("Using consumer delta time from context: {}ms", context.getConsumerDeltaTime());
            return delta;
        }

        // 2. Properties map value (in seconds — convert to ms)
        long defaultDeltaMs = frameworkConfig.getConsumerDeltaTime().toMillis();
        String deltaTimeStr = properties.get(CONSUMER_DELTA_TIME);
        if (deltaTimeStr != null && !deltaTimeStr.isEmpty()) {
            log.debug(MESSAGE_CONSUMER_DELTA_TIME_FROM_DATATABLE, deltaTimeStr);
            try {
                long delta = System.currentTimeMillis() - (Long.parseLong(deltaTimeStr) * 1000);
                log.debug(MESSAGE_CONSUMER_DELTA_TIME_IN_TIMESTAMP, delta);
                return delta;
            } catch (NumberFormatException e) {
                log.error(ERROR_PARSING_CONSUMER_DELTA_TIME, defaultDeltaMs);
            }
        }

        // 3. Framework default
        log.debug(MESSAGE_CONSUMER_NO_DELTA_TIME_FOUND, defaultDeltaMs);
        return System.currentTimeMillis() - defaultDeltaMs;
    }

    /**
     * Seeks each assigned partition to the offset corresponding to {@code deltaTimestamp}. Partitions with no messages
     * after that timestamp are seeked to the end.
     */
    private void seekToOffset(long deltaTimestamp) {
        Set<TopicPartition> assignments = kafkaConsumer.assignment();
        log.debug(MESSAGE_CONSUMER_SEEKING_TO_OFFSET, deltaTimestamp);

        Map<TopicPartition, Long> timestampMap =
                assignments.stream().collect(Collectors.toMap(tp -> tp, tp -> deltaTimestamp));
        Map<TopicPartition, OffsetAndTimestamp> offsets = kafkaConsumer.offsetsForTimes(timestampMap);
        log.trace(MESSAGE_CONSUMER_RETRIEVED_OFFSETS, timestampMap);

        offsets.forEach((topicPartition, offsetAndTimestamp) -> {
            log.trace(MESSAGE_CONSUMER_OFFSET_FOR_TIMESTAMP, offsetAndTimestamp);
            if (offsetAndTimestamp != null) {
                kafkaConsumer.seek(topicPartition, offsetAndTimestamp.offset());
            } else {
                kafkaConsumer.seekToEnd(Collections.singletonList(topicPartition));
                log.debug(MESSAGE_CONSUMER_OFFSETS_NULL_SEEK_TO_LATEST, topicPartition.partition());
            }
        });
    }

    /**
     * Polls Kafka until a candidate record is found or {@link #readTimeoutMs} expires.
     *
     * <p>In <em>single-record mode</em> (default) the method returns as soon as at least one candidate passes the
     * key-filter and deduplication check.
     *
     * <p>In <em>batch mode</em> ({@link ConsumerContext#isBatchConsumer()} == {@code true}) the method keeps polling
     * until {@link ConsumerContext#getBatchSize()} distinct candidates have been collected, or the timeout expires.
     *
     * @return non-empty, unmodifiable list of {@link ConsumedRecord} ready for matching
     * @throws FetchException if the timeout expires before the required number of records is found
     */
    private List<ConsumedRecord<V>> pollUntilRecordFound() throws FetchException {
        long startTime = System.currentTimeMillis();
        long pollIntervalMs = frameworkConfig.getPollIntervalMillis();
        String namespacedTopic = context.getTopic().getNamespacedTopic();

        boolean batchMode = context.isBatchConsumer();
        int targetSize = batchMode ? context.getBatchSize() : 1;
        List<ConsumedRecord<V>> accumulated = new ArrayList<>();

        log.debug(
                "Polling topic '{}' in {} mode, target={} record(s).",
                namespacedTopic,
                batchMode ? "BATCH" : "SINGLE",
                targetSize);

        while (System.currentTimeMillis() - startTime < readTimeoutMs) {
            ConsumerRecords<K, V> batch = kafkaConsumer.poll(Duration.ofMillis(pollIntervalMs));

            if (!batch.isEmpty()) {
                log.debug(MESSAGE_CONSUMER_RECORDS_IN_TOPIC_NOT_EMPTY, namespacedTopic);

                for (ConsumerRecord<K, V> record : batch) {
                    ConsumedRecord<V> consumed = ConsumedRecord.fromKafkaRecord(record);

                    if (isAlreadyMatched(consumed)) {
                        log.debug(MESSAGE_CONSUMER_RECORD_ALREADY_MATCHED, consumed.toMatchedRecord());
                        continue;
                    }
                    if (!passesKeyFilter(record)) {
                        continue;
                    }

                    log.info(MESSAGE_CONSUMER_GOT_RECORD_WITH_KEY_AND_VALUE, record.key(), record.value());
                    registerAsMatched(consumed);
                    accumulated.add(consumed);

                    // In SINGLE mode return immediately on the first passing record.
                    // In BATCH mode stop as soon as targetSize is reached — do NOT
                    // continue draining the current poll batch beyond the target.
                    if (!batchMode) {
                        return Collections.unmodifiableList(accumulated);
                    }
                    if (accumulated.size() >= targetSize) {
                        log.info(
                                "Batch complete — collected {} / {} records from topic '{}'.",
                                accumulated.size(),
                                targetSize,
                                namespacedTopic);
                        return Collections.unmodifiableList(accumulated);
                    }
                }
            }
        }

        if (!accumulated.isEmpty()) {
            // Partial batch collected — treat as a timeout with context
            throw new FetchException("Timed out after " + readTimeoutMs + "ms waiting for " + targetSize
                    + " record(s) on topic '" + namespacedTopic
                    + "' — only " + accumulated.size() + " collected.");
        }

        throw new FetchException(
                "Timed out after " + readTimeoutMs + "ms waiting for a record on topic '" + namespacedTopic + "'.");
    }

    // =========================================================================
    // Private — filters and deduplication
    // =========================================================================

    /** Returns {@code true} if the record has already been claimed by another consumer in this test execution. */
    private boolean isAlreadyMatched(ConsumedRecord<V> record) {
        return MATCHED_RECORDS.contains(record.toMatchedRecord());
    }

    /** Registers a record in the shared deduplication registry. */
    private void registerAsMatched(ConsumedRecord<V> record) {
        MATCHED_RECORDS.add(record.toMatchedRecord());
        log.debug(MESSAGE_CONSUMER_RECORD_NOT_MATCHED_YET, record.toMatchedRecord());
    }

    /**
     * Returns {@code true} if no key-filter is configured, or if the record key matches the expected key from the
     * context / properties.
     */
    private boolean passesKeyFilter(ConsumerRecord<K, V> record) {
        // Context takes priority over properties map
        String expectedKey = context.getExpectedRecordKey();
        if (expectedKey == null || expectedKey.isEmpty()) {
            expectedKey = properties.get("expectedRecordKey");
        }

        if (expectedKey == null || expectedKey.isEmpty()) {
            return true; // No filter configured — accept any key
        }

        String recordKey = record.key() != null ? record.key().toString() : null;
        if (expectedKey.equals(recordKey)) {
            log.info(MESSAGE_CONSUMER_RECORD_MATCHES_EXPECTED_KEY, expectedKey);
            return true;
        }

        log.debug(MESSAGE_CONSUMER_RECORD_DO_NOT_MATCHES_EXPECTED_KEY, recordKey, expectedKey);
        return false;
    }

    // =========================================================================
    // Private — timeout resolution
    // =========================================================================

    private long resolveReadTimeout() {
        if (context.getReadTimeout() != null) {
            return context.getReadTimeout();
        }
        String timeoutStr = properties.get("consumerReadTimeout");
        if (timeoutStr != null && !timeoutStr.isEmpty()) {
            try {
                return Long.parseLong(timeoutStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid read timeout value '{}', falling back to framework default.", timeoutStr);
            }
        }
        return frameworkConfig.getDefaultReadTimeoutMillis();
    }
}
