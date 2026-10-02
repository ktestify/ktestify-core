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

import io.github.ktestify.exceptions.ComparisonException;
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.github.ktestify.match.impl.MatcherTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that every matcher handles a tombstone (record with a {@code null} value) the same way: value matchers fail
 * cleanly without throwing, key-only and attribute matchers ignore the value.
 */
@DisplayName("Tombstone handling across all matchers")
class TombstoneMatcherTest {

    private static final String KEY = "test-key";

    private static void assertNullValueFailure(MatchResult result) {
        assertFalse(result.isPassed());
        assertTrue(result.getDiff().contains(MatchResult.NULL_VALUE_MESSAGE), result.getDiff());
    }

    @Nested
    @DisplayName("String value matchers fail with the tombstone message")
    class StringValueMatchers {

        @Test
        void fileRecordMatcher() throws ComparisonException {
            MatchResult result =
                    new FileRecordMatcher().match(rawRecord(KEY, null), ctxWithFile("expected-order.json"));
            assertNullValueFailure(result);
            assertNull(result.getActual());
        }

        @Test
        void fileKeyRecordMatcher() throws ComparisonException {
            MatchResult result = new FileKeyRecordMatcher()
                    .match(rawRecord(KEY, null), ctxWithFileAndKey("expected-order.json", KEY));
            assertNullValueFailure(result);
        }

        @Test
        void fieldsRecordMatcherInline() throws ComparisonException {
            MatchResult result =
                    new FieldsRecordMatcher().match(rawRecord(KEY, null), ctxWithKeyAndValue("0:0:3", "ABC"));
            assertNullValueFailure(result);
        }

        @Test
        void fieldsRecordMatcherFromFile() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchKey("0:0:3")
                    .matchFilePath(resourcePath("positional-record.txt"))
                    .build();
            assertNullValueFailure(new FieldsRecordMatcher().match(rawRecord(KEY, null), ctx));
        }

        @Test
        void xmlRecordMatcher() throws ComparisonException {
            assertNullValueFailure(
                    new XmlRecordMatcher().match(rawRecord(KEY, null), ctxWithFile("expected-order.xml")));
        }

        @Test
        void xPathRecordMatcher() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePath(resourcePath("expected-order.xml"))
                    .excludedFields(List.of("/order/id"))
                    .build();
            assertNullValueFailure(new XPathRecordMatcher().match(rawRecord(KEY, null), ctx));
        }
    }

    @Nested
    @DisplayName("Avro value matchers fail with the tombstone message")
    class AvroValueMatchers {

        @Test
        void avroFileRecordMatcher() throws ComparisonException {
            assertNullValueFailure(
                    new AvroFileRecordMatcher().match(avroRecord(KEY, null), ctxWithFile("expected-order.json")));
        }

        @Test
        void avroFileKeyRecordMatcher() throws ComparisonException {
            assertNullValueFailure(new AvroFileKeyRecordMatcher()
                    .match(avroRecord(KEY, null), ctxWithFileAndKey("expected-order.json", KEY)));
        }

        @Test
        void avroFieldsRecordMatcherInline() throws ComparisonException {
            assertNullValueFailure(
                    new AvroFieldsRecordMatcher().match(avroRecord(KEY, null), ctxWithKeyAndValue("orderId", "1")));
        }

        @Test
        void avroFieldsRecordMatcherFromFile() throws ComparisonException {
            assertNullValueFailure(new AvroFieldsRecordMatcher()
                    .match(avroRecord(KEY, null), ctxWithFileAndKey("expected-order.json", "orderId")));
        }

        @Test
        void avroFieldsRecordMatcherMultiField() throws ComparisonException {
            assertNullValueFailure(new AvroFieldsRecordMatcher()
                    .match(avroRecord(KEY, null), ctxWithKeyValues(Map.of("orderId", "1"))));
        }
    }

    @Nested
    @DisplayName("Matchers that never read the value accept a tombstone")
    class ValueAgnosticMatchers {

        @Test
        void keyRecordMatcher() throws ComparisonException {
            assertTrue(new KeyRecordMatcher()
                    .match(rawRecord(KEY, null), ctxWithKey(KEY))
                    .isPassed());
        }

        @Test
        void avroKeyRecordMatcher() throws ComparisonException {
            assertTrue(new AvroKeyRecordMatcher()
                    .match(avroRecord(KEY, null), ctxWithKey(KEY))
                    .isPassed());
        }

        @Test
        void noOpRecordMatcher() {
            assertTrue(new NoOpRecordMatcher<String>()
                    .match(rawRecord(KEY, null), MatchContext.builder().build())
                    .isPassed());
        }
    }

    @Nested
    @DisplayName("Every matcher returns noRecords for an empty or null list")
    class EmptyInput {

        @Test
        void noOpToleratesNullList() {
            assertTrue(new NoOpRecordMatcher<String>()
                    .match(null, MatchContext.builder().build())
                    .isPassed());
        }

        @Test
        void attributeMatcherReturnsNoRecords() throws ComparisonException {
            MatchResult result = new AttributeRecordMatcher<String>()
                    .match(
                            List.of(),
                            MatchContext.builder()
                                    .expectedAttributes(Map.of("statusCode", "200"))
                                    .build());
            assertFalse(result.isPassed());
        }
    }
}
