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
import io.github.ktestify.match.MatchResult;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AvroFileKeyRecordMatcher")
class AvroFileKeyRecordMatcherTest {

    private final AvroFileKeyRecordMatcher matcher = new AvroFileKeyRecordMatcher();

    private static final Schema ORDER_SCHEMA = SchemaBuilder.record("Order")
            .fields()
            .requiredString("orderId")
            .requiredString("customerId")
            .requiredDouble("amount")
            .requiredString("status")
            .endRecord();

    private static final String EXPECTED_KEY = "ORDER-KEY-1";

    private GenericRecord matchingRecord() {
        GenericRecord r = new GenericData.Record(ORDER_SCHEMA);
        r.put("orderId", "ORD-001");
        r.put("customerId", "CUST-42");
        r.put("amount", 99.99);
        r.put("status", "CREATED");
        return r;
    }

    private GenericRecord differentRecord() {
        GenericRecord r = new GenericData.Record(ORDER_SCHEMA);
        r.put("orderId", "ORD-999");
        r.put("customerId", "CUST-00");
        r.put("amount", 0.01);
        r.put("status", "FAILED");
        return r;
    }

    @Nested
    @DisplayName("Passing scenarios")
    class Passing {

        @Test
        @DisplayName("passes when both Avro key and value match")
        void keyAndValueMatch() throws ComparisonException {
            MatchResult result = matcher.match(
                    avroRecord(EXPECTED_KEY, matchingRecord()), ctxWithFileAndKey("expected-order.json", EXPECTED_KEY));
            assertTrue(result.isPassed());
        }
    }

    @Nested
    @DisplayName("Failing scenarios")
    class Failing {

        @Test
        @DisplayName("fails when key matches but Avro value does not")
        void keyMatchesValueDoesNot() throws ComparisonException {
            MatchResult result = matcher.match(
                    avroRecord(EXPECTED_KEY, differentRecord()),
                    ctxWithFileAndKey("expected-order.json", EXPECTED_KEY));
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("fails when value matches but key does not")
        void valueMatchesKeyDoesNot() throws ComparisonException {
            MatchResult result = matcher.match(
                    avroRecord("WRONG-KEY", matchingRecord()), ctxWithFileAndKey("expected-order.json", EXPECTED_KEY));
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("throws ComparisonException when matchKey is missing")
        void throwsWhenNoMatchKey() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            avroRecord(EXPECTED_KEY, matchingRecord()), ctxWithFile("expected-order.json")));
        }

        @Test
        @DisplayName("throws ComparisonException when matchFilePath is missing")
        void throwsWhenNoFilePath() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(avroRecord(EXPECTED_KEY, matchingRecord()), ctxWithKey(EXPECTED_KEY)));
        }
    }
}
