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
package io.github.ktestify.match.impl;

import static io.github.ktestify.match.impl.MatcherTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import io.github.ktestify.exceptions.ComparisonException;
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
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

    /** Builds a GenericRecord that matches expected-order.json */
    private GenericRecord matchingRecord() {
        GenericRecord record = new GenericData.Record(ORDER_SCHEMA);
        record.put("orderId", "ORD-001");
        record.put("customerId", "CUST-42");
        record.put("amount", 99.99);
        record.put("status", "CREATED");
        return record;
    }

    /** Builds a GenericRecord that does NOT match expected-order.json */
    private GenericRecord differentRecord() {
        GenericRecord record = new GenericData.Record(ORDER_SCHEMA);
        record.put("orderId", "ORD-002");
        record.put("customerId", "CUST-99");
        record.put("amount", 1.00);
        record.put("status", "CANCELLED");
        return record;
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
}
