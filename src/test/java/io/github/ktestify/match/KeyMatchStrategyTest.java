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
package io.github.ktestify.match;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link KeyMatchStrategy}.
 *
 * @since 1.1.1
 */
@DisplayName("KeyMatchStrategy")
class KeyMatchStrategyTest {

    @Nested
    @DisplayName("EXACT strategy")
    class ExactStrategy {

        @ParameterizedTest
        @CsvSource({"ORDER-1, ORDER-1", "key-123, key-123", "'', ''"})
        @DisplayName("matches when keys are exactly equal")
        void matchesWhenEqual(String expected, String actual) {
            assertTrue(KeyMatchStrategy.EXACT.matches(expected, actual));
        }

        @Test
        @DisplayName("does not match when keys differ")
        void doesNotMatchWhenDifferent() {
            assertFalse(KeyMatchStrategy.EXACT.matches("ORDER-1", "ORDER-2"));
        }

        @Test
        @DisplayName("does not match when actual is null")
        void doesNotMatchWhenActualIsNull() {
            assertFalse(KeyMatchStrategy.EXACT.matches("ORDER-1", null));
        }

        @Test
        @DisplayName("does not match when expected is null")
        void doesNotMatchWhenExpectedIsNull() {
            assertFalse(KeyMatchStrategy.EXACT.matches(null, "ORDER-1"));
        }
    }

    @Nested
    @DisplayName("CONTAINS strategy")
    class ContainsStrategy {

        @ParameterizedTest
        @CsvSource({"ORD, ORDER-123", "123, ORDER-123", "ORDER, ORDER-123"})
        @DisplayName("matches when actual contains expected substring")
        void matchesWhenContains(String expected, String actual) {
            assertTrue(KeyMatchStrategy.CONTAINS.matches(expected, actual));
        }

        @Test
        @DisplayName("does not match when actual does not contain expected")
        void doesNotMatchWhenNotContains() {
            assertFalse(KeyMatchStrategy.CONTAINS.matches("XYZ", "ORDER-123"));
        }

        @Test
        @DisplayName("does not match when actual is null")
        void doesNotMatchWhenActualIsNull() {
            assertFalse(KeyMatchStrategy.CONTAINS.matches("ORD", null));
        }

        @Test
        @DisplayName("does not match when expected is null")
        void doesNotMatchWhenExpectedIsNull() {
            assertFalse(KeyMatchStrategy.CONTAINS.matches(null, "ORDER-123"));
        }
    }

    @Nested
    @DisplayName("STARTS_WITH strategy")
    class StartsWithStrategy {

        @ParameterizedTest
        @CsvSource({"ORD, ORDER-123", "ORDER-, ORDER-123", "ORDER-1, ORDER-123"})
        @DisplayName("matches when actual starts with expected prefix")
        void matchesWhenStartsWith(String expected, String actual) {
            assertTrue(KeyMatchStrategy.STARTS_WITH.matches(expected, actual));
        }

        @Test
        @DisplayName("does not match when actual does not start with expected")
        void doesNotMatchWhenNotStartsWith() {
            assertFalse(KeyMatchStrategy.STARTS_WITH.matches("USER-", "ORDER-123"));
        }

        @Test
        @DisplayName("does not match when actual is null")
        void doesNotMatchWhenActualIsNull() {
            assertFalse(KeyMatchStrategy.STARTS_WITH.matches("ORD", null));
        }

        @Test
        @DisplayName("does not match when expected is null")
        void doesNotMatchWhenExpectedIsNull() {
            assertFalse(KeyMatchStrategy.STARTS_WITH.matches(null, "ORDER-123"));
        }
    }

    @Nested
    @DisplayName("ENDS_WITH strategy")
    class EndsWithStrategy {

        @ParameterizedTest
        @CsvSource({"123, ORDER-123", "-123, ORDER-123", "R-123, ORDER-123"})
        @DisplayName("matches when actual ends with expected suffix")
        void matchesWhenEndsWith(String expected, String actual) {
            assertTrue(KeyMatchStrategy.ENDS_WITH.matches(expected, actual));
        }

