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
package io.github.ktestify.match;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MatchResult")
class MatchResultTest {

    @Nested
    @DisplayName("pass()")
    class Pass {

        @Test
        @DisplayName("isPassed returns true")
        void isPassed() {
            assertTrue(MatchResult.pass().isPassed());
        }

        @Test
        @DisplayName("diff is empty")
        void diffIsEmpty() {
            assertTrue(MatchResult.pass().getDiff().isEmpty());
        }

        @Test
        @DisplayName("expected and actual are empty")
        void expectedAndActualEmpty() {
            MatchResult r = MatchResult.pass();
            assertTrue(r.getExpected().isEmpty());
            assertTrue(r.getActual().isEmpty());
        }
    }

    @Nested
    @DisplayName("pass(expected, actual)")
    class PassWithValues {

        @Test
        @DisplayName("isPassed returns true")
        void isPassed() {
            assertTrue(MatchResult.pass("exp", "act").isPassed());
        }

        @Test
        @DisplayName("carries expected and actual values")
        void carriesValues() {
            MatchResult r = MatchResult.pass("exp", "act");
            assertEquals("exp", r.getExpected());
            assertEquals("act", r.getActual());
        }

        @Test
        @DisplayName("diff is empty on a pass")
        void diffIsEmpty() {
            assertTrue(MatchResult.pass("exp", "act").getDiff().isEmpty());
        }
    }

    @Nested
    @DisplayName("fail(diff, expected, actual)")
    class FailWithValues {

        @Test
        @DisplayName("isPassed returns false")
        void isNotPassed() {
            assertFalse(MatchResult.fail("diff msg", "exp", "act").isPassed());
        }

        @Test
        @DisplayName("carries diff, expected and actual")
        void carriesAllFields() {
            MatchResult r = MatchResult.fail("diff msg", "exp", "act");
            assertEquals("diff msg", r.getDiff());
            assertEquals("exp", r.getExpected());
            assertEquals("act", r.getActual());
        }
    }

    @Nested
    @DisplayName("fail(message)")
    class FailMessageOnly {

        @Test
        @DisplayName("isPassed returns false")
        void isNotPassed() {
            assertFalse(MatchResult.fail("something went wrong").isPassed());
        }

        @Test
        @DisplayName("message is stored in diff field")
        void messageStoredInDiff() {
            assertEquals(
                    "something went wrong",
                    MatchResult.fail("something went wrong").getDiff());
        }

        @Test
        @DisplayName("expected and actual are empty")
        void expectedAndActualEmpty() {
            MatchResult r = MatchResult.fail("something went wrong");
            assertTrue(r.getExpected().isEmpty());
            assertTrue(r.getActual().isEmpty());
        }
    }
}
