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
import io.github.ktestify.match.KeyMatchStrategy;
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.github.ktestify.match.impl.MatcherTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KeyRecordMatcher")
class KeyRecordMatcherTest {

    private final KeyRecordMatcher matcher = new KeyRecordMatcher();

    @Nested
    @DisplayName("Passing scenarios")
    class Passing {

        @Test
        @DisplayName("passes when record key matches expected key")
        void keyMatches() throws ComparisonException {
            MatchResult result = matcher.match(rawRecord("ORDER-1", "any-value"), ctxWithKey("ORDER-1"));
            assertTrue(result.isPassed());
        }
    }

    @Nested
    @DisplayName("Failing scenarios")
    class Failing {

        @Test
        @DisplayName("fails when record key differs from expected key")
        void keyDoesNotMatch() throws ComparisonException {
            MatchResult result = matcher.match(rawRecord("WRONG-KEY", "value"), ctxWithKey("ORDER-1"));
            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("ORDER-1"));
            assertTrue(result.getDiff().contains("WRONG-KEY"));
        }

        @Test
        @DisplayName("throws ComparisonException when matchKey is not set")
        void throwsWhenNoMatchKey() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            rawRecord("key", "value"), MatchContext.builder().build()));
        }

        @Test
        @DisplayName("throws ComparisonException when matchKey is blank")
        void throwsWhenBlankMatchKey() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            rawRecord("key", "value"),
                            MatchContext.builder().matchKey("   ").build()));
        }
    }

    @Nested
    @DisplayName("MatchResult content")
    class ResultContent {

        @Test
        @DisplayName("result carries expected and actual keys")
        void resultCarriesKeys() throws ComparisonException {
            MatchResult result = matcher.match(rawRecord("ACTUAL", "value"), ctxWithKey("EXPECTED"));
            assertEquals("EXPECTED", result.getExpected());
            assertEquals("ACTUAL", result.getActual());
        }
    }

    @Nested
    @DisplayName("Key match strategies")
    class KeyStrategies {

        @Test
        @DisplayName("STARTS_WITH passes when key starts with expected prefix")
        void startsWithPasses() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord("ORD-abc-123", "value"), ctxWithKeyAndStrategy("ORD-", KeyMatchStrategy.STARTS_WITH));
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("STARTS_WITH fails when key does not start with expected prefix")
        void startsWithFails() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord("USER-abc-123", "value"), ctxWithKeyAndStrategy("ORD-", KeyMatchStrategy.STARTS_WITH));
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("CONTAINS passes when key contains expected substring")
        void containsPasses() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord("order-ABC-123", "value"), ctxWithKeyAndStrategy("ABC", KeyMatchStrategy.CONTAINS));
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("CONTAINS fails when key does not contain expected substring")
        void containsFails() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord("order-XYZ-123", "value"), ctxWithKeyAndStrategy("ABC", KeyMatchStrategy.CONTAINS));
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("ENDS_WITH passes when key ends with expected suffix")
        void endsWithPasses() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord("ORD-abc-123", "value"), ctxWithKeyAndStrategy("-123", KeyMatchStrategy.ENDS_WITH));
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("ENDS_WITH fails when key does not end with expected suffix")
        void endsWithFails() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord("ORD-abc-456", "value"), ctxWithKeyAndStrategy("-123", KeyMatchStrategy.ENDS_WITH));
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("REGEX passes when key matches expected pattern")
        void regexPasses() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord("ORD-123456", "value"), ctxWithKeyAndStrategy("ORD-\\d{6}", KeyMatchStrategy.REGEX));
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("REGEX fails when key does not match expected pattern")
        void regexFails() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord("ORD-abc", "value"), ctxWithKeyAndStrategy("ORD-\\d{6}", KeyMatchStrategy.REGEX));
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("EXACT is the default when strategy is not set")
        void exactIsDefault() throws ComparisonException {
            MatchResult result = matcher.match(rawRecord("ORD-123", "value"), ctxWithKey("ORD-123"));
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("EXACT fails for partial match (backward compatibility)")
        void exactFailsForPartial() throws ComparisonException {
            MatchResult result = matcher.match(rawRecord("ORD-123", "value"), ctxWithKey("ORD"));
            assertFalse(result.isPassed());
        }
    }
}
