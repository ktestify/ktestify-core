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
}
