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
import io.github.ktestify.match.BatchMatchStrategy;
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import io.github.ktestify.models.ConsumedRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static io.github.ktestify.match.impl.MatcherTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileRecordMatcher")
class FileRecordMatcherTest {

    private final FileRecordMatcher matcher = new FileRecordMatcher();

    // Loaded via FileUtils so it matches exactly what the matcher reads (trailing newline included)
    private static final String EXPECTED_CONTENT = fileContent("expected-order.json");
    private static final String BATCH_CONTENT_1 = fileContent("batch-order-1.json");
    private static final String BATCH_CONTENT_2 = fileContent("batch-order-2.json");
    private static final String BATCH_CONTENT_3 = fileContent("batch-order-3.json");
    private static final List<String> BATCH_FILES = List.of(
            resourcePath("batch-order-1.json"), resourcePath("batch-order-2.json"), resourcePath("batch-order-3.json"));

    private static List<ConsumedRecord<String>> rawRecords(String... values) {
        return java.util.Arrays.stream(values)
                .map(v ->
                        new ConsumedRecord<>("test-topic", 0, 0L, "test-key", v, Instant.now(), Collections.emptyMap()))
                .toList();
    }

    private static ConsumedRecord<String> keyedRawRecord(String key, String value) {
        return new ConsumedRecord<>("test-topic", 0, 0L, key, value, Instant.now(), Collections.emptyMap());
    }

    @SafeVarargs
    private static List<ConsumedRecord<String>> keyedRawRecords(ConsumedRecord<String>... records) {
        return List.of(records);
    }

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

    @Nested
    @DisplayName("Batch scenarios")
    class BatchScenarios {

        @Test
        @DisplayName("passes in POSITIONAL mode when 3 records match 3 files in order")
        void batchPositionalPass() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .batchMatchStrategy(BatchMatchStrategy.POSITIONAL)
                    .build();
            MatchResult result = matcher.match(rawRecords(BATCH_CONTENT_1, BATCH_CONTENT_2, BATCH_CONTENT_3), ctx);
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("fails in POSITIONAL mode when record 2 and record 3 are swapped")
        void batchPositionalFailWhenSwapped() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .batchMatchStrategy(BatchMatchStrategy.POSITIONAL)
                    .build();
            MatchResult result = matcher.match(rawRecords(BATCH_CONTENT_1, BATCH_CONTENT_3, BATCH_CONTENT_2), ctx);
            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("Record at index 1"));
        }

        @Test
        @DisplayName("passes in LAZY mode when records are out of order")
        void batchLazyPassOutOfOrder() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .batchMatchStrategy(BatchMatchStrategy.LAZY)
                    .build();
            MatchResult result = matcher.match(rawRecords(BATCH_CONTENT_3, BATCH_CONTENT_1, BATCH_CONTENT_2), ctx);
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName(
                "fails in LAZY mode when duplicate record cannot match remaining files (1-to-1 without replacement)")
        void batchLazyFailOnDuplicateRecord() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .batchMatchStrategy(BatchMatchStrategy.LAZY)
                    .build();
            MatchResult result = matcher.match(rawRecords(BATCH_CONTENT_1, BATCH_CONTENT_1, BATCH_CONTENT_2), ctx);
            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("Record at index 1 did not match any remaining expected file"));
        }

        @Test
        @DisplayName("throws ComparisonException when records count does not equal matchFilePaths count")
        void throwsWhenRecordCountMismatchesFileCount() {
            MatchContext ctx =
                    MatchContext.builder().matchFilePaths(BATCH_FILES).build();
            assertThrows(
                    ComparisonException.class, () -> matcher.match(rawRecords(BATCH_CONTENT_1, BATCH_CONTENT_2), ctx));
        }

        @Test
        @DisplayName("passes in POSITIONAL mode when expectedRecordKeys (matchKeys) and files match in order")
        void batchPositionalWithExpectedRecordKeysPass() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .matchKeys(List.of("key-1", "key-2", "key-3"))
                    .batchMatchStrategy(BatchMatchStrategy.POSITIONAL)
                    .build();
            MatchResult result = matcher.match(
                    keyedRawRecords(
                            keyedRawRecord("key-1", BATCH_CONTENT_1),
                            keyedRawRecord("key-2", BATCH_CONTENT_2),
                            keyedRawRecord("key-3", BATCH_CONTENT_3)),
                    ctx);
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("fails in POSITIONAL mode when expectedRecordKeys (matchKeys) do not match at index")
        void batchPositionalWithExpectedRecordKeysFailOnKeyMismatch() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .matchKeys(List.of("key-1", "key-2", "key-3"))
                    .batchMatchStrategy(BatchMatchStrategy.POSITIONAL)
                    .build();
            MatchResult result = matcher.match(
                    keyedRawRecords(
                            keyedRawRecord("key-1", BATCH_CONTENT_1),
                            keyedRawRecord("wrong-key", BATCH_CONTENT_2),
                            keyedRawRecord("key-3", BATCH_CONTENT_3)),
                    ctx);
            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("Record at index 1 key does not match"));
        }

        @Test
        @DisplayName("passes in LAZY mode when expectedRecordKeys and files arrive out of order")
        void batchLazyWithExpectedRecordKeysPassOutOfOrder() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .matchKeys(List.of("key-1", "key-2", "key-3"))
                    .batchMatchStrategy(BatchMatchStrategy.LAZY)
                    .build();
            MatchResult result = matcher.match(
                    keyedRawRecords(
                            keyedRawRecord("key-3", BATCH_CONTENT_3),
                            keyedRawRecord("key-1", BATCH_CONTENT_1),
                            keyedRawRecord("key-2", BATCH_CONTENT_2)),
                    ctx);
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("fails in LAZY mode when value matches file j but key does not match key j")
        void batchLazyWithExpectedRecordKeysFailWhenKeyAndFilePairedWrong() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .matchKeys(List.of("key-1", "key-2", "key-3"))
                    .batchMatchStrategy(BatchMatchStrategy.LAZY)
                    .build();
            MatchResult result = matcher.match(
                    keyedRawRecords(
                            keyedRawRecord("key-2", BATCH_CONTENT_1),
                            keyedRawRecord("key-1", BATCH_CONTENT_2),
                            keyedRawRecord("key-3", BATCH_CONTENT_3)),
                    ctx);
            assertFalse(result.isPassed());
            assertTrue(result.getDiff().contains("key-1 ->"));
        }

        @Test
        @DisplayName("throws ComparisonException when matchKeys count does not equal matchFilePaths count")
        void throwsWhenMatchKeysCountMismatchesFileCount() {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePaths(BATCH_FILES)
                    .matchKeys(List.of("key-1", "key-2"))
                    .build();
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(rawRecords(BATCH_CONTENT_1, BATCH_CONTENT_2, BATCH_CONTENT_3), ctx));
        }
    }
}
