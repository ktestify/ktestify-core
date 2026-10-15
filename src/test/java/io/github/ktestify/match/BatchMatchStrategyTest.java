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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link BatchMatchStrategy}.
 *
 * @since 1.2.5
 */
@DisplayName("BatchMatchStrategy")
class BatchMatchStrategyTest {

    @Nested
    @DisplayName("fromString parsing")
    class FromStringParsing {

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "positional",
                    "POSITIONAL",
                    "Positional",
                    "ordered",
                    "ORDERED",
                    "index",
                    "INDEX",
                    "strict",
                    "STRICT",
                    " positional "
                })
        @DisplayName("parses positional and its aliases")
        void parsesPositional(String input) {
            assertEquals(BatchMatchStrategy.POSITIONAL, BatchMatchStrategy.fromString(input));
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "lazy",
                    "LAZY",
                    "Lazy",
                    "fuzzy",
                    "FUZZY",
                    "unordered",
                    "UNORDERED",
                    "any_order",
                    "any-order",
                    "ANY-ORDER",
                    "ANY ORDER",
                    " lazy "
                })
        @DisplayName("parses lazy and its aliases including hyphens and spaces")
        void parsesLazy(String input) {
            assertEquals(BatchMatchStrategy.LAZY, BatchMatchStrategy.fromString(input));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "unknown", "invalid-strategy"})
        @DisplayName("defaults to POSITIONAL for null, blank, or unrecognized values")
        void defaultsToPositionalForUnrecognized(String input) {
            assertEquals(BatchMatchStrategy.POSITIONAL, BatchMatchStrategy.fromString(input));
        }
    }
}
