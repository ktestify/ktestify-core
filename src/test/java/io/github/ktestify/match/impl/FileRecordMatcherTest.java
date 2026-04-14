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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FileRecordMatcher")
class FileRecordMatcherTest {

    private final FileRecordMatcher matcher = new FileRecordMatcher();

    // Loaded via FileUtils so it matches exactly what the matcher reads (trailing newline included)
    private static final String EXPECTED_CONTENT = fileContent("expected-order.json");

    @Nested
    @DisplayName("Passing scenarios")
    class Passing {

        @Test
        @DisplayName("passes when record value exactly matches the expected file")
        void matchesFile() throws ComparisonException {
            MatchResult result = matcher.match(rawRecord(EXPECTED_CONTENT), ctxWithFile("expected-order.json"));
            assertTrue(result.isPassed());
        }
    }

    @Nested
    @DisplayName("Failing scenarios")
    class Failing {

        @Test
        @DisplayName("fails when record value differs from the expected file")
        void doesNotMatchFile() throws ComparisonException {
            MatchResult result = matcher.match(rawRecord("different content"), ctxWithFile("expected-order.json"));
            assertFalse(result.isPassed());
            assertFalse(result.getDiff().isEmpty());
        }

        @Test
        @DisplayName("throws ComparisonException when matchFilePath is not set")
        void throwsWhenNoFilePath() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            rawRecord("value"), MatchContext.builder().build()));
        }

        @Test
        @DisplayName("throws ComparisonException when matchFilePath is blank")
        void throwsWhenBlankFilePath() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            rawRecord("value"),
                            MatchContext.builder().matchFilePath("  ").build()));
        }
    }

    @Nested
    @DisplayName("MatchResult content")
    class ResultContent {

        @Test
        @DisplayName("result carries expected and actual on failure")
        void resultCarriesValues() throws ComparisonException {
            MatchResult result = matcher.match(rawRecord("wrong"), ctxWithFile("expected-order.json"));
            assertFalse(result.getExpected().isEmpty());
            assertFalse(result.getActual().isEmpty());
        }

        @Test
        @DisplayName("diff is empty on success")
        void diffEmptyOnSuccess() throws ComparisonException {
            MatchResult result = matcher.match(rawRecord(EXPECTED_CONTENT), ctxWithFile("expected-order.json"));
            assertTrue(result.getDiff().isEmpty());
        }
    }
}