        @Test
        @DisplayName("does not match when actual does not end with expected")
        void doesNotMatchWhenNotEndsWith() {
            assertFalse(KeyMatchStrategy.ENDS_WITH.matches("456", "ORDER-123"));
        }

        @Test
        @DisplayName("does not match when actual is null")
        void doesNotMatchWhenActualIsNull() {
            assertFalse(KeyMatchStrategy.ENDS_WITH.matches("123", null));
        }

        @Test
        @DisplayName("does not match when expected is null")
        void doesNotMatchWhenExpectedIsNull() {
            assertFalse(KeyMatchStrategy.ENDS_WITH.matches(null, "ORDER-123"));
        }
    }

    @Nested
    @DisplayName("REGEX strategy")
    class RegexStrategy {

        @ParameterizedTest
        @CsvSource({"ORD-\\d+, ORD-123", "ORD-\\d{3}, ORD-123", ".*123, ORDER-123"})
        @DisplayName("matches when actual matches expected regex pattern")
        void matchesWhenRegexMatches(String expected, String actual) {
            assertTrue(KeyMatchStrategy.REGEX.matches(expected, actual));
        }

        @Test
        @DisplayName("does not match when actual does not match regex")
        void doesNotMatchWhenRegexDoesNotMatch() {
            assertFalse(KeyMatchStrategy.REGEX.matches("ORD-\\d{6}", "ORDER-123"));
        }

        @Test
        @DisplayName("does not match when actual is null")
        void doesNotMatchWhenActualIsNull() {
            assertFalse(KeyMatchStrategy.REGEX.matches("ORD-\\d+", null));
        }

        @Test
        @DisplayName("does not match when expected is null")
        void doesNotMatchWhenExpectedIsNull() {
            assertFalse(KeyMatchStrategy.REGEX.matches(null, "ORDER-123"));
        }
    }

    @Nested
    @DisplayName("fromString parsing")
    class FromStringParsing {

        @ParameterizedTest
        @CsvSource({"exact, EXACT", "EXACT, EXACT", "Exact, EXACT"})
        @DisplayName("parses exact variations")
        void parsesExact(String input, KeyMatchStrategy expected) {
            assertEquals(expected, KeyMatchStrategy.fromString(input));
        }

        @ParameterizedTest
        @CsvSource({"contains, CONTAINS", "CONTAINS, CONTAINS"})
        @DisplayName("parses contains variations")
        void parsesContains(String input, KeyMatchStrategy expected) {
            assertEquals(expected, KeyMatchStrategy.fromString(input));
        }

        @ParameterizedTest
        @CsvSource({
            "starts_with, STARTS_WITH",
            "starts-with, STARTS_WITH",
            "STARTS-WITH, STARTS_WITH",
            "STARTS WITH, STARTS_WITH"
        })
        @DisplayName("parses starts_with variations including hyphens and spaces")
        void parsesStartsWith(String input, KeyMatchStrategy expected) {
            assertEquals(expected, KeyMatchStrategy.fromString(input));
        }

        @ParameterizedTest
        @CsvSource({"ends_with, ENDS_WITH", "ends-with, ENDS_WITH", "ENDS-WITH, ENDS_WITH", "ENDS WITH, ENDS_WITH"})
        @DisplayName("parses ends_with variations including hyphens and spaces")
        void parsesEndsWith(String input, KeyMatchStrategy expected) {
            assertEquals(expected, KeyMatchStrategy.fromString(input));
        }

        @ParameterizedTest
        @CsvSource({"regex, REGEX", "REGEX, REGEX"})
        @DisplayName("parses regex variations")
        void parsesRegex(String input, KeyMatchStrategy expected) {
            assertEquals(expected, KeyMatchStrategy.fromString(input));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "unknown", "invalid-strategy"})
        @DisplayName("defaults to EXACT for null, blank, or unrecognized values")
        void defaultsToExactForUnrecognized(String input) {
            assertEquals(KeyMatchStrategy.EXACT, KeyMatchStrategy.fromString(input));
        }
    }
}
