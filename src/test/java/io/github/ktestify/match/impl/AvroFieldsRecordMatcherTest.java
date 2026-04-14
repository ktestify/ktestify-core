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
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AvroFieldsRecordMatcher")
class AvroFieldsRecordMatcherTest {

    private final AvroFieldsRecordMatcher matcher = new AvroFieldsRecordMatcher();

    private static final Schema ORDER_SCHEMA = SchemaBuilder.record("Order")
            .fields()
            .requiredString("orderId")
            .requiredString("customerId")
            .requiredDouble("amount")
            .requiredString("status")
            .endRecord();

    private GenericRecord buildRecord(String orderId, String customerId, double amount, String status) {
        GenericRecord r = new GenericData.Record(ORDER_SCHEMA);
        r.put("orderId", orderId);
        r.put("customerId", customerId);
        r.put("amount", amount);
        r.put("status", status);
        return r;
    }

    @Nested
    @DisplayName("Inline value matching")
    class InlineValue {

        @Test
        @DisplayName("passes when the specified field matches the inline expected value")
        void fieldMatchesInlineValue() throws ComparisonException {
            MatchResult result = matcher.match(
                    avroRecord("key", buildRecord("ORD-001", "CUST-42", 99.99, "CREATED")),
                    ctxWithKeyAndValue("status", "CREATED"));
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("fails when the specified field does not match the inline expected value")
        void fieldDoesNotMatchInlineValue() throws ComparisonException {
            MatchResult result = matcher.match(
                    avroRecord("key", buildRecord("ORD-001", "CUST-42", 99.99, "SHIPPED")),
                    ctxWithKeyAndValue("status", "CREATED"));
            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("status"));
            assertTrue(result.getDiff().contains("CREATED"));
            assertEquals("CREATED", result.getExpected());
        }
    }

    @Nested
    @DisplayName("File-based matching")
    class FileBased {

        @Test
        @DisplayName("passes when specified field matches same field value in expected file")
        void fieldMatchesFileValue() throws ComparisonException {
            // orderId in the record matches orderId in expected-order.json
            MatchContext ctx = MatchContext.builder()
                    .matchKey("orderId")
                    .matchFilePath(resourcePath("expected-order.json"))
                    .build();
            MatchResult result =
                    matcher.match(avroRecord("key", buildRecord("ORD-001", "CUST-42", 99.99, "CREATED")), ctx);
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("fails when specified field differs from same field in expected file")
        void fieldDoesNotMatchFileValue() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchKey("orderId")
                    .matchFilePath(resourcePath("expected-order.json"))
                    .build();
            MatchResult result =
                    matcher.match(avroRecord("key", buildRecord("ORD-999", "CUST-42", 99.99, "CREATED")), ctx);
            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("orderId"));
        }
    }

    @Nested
    @DisplayName("Configuration errors")
    class ConfigErrors {

        @Test
        @DisplayName("throws ComparisonException when matchKey is not set")
        void throwsWhenNoMatchKey() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            avroRecord("key", buildRecord("ORD-001", "CUST-42", 99.99, "CREATED")),
                            MatchContext.builder().matchValue("CREATED").build()));
        }

        @Test
        @DisplayName("throws ComparisonException when neither matchValue nor matchFilePath is set")
        void throwsWhenNeitherValueNorFile() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            avroRecord("key", buildRecord("ORD-001", "CUST-42", 99.99, "CREATED")),
                            ctxWithKey("status")));
        }
    }
}
