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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.github.ktestify.match.impl.MatcherTestSupport.ctxWithAttributes;
import static io.github.ktestify.match.impl.MatcherTestSupport.rawRecordWithAttributes;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AttributeRecordMatcher")
class AttributeRecordMatcherTest {

    private final AttributeRecordMatcher<String> matcher = new AttributeRecordMatcher<>();

    // =========================================================================
    // Passing cases
    // =========================================================================

    @Nested
    @DisplayName("Passing cases")
    class Passing {

        @Test
        @DisplayName("single expected attribute matching the actual value passes")
        void singleAttributeMatches() {
            MatchResult result = matcher.match(
                    rawRecordWithAttributes("body", Map.of("statusCode", "200")),
                    ctxWithAttributes(Map.of("statusCode", "200")));

            assertTrue(result.isPassed());
            assertEquals("", result.getDiff());
        }

        @Test
        @DisplayName("every expected attribute must match, extra actual attributes are tolerated")
        void extraActualAttributesTolerated() {
            MatchResult result = matcher.match(
                    rawRecordWithAttributes("body", Map.of("statusCode", "201", "elapsedMs", "42")),
                    ctxWithAttributes(Map.of("statusCode", "201")));

            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("empty expectations pass without inspecting the record")
        void emptyExpectationsPass() {
            MatchResult result = matcher.match(
                    rawRecordWithAttributes("body", Collections.emptyMap()), ctxWithAttributes(Collections.emptyMap()));

            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("default MatchContext (no expectedAttributes set) passes")
        void defaultContextPasses() {
            MatchResult result = matcher.match(
                    rawRecordWithAttributes("body", Map.of("statusCode", "500")),
                    MatchContext.builder().build());

            assertTrue(result.isPassed());
        }
    }

    // =========================================================================
    // Failing cases
    // =========================================================================

    @Nested
    @DisplayName("Failing cases")
    class Failing {

        @Test
        @DisplayName("value mismatch fails and reports expected vs actual")
        void valueMismatchFails() {
            MatchResult result = matcher.match(
                    rawRecordWithAttributes("body", Map.of("statusCode", "500")),
                    ctxWithAttributes(Map.of("statusCode", "200")));

            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("statusCode"));
            assertTrue(result.getDiff().contains("200"));
            assertTrue(result.getDiff().contains("500"));
        }

        @Test
        @DisplayName("missing key fails and reports a null actual value")
        void missingKeyFails() {
            MatchResult result = matcher.match(
                    rawRecordWithAttributes("body", Map.of("elapsedMs", "42")),
                    ctxWithAttributes(Map.of("statusCode", "200")));

            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("statusCode"));
            assertTrue(result.getDiff().contains("null"));
        }

        @Test
        @DisplayName("comparison is case-sensitive")
        void caseSensitive() {
            MatchResult result = matcher.match(
                    rawRecordWithAttributes("body", Map.of("status", "OK")), ctxWithAttributes(Map.of("status", "ok")));

            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("multiple mismatches are all reported")
        void multipleMismatchesReported() {
            MatchResult result = matcher.match(
                    rawRecordWithAttributes("body", Map.of("statusCode", "500", "elapsedMs", "9")),
                    ctxWithAttributes(Map.of("statusCode", "200", "elapsedMs", "42")));

            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("statusCode"));
            assertTrue(result.getDiff().contains("elapsedMs"));
        }
    }

    // =========================================================================
    // Misconfiguration
    // =========================================================================

    @Nested
    @DisplayName("Misconfiguration")
    class Misconfiguration {

        @Test
        @DisplayName("fails with noRecords when there is no record to inspect, like every other matcher")
        void failsWithoutRecords() throws ComparisonException {
            MatchContext context = ctxWithAttributes(Map.of("statusCode", "200"));
            MatchResult result = matcher.match(List.of(), context);
            assertFalse(result.isPassed());
            assertEquals(MatchResult.noRecords().getDiff(), result.getDiff());
        }
    }
}
