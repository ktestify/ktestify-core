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
package io.github.ktestify.models;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConsumedRecord")
class ConsumedRecordTest {

    // =========================================================================
    // Backward-compatible constructor
    // =========================================================================

    @Nested
    @DisplayName("Legacy seven-argument constructor")
    class LegacyConstructor {

        @Test
        @DisplayName("keeps every pre-existing field intact")
        void keepsExistingFields() {
            Instant now = Instant.now();
            ConsumedRecord<String> record =
                    new ConsumedRecord<>("topic", 3, 42L, "key-1", "value-1", now, Map.of("h1", "v1"));

            assertEquals("topic", record.getSource());
            assertEquals(3, record.getPartition());
            assertEquals(42L, record.getOffset());
            assertEquals("key-1", record.getKey());
            assertEquals("value-1", record.getValue());
            assertEquals(now, record.getTimestamp());
            assertEquals(Map.of("h1", "v1"), record.getHeaders());
        }

        @Test
        @DisplayName("defaults attributes to an empty map")
        void defaultsAttributes() {
            ConsumedRecord<String> record =
                    new ConsumedRecord<>("topic", 0, -1L, "key", "value", Instant.now(), Collections.emptyMap());

            assertNotNull(record.getAttributes());
            assertTrue(record.getAttributes().isEmpty());
        }
    }

    // =========================================================================
    // Attributes
    // =========================================================================

    @Nested
    @DisplayName("Attributes")
    class Attributes {

        @Test
        @DisplayName("full constructor stores the supplied attributes")
        void storesAttributes() {
            Map<String, String> attributes = Map.of("statusCode", "200", "elapsedMs", "42");
            ConsumedRecord<String> record = new ConsumedRecord<>(
                    "http://localhost/api", 0, -1L, "GET", "{}", Instant.now(), Collections.emptyMap(), attributes);

            assertEquals(attributes, record.getAttributes());
        }

        @Test
        @DisplayName("null attributes are normalised to an empty map")
        void nullAttributesBecomeEmpty() {
            ConsumedRecord<String> record =
                    new ConsumedRecord<>("topic", 0, -1L, "key", "value", Instant.now(), Collections.emptyMap(), null);

            assertNotNull(record.getAttributes());
            assertTrue(record.getAttributes().isEmpty());
        }
    }

    // =========================================================================
    // Builder
    // =========================================================================

    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("builds a fully populated record")
        void buildsRecord() {
            Instant now = Instant.now();
            ConsumedRecord<String> record = ConsumedRecord.<String>builder()
                    .source("http://localhost/api")
                    .partition(0)
                    .offset(-1L)
                    .key("POST")
                    .value("{\"ok\":true}")
                    .timestamp(now)
                    .headers(Map.of("Content-Type", "application/json"))
                    .attributes(Map.of("statusCode", "201"))
                    .build();

            assertEquals("http://localhost/api", record.getSource());
            assertEquals("POST", record.getKey());
            assertEquals("{\"ok\":true}", record.getValue());
            assertEquals(now, record.getTimestamp());
            assertEquals("application/json", record.getHeaders().get("Content-Type"));
            assertEquals("201", record.getAttributes().get("statusCode"));
        }

        @Test
        @DisplayName("omitting attributes yields an empty map")
        void omittedAttributesAreEmpty() {
            ConsumedRecord<String> record = ConsumedRecord.<String>builder()
                    .source("topic")
                    .value("value")
                    .timestamp(Instant.now())
                    .headers(Collections.emptyMap())
                    .build();

            assertTrue(record.getAttributes().isEmpty());
        }
    }

    // =========================================================================
    // Kafka factory
    // =========================================================================

    @Nested
    @DisplayName("fromKafkaRecord")
    class FromKafkaRecord {

        @Test
        @DisplayName("maps Kafka coordinates and leaves attributes empty")
        void mapsKafkaRecord() {
            RecordHeaders headers = new RecordHeaders();
            headers.add("trace-id", "abc".getBytes(StandardCharsets.UTF_8));

            ConsumerRecord<String, String> kafkaRecord = new ConsumerRecord<>(
                    "orders",
                    2,
                    17L,
                    1_700_000_000_000L,
                    org.apache.kafka.common.record.TimestampType.CREATE_TIME,
                    0,
                    0,
                    "ORD-001",
                    "{\"id\":1}",
                    headers,
                    java.util.Optional.empty());

            ConsumedRecord<String> record = ConsumedRecord.fromKafkaRecord(kafkaRecord);

            assertEquals("orders", record.getSource());
            assertEquals(2, record.getPartition());
            assertEquals(17L, record.getOffset());
            assertEquals("ORD-001", record.getKey());
            assertEquals("{\"id\":1}", record.getValue());
            assertEquals("abc", record.getHeaders().get("trace-id"));
            assertTrue(record.getAttributes().isEmpty());
        }
    }

    // =========================================================================
    // toMatchedRecord
    // =========================================================================

    @Nested
    @DisplayName("toMatchedRecord")
    class ToMatchedRecord {

        @Test
        @DisplayName("copies the record coordinates")
        void copiesCoordinates() {
            Instant now = Instant.now();
            ConsumedRecord<String> record =
                    new ConsumedRecord<>("topic", 1, 5L, "key", "value", now, Collections.emptyMap());

            MatchedRecord matched = record.toMatchedRecord();

            assertEquals("topic", matched.getTopic());
            assertEquals(1, matched.getPartition());
            assertEquals(5L, matched.getOffset());
            assertEquals("key", matched.getKey());
        }
    }
}
