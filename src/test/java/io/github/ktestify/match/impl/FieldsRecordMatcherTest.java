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
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FieldsRecordMatcher")
class FieldsRecordMatcherTest {

    private final FieldsRecordMatcher matcher = new FieldsRecordMatcher();

    // positional-record.txt line 0: "REF001 ACME Corp    00099.99CREATED"
    // chars 0-6 = "REF001 "  →  chars 0-6
    // chars 7-16 = "ACME Corp " →  7-16
    private static final String RECORD_LINE_0 = "REF001 ACME Corp    00099.99CREATED";
    private static final String RECORD_CONTENT = RECORD_LINE_0 + "\nREF002 Other Corp   00050.00PENDING";

    // "line:from:to" descriptor for chars 0-6 on line 0 → "REF001"
    private static final String POS_0_0_6 = "0:0:6";

    @Nested
    @DisplayName("Inline value matching")
    class InlineValue {

        @Test
        @DisplayName("passes when extracted field matches inline expected value")
        void matchesInlineValue() throws ComparisonException {
            MatchContext ctx = ctxWithKeyAndValue(POS_0_0_6, "REF001");
            MatchResult result = matcher.match(rawRecord(RECORD_CONTENT), ctx);
            assertTrue(result.isPassed());
            assertEquals("REF001", result.getExpected());
            assertEquals("REF001", result.getActual());
        }

        @Test
        @DisplayName("fails when extracted field does not match inline expected value")
        void doesNotMatchInlineValue() throws ComparisonException {
            MatchContext ctx = ctxWithKeyAndValue(POS_0_0_6, "WRONG!");
            MatchResult result = matcher.match(rawRecord(RECORD_CONTENT), ctx);
            assertFalse(result.isPassed());
            // diff must describe exactly which position failed
            assertTrue(result.getDiff().contains("line=0"));
            assertTrue(result.getDiff().contains("from=0"));
            assertTrue(result.getDiff().contains("to=6"));
            assertEquals("WRONG!", result.getExpected());
            assertEquals("REF001", result.getActual());
        }
    }

    @Nested
    @DisplayName("File-based matching")
    class FileBased {

        @Test
        @DisplayName("passes when extracted field matches same position in expected file")
        void matchesFileField() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchKey(POS_0_0_6)
                    .matchFilePath(resourcePath("positional-record.txt"))
                    .build();
            MatchResult result = matcher.match(rawRecord(RECORD_CONTENT), ctx);
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("fails when extracted field differs from same position in expected file")
        void doesNotMatchFileField() throws ComparisonException {
            // Record has "REF002" on line 1 chars 0-6; expected file has "REF001" on line 0 chars 0-6
            // Use line 0 for actual but a record whose line 0 has different content
            String differentRecord = "XXXXXX ACME Corp    00099.99CREATED\nREF002 Other Corp   00050.00PENDING";
            MatchContext ctx = MatchContext.builder()
                    .matchKey(POS_0_0_6) // line 0, chars 0-6 → "XXXXXX" vs file's "REF001"
                    .matchFilePath(resourcePath("positional-record.txt"))
                    .build();
            MatchResult result = matcher.match(rawRecord(differentRecord), ctx);
            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("line=0"));
            assertEquals("REF001", result.getExpected());
            assertEquals("XXXXXX", result.getActual());
        }
    }

    @Nested
    @DisplayName("Configuration errors")
    class ConfigErrors {

        @Test
        @DisplayName("throws ComparisonException when matchKey (position descriptor) is missing")
        void throwsWhenNoMatchKey() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            rawRecord(RECORD_CONTENT),
                            MatchContext.builder().matchValue("REF001").build()));
        }

        @Test
        @DisplayName("throws ComparisonException when position descriptor has wrong number of parts")
        void throwsWhenBadDescriptor() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            rawRecord(RECORD_CONTENT),
                            MatchContext.builder()
                                    .matchKey("not-valid")
                                    .matchValue("X")
                                    .build()));
        }

        @Test
        @DisplayName("throws ComparisonException when position descriptor contains non-integer values")
        void throwsWhenNonIntegerDescriptor() {
            // 3 parts but second is not a number → triggers NumberFormatException path
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            rawRecord(RECORD_CONTENT),
                            MatchContext.builder()
                                    .matchKey("0:abc:6")
                                    .matchValue("X")
                                    .build()));
        }

        @Test
        @DisplayName("throws ComparisonException when neither matchValue nor matchFilePath is set")
        void throwsWhenNeitherValueNorFile() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            rawRecord(RECORD_CONTENT),
                            MatchContext.builder().matchKey(POS_0_0_6).build()));
        }
    }
}
