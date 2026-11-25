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

import static io.github.ktestify.match.impl.MatcherTestSupport.avroRecord;
import static io.github.ktestify.match.impl.MatcherTestSupport.rawRecord;
import static org.junit.jupiter.api.Assertions.*;

import io.github.ktestify.exceptions.ComparisonException;
import io.github.ktestify.match.KeyMatchStrategy;
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TombstoneRecordMatcher")
class TombstoneRecordMatcherTest {

    private final TombstoneRecordMatcher<String> rawMatcher = new TombstoneRecordMatcher<>();
    private final TombstoneRecordMatcher<GenericRecord> avroMatcher = new TombstoneRecordMatcher<>();

    @Test
    @DisplayName("passes for raw tombstone with no key constraint")
    void passesForRawTombstoneWithoutKeyConstraint() throws ComparisonException {
        MatchResult result = rawMatcher.match(
                rawRecord("order-1", null), MatchContext.builder().build());
        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("passes for Avro tombstone with matching key")
    void passesForAvroTombstoneWithMatchingKey() throws ComparisonException {
        MatchContext ctx = MatchContext.builder().matchKey("order-1").build();
        MatchResult result = avroMatcher.match(avroRecord("order-1", null), ctx);
        assertTrue(result.isPassed());
        assertEquals("order-1", result.getActual());
    }

    @Test
    @DisplayName("passes when key matches using KeyMatchStrategy")
    void passesWithKeyMatchStrategy() throws ComparisonException {
        MatchContext ctx = MatchContext.builder()
                .matchKey("order-")
                .keyMatchStrategy(KeyMatchStrategy.STARTS_WITH)
                .build();
        MatchResult result = rawMatcher.match(rawRecord("order-999", null), ctx);
        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("fails when raw record value is non-null")
    void failsForNonNullRawValue() throws ComparisonException {
        MatchContext ctx = MatchContext.builder().matchKey("order-1").build();
        MatchResult result = rawMatcher.match(rawRecord("order-1", "{\"status\":\"ACTIVE\"}"), ctx);
        assertFalse(result.isPassed());
        assertTrue(result.getDiff().contains("non-null value"), result.getDiff());
        assertEquals("{\"status\":\"ACTIVE\"}", result.getActual());
    }

    @Test
    @DisplayName("fails when Avro record value is non-null")
    void failsForNonNullAvroValue() throws ComparisonException {
        Schema schema = new Schema.Parser()
                .parse("{\"type\":\"record\",\"name\":\"Order\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"}]}");
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", "1");

        MatchResult result = avroMatcher.match(
                avroRecord("order-1", record), MatchContext.builder().build());
        assertFalse(result.isPassed());
        assertTrue(result.getDiff().contains("non-null value"), result.getDiff());
    }

    @Test
    @DisplayName("fails when tombstone key does not match expected key")
    void failsOnKeyMismatch() throws ComparisonException {
        MatchContext ctx = MatchContext.builder().matchKey("order-1").build();
        MatchResult result = rawMatcher.match(rawRecord("order-2", null), ctx);
        assertFalse(result.isPassed());
        assertTrue(result.getDiff().contains("order-1"), result.getDiff());
        assertTrue(result.getDiff().contains("order-2"), result.getDiff());
    }

    @Test
    @DisplayName("fails when tombstone key is null and expected key is set")
    void failsWhenActualKeyIsNullAndExpectedKeySet() throws ComparisonException {
        MatchContext ctx = MatchContext.builder().matchKey("order-1").build();
        MatchResult result = rawMatcher.match(rawRecord(null, null), ctx);
        assertFalse(result.isPassed());
    }

    @Test
    @DisplayName("returns noRecords for empty or null list")
    void returnsNoRecordsForEmptyOrNull() throws ComparisonException {
        MatchContext ctx = MatchContext.builder().build();
        assertFalse(rawMatcher.match(List.of(), ctx).isPassed());
        assertFalse(rawMatcher.match(null, ctx).isPassed());
    }
}
