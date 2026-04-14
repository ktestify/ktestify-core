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
package io.github.ktestify.models;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import lombok.Value;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

/**
 * Immutable value object representing a single record that has been fetched from any IO source (Kafka, IBM MQ, etc.).
 *
 * <p>This is the <em>common currency</em> that flows between the transport layer ({@code RecordFetcher}) and the
 * assertion layer ({@code RecordMatcher}). Matchers have zero dependency on Kafka or any other transport — they only
 * know about {@code ConsumedRecord}.
 *
 * @param <V> the type of the record value
 * @since 0.3.0
 */
@Value
public class ConsumedRecord<V> {

    /** The source topic / queue / channel name. */
    String source;

    /** Partition index — 0 for non-partitioned sources (e.g. IBM MQ). */
    int partition;

    /** Offset within the partition — {@code -1} if the source has no offset concept. */
    long offset;

    /** Record key as a String. May be {@code null} if the source has no key concept. */
    String key;

    /** The deserialized record value. */
    V value;

    /** Timestamp at which the record was written to the source. */
    Instant timestamp;

    /** Transport-level headers / properties. Keys and values are Strings to stay transport-agnostic. */
    Map<String, String> headers;

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a {@code ConsumedRecord} from a Kafka {@link ConsumerRecord}.
     *
     * @param <K> the Kafka key type
     * @param <V> the Kafka value type
     * @param record the Kafka consumer record
     * @return a new {@code ConsumedRecord}
     */
    public static <K, V> ConsumedRecord<V> fromKafkaRecord(ConsumerRecord<K, V> record) {
        Map<String, String> headers = extractHeaders(record);
        String key = record.key() != null ? record.key().toString() : null;
        return new ConsumedRecord<>(
                record.topic(),
                record.partition(),
                record.offset(),
                key,
                record.value(),
                Instant.ofEpochMilli(record.timestamp()),
                headers);
    }

    /**
     * Converts this record's coordinates into a {@link MatchedRecord} for deduplication purposes.
     *
     * @return a new {@code MatchedRecord}
     */
    public MatchedRecord toMatchedRecord() {
        return new MatchedRecord(source, partition, offset, key, timestamp, Instant.now());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static <K, V> Map<String, String> extractHeaders(ConsumerRecord<K, V> record) {
        if (record.headers() == null) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new java.util.LinkedHashMap<>();
        for (Header header : record.headers()) {
            if (header.value() != null) {
                result.put(header.key(), new String(header.value(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
