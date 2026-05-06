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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FileKeyRecordMatcher")
class FileKeyRecordMatcherTest {

    private final FileKeyRecordMatcher matcher = new FileKeyRecordMatcher();

    // Loaded via FileUtils so it matches exactly what the matcher reads (trailing newline included)
    private static final String EXPECTED_CONTENT = fileContent("expected-order.json");
    private static final String EXPECTED_KEY = "ORDER-KEY-1";

    @Nested
    @DisplayName("Passing scenarios")
    class Passing {

        @Test
        @DisplayName("passes when both key and value match")
        void keyAndValueMatch() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord(EXPECTED_KEY, EXPECTED_CONTENT), ctxWithFileAndKey("expected-order.json", EXPECTED_KEY));
            assertTrue(result.isPassed());
        }
    }

    @Nested
    @DisplayName("Failing scenarios")
    class Failing {

        @Test
        @DisplayName("fails when key matches but value does not")
        void keyMatchesValueDoesNot() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord(EXPECTED_KEY, "wrong value"), ctxWithFileAndKey("expected-order.json", EXPECTED_KEY));
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("fails when value matches but key does not")
        void valueMatchesKeyDoesNot() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord("WRONG-KEY", EXPECTED_CONTENT), ctxWithFileAndKey("expected-order.json", EXPECTED_KEY));
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("fails when both key and value differ")
        void neitherMatches() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord("WRONG-KEY", "wrong value"), ctxWithFileAndKey("expected-order.json", EXPECTED_KEY));
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("throws ComparisonException when matchKey is missing")
        void throwsWhenNoMatchKey() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(rawRecord(EXPECTED_CONTENT), ctxWithFile("expected-order.json")));
        }

        @Test
        @DisplayName("throws ComparisonException when matchFilePath is missing")
        void throwsWhenNoFilePath() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(rawRecord(EXPECTED_KEY, EXPECTED_CONTENT), ctxWithKey(EXPECTED_KEY)));
        }
    }
}
