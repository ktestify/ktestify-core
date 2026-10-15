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
package io.github.ktestify.match.impl;

import static io.github.ktestify.match.impl.MatcherTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import io.github.ktestify.exceptions.ComparisonException;
import io.github.ktestify.match.BatchMatchStrategy;
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import io.github.ktestify.models.ConsumedRecord;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AvroFileRecordMatcher")
class AvroFileRecordMatcherTest {

    private final AvroFileRecordMatcher matcher = new AvroFileRecordMatcher();

    private static final Schema ORDER_SCHEMA = SchemaBuilder.record("Order")
            .fields()
            .requiredString("orderId")
            .requiredString("customerId")
            .requiredDouble("amount")
            .requiredString("status")
            .endRecord();

    private static final List<String> BATCH_FILES = List.of(
            resourcePath("batch-order-1.json"), resourcePath("batch-order-2.json"), resourcePath("batch-order-3.json"));

    private GenericRecord buildOrder(String orderId, String customerId, double amount, String status) {
        GenericRecord record = new GenericData.Record(ORDER_SCHEMA);
        record.put("orderId", orderId);
        record.put("customerId", customerId);
        record.put("amount", amount);
        record.put("status", status);
        return record;
    }

    @SafeVarargs
    private static List<ConsumedRecord<GenericRecord>> avroRecords(GenericRecord... records) {
        return java.util.Arrays.stream(records)
                .map(r ->
                        new ConsumedRecord<>("test-topic", 0, 0L, "test-key", r, Instant.now(), Collections.emptyMap()))
                .toList();
    }

    /** Builds a GenericRecord that matches expected-order.json */
    private GenericRecord matchingRecord() {
        return buildOrder("ORD-001", "CUST-42", 99.99, "CREATED");
    }

    /** Builds a GenericRecord that does NOT match expected-order.json */
    private GenericRecord differentRecord() {
        return buildOrder("ORD-002", "CUST-99", 1.00, "CANCELLED");
    }

    @Nested
    @DisplayName("Passing scenarios")
    class Passing {

        @Test
        @DisplayName("passes when Avro record value matches expected JSON file")
        void matchesFile() throws ComparisonException {
            MatchResult result = matcher.match(avroRecord("key", matchingRecord()), ctxWithFile("expected-order.json"));
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("passes when differing field is in the exclusion list")
        void passesWithExclusion() throws ComparisonException {
            MatchResult result = matcher.match(
                    avroRecord("key", differentRecord()),
                    ctxWithFileAndExclusions(
                            "expected-order.json", List.of("orderId", "customerId", "amount", "status")));
            assertTrue(result.isPassed());
        }
    }

    @Nested
    @DisplayName("Failing scenarios")
    class Failing {

        @Test
        @DisplayName("fails when Avro record value differs from expected JSON file")
        void doesNotMatchFile() throws ComparisonException {
            MatchResult result =
                    matcher.match(avroRecord("key", differentRecord()), ctxWithFile("expected-order.json"));
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("throws ComparisonException when matchFilePath is missing")
        void throwsWhenNoFilePath() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            avroRecord("key", matchingRecord()),
                            MatchContext.builder().build()));
        }
    }

    @Nested
    @DisplayName("Batch scenarios")
    class BatchScenarios {

        private final GenericRecord r1 = buildOrder("ORD-001", "CUST-42", 99.99, "CREATED");
        private final GenericRecord r2 = buildOrder("ORD-002", "CUST-43", 49.99, "PENDING");
        private final GenericRecord r3 = buildOrder("ORD-003", "CUST-44", 19.99, "SHIPPED");

        @Test
        @DisplayName("passes in POSITIONAL mode when 3 Avro records match 3 files in order")
        void batchPositionalPass() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .batchMatchStrategy(BatchMatchStrategy.POSITIONAL)
                    .build();
            MatchResult result = matcher.match(avroRecords(r1, r2, r3), ctx);
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("fails in POSITIONAL mode when record 2 and record 3 are swapped")
        void batchPositionalFailWhenSwapped() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .batchMatchStrategy(BatchMatchStrategy.POSITIONAL)
                    .build();
            MatchResult result = matcher.match(avroRecords(r1, r3, r2), ctx);
            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("Avro record at index 1"));
        }

        @Test
        @DisplayName("passes in LAZY mode when Avro records are out of order")
        void batchLazyPassOutOfOrder() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .batchMatchStrategy(BatchMatchStrategy.LAZY)
                    .build();
            MatchResult result = matcher.match(avroRecords(r3, r1, r2), ctx);
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("fails in LAZY mode when duplicate Avro record cannot match remaining files")
        void batchLazyFailOnDuplicateRecord() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .batchMatchStrategy(BatchMatchStrategy.LAZY)
                    .build();
            MatchResult result = matcher.match(avroRecords(r1, r1, r2), ctx);
            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("Record at index 1 did not match any remaining expected file"));
        }

        @Test
        @DisplayName("throws ComparisonException when records count does not equal matchFilePaths count")
        void throwsWhenRecordCountMismatchesFileCount() {
            MatchContext ctx =
                    MatchContext.builder().matchFilePaths(BATCH_FILES).build();
            assertThrows(ComparisonException.class, () -> matcher.match(avroRecords(r1, r2), ctx));
        }
    }
}
