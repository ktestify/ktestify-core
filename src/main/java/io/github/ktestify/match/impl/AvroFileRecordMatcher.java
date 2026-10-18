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
import io.github.ktestify.match.*;
import io.github.ktestify.models.ConsumedRecord;
import io.github.ktestify.utils.FileUtils;
import io.github.ktestify.utils.serdes.AvroUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Compares Avro record value(s) against the content of expected JSON file(s) using smart matching. Supports optional
 * field exclusion via {@link MatchContext#getExcludedFields()} and batch matching strategies via
 * {@link MatchContext#getBatchMatchStrategy()}.
 *
 * <p>Requires {@link MatchContext#getMatchFilePaths()} to be set. A record with a {@code null} value (tombstone) fails
 * with {@link MatchResult#nullValue(String)}.
 *
 * @since 0.3.0
 */
@Slf4j
public class AvroFileRecordMatcher implements RecordMatcher<GenericRecord> {

    @Override
    public MatchResult match(List<ConsumedRecord<GenericRecord>> records, MatchContext context)
            throws ComparisonException {
        if (records == null || records.isEmpty()) {
            return MatchResult.noRecords();
        }
        List<String> filePaths = context.getMatchFilePaths();
        if (filePaths == null || filePaths.isEmpty() || filePaths.stream().anyMatch(p -> p == null || p.isBlank())) {
            throw new ComparisonException("AvroFileRecordMatcher requires matchFilePath(s) to be set.");
        }
        List<String> expectedKeys = context.getMatchKeys() != null ? context.getMatchKeys() : List.of();
        if (!expectedKeys.isEmpty() && expectedKeys.size() != filePaths.size()) {
            throw new ComparisonException("Expected record keys count (" + expectedKeys.size()
                    + ") does not match expected file count (" + filePaths.size() + ").");
        }

        if (records.size() == 1 && filePaths.size() == 1 && expectedKeys.isEmpty()) {
            return matchSingle(records.getFirst(), filePaths.getFirst(), context);
        }

        if (records.size() != filePaths.size()) {
            throw new ComparisonException("Consumed record count (" + records.size()
                    + ") does not match expected file count (" + filePaths.size() + ").");
        }

        BatchMatchStrategy strategy = context.getBatchMatchStrategy() != null
                ? context.getBatchMatchStrategy()
                : BatchMatchStrategy.POSITIONAL;

        if (strategy == BatchMatchStrategy.LAZY) {
            return matchBatchLazy(records, filePaths, expectedKeys, context);
        }
        return matchBatchPositional(records, filePaths, expectedKeys, context);
    }

    private MatchResult matchSingle(ConsumedRecord<GenericRecord> record, String filePath, MatchContext context) {
        String expectedValue = FileUtils.getFileContent(FileUtils.getFile(filePath));
        GenericRecord value = record.getValue();
        if (value == null) {
            log.error("Avro record value is null (tombstone), expected content of '{}'.", filePath);
            return MatchResult.nullValue(expectedValue);
        }
        String actualValue = AvroJson.of(value);

        log.debug("Avro file match, actual:\n{}\nExpected:\n{}", actualValue, expectedValue);

        if (matchesExpectedContent(expectedValue, actualValue, context)) {
            log.info("Avro record matches expected file '{}'.", filePath);
            return MatchResult.pass(expectedValue, actualValue);
        }
        log.error(
                "Avro record does not match expected file '{}'.\nExpected:\n{}\nActual:\n{}",
                filePath,
                expectedValue,
                actualValue);
        return MatchResult.fail("Avro record does not match file '" + filePath + "'.", expectedValue, actualValue);
    }

    private MatchResult matchBatchPositional(
            List<ConsumedRecord<GenericRecord>> records,
            List<String> filePaths,
            List<String> expectedKeys,
            MatchContext context) {
        KeyMatchStrategy keyStrategy =
                context.getKeyMatchStrategy() != null ? context.getKeyMatchStrategy() : KeyMatchStrategy.EXACT;
        String lastExpected = "";
        String lastActual = "";
        for (int i = 0; i < records.size(); i++) {
            String filePath = filePaths.get(i);
            String expectedValue = FileUtils.getFileContent(FileUtils.getFile(filePath));
            GenericRecord value = records.get(i).getValue();
            String actualKey = records.get(i).getKey();

            if (!expectedKeys.isEmpty()) {
                String expectedKey = expectedKeys.get(i);
                if (!keyStrategy.matches(expectedKey, actualKey)) {
                    log.error(
                            "Avro record at index {} key mismatch: expected '{}' ({}), actual '{}'.",
                            i,
                            expectedKey,
                            keyStrategy,
                            actualKey);
                    return MatchResult.fail(
                            "Avro record at index " + i + " key does not match, expected: '" + expectedKey
                                    + "', actual: '" + actualKey + "', strategy: " + keyStrategy + ".",
                            expectedKey,
                            actualKey != null ? actualKey : "<null>");
                }
            }

            if (value == null) {
                log.error("Avro record at index {} has null value (tombstone), expected content of '{}'.", i, filePath);
                return MatchResult.nullValue(expectedValue);
            }
            String actualValue = AvroJson.of(value);
            if (!matchesExpectedContent(expectedValue, actualValue, context)) {
                log.error(
                        "Avro record at index {} does not match expected file '{}'.\nExpected:\n{}\nActual:\n{}",
                        i,
                        filePath,
                        expectedValue,
                        actualValue);
                return MatchResult.fail(
                        "Avro record at index " + i + " does not match file '" + filePath + "'.",
                        expectedValue,
                        actualValue);
            }
            if (!expectedKeys.isEmpty()) {
                log.info(
                        "Avro record at index {} matches expected key '{}' and file '{}'.",
                        i,
                        expectedKeys.get(i),
                        filePath);
            } else {
                log.info("Avro record at index {} matches expected file '{}'.", i, filePath);
            }
            lastExpected = expectedValue;
            lastActual = actualValue;
        }
        return MatchResult.pass(lastExpected, lastActual);
    }

    private MatchResult matchBatchLazy(
            List<ConsumedRecord<GenericRecord>> records,
            List<String> filePaths,
            List<String> expectedKeys,
            MatchContext context) {
        KeyMatchStrategy keyStrategy =
                context.getKeyMatchStrategy() != null ? context.getKeyMatchStrategy() : KeyMatchStrategy.EXACT;
        List<String> expectedContents = filePaths.stream()
                .map(path -> FileUtils.getFileContent(FileUtils.getFile(path)))
                .toList();
        boolean[] matchedFiles = new boolean[filePaths.size()];
        String lastExpected = "";
        String lastActual = "";

        for (int i = 0; i < records.size(); i++) {
            GenericRecord value = records.get(i).getValue();
            String actualKey = records.get(i).getKey();
            if (value == null) {
                log.error("Avro record at index {} has null value (tombstone).", i);
                return MatchResult.nullValue(expectedContents.getFirst());
            }
            String actualValue = AvroJson.of(value);

            int matchedIndex = -1;
            for (int j = 0; j < filePaths.size(); j++) {
                if (!matchedFiles[j]
                        && (expectedKeys.isEmpty() || keyStrategy.matches(expectedKeys.get(j), actualKey))
                        && matchesExpectedContentQuietly(expectedContents.get(j), actualValue, context)) {
                    matchedIndex = j;
                    break;
                }
            }

            if (matchedIndex >= 0) {
                matchedFiles[matchedIndex] = true;
                if (!expectedKeys.isEmpty()) {
                    log.info(
                            "Avro record at index {} matched expected key '{}' and file '{}'.",
                            i,
                            expectedKeys.get(matchedIndex),
                            filePaths.get(matchedIndex));
                } else {
                    log.info("Avro record at index {} matched expected file '{}'.", i, filePaths.get(matchedIndex));
                }
                lastExpected = expectedContents.get(matchedIndex);
                lastActual = actualValue;
            } else {
                List<String> unmatchedFiles = new ArrayList<>();
                String firstUnmatchedExpected = "";
                for (int j = 0; j < filePaths.size(); j++) {
                    if (!matchedFiles[j]) {
                        unmatchedFiles.add(
                                !expectedKeys.isEmpty()
                                        ? expectedKeys.get(j) + " -> " + filePaths.get(j)
                                        : filePaths.get(j));
                        if (firstUnmatchedExpected.isEmpty()) {
                            firstUnmatchedExpected = expectedContents.get(j);
                        }
                    }
                }
                log.error(
                        "Avro record at index {} (key='{}') did not match any remaining expected file. Unmatched files: {}\nActual record:\n{}",
                        i,
                        actualKey,
                        unmatchedFiles,
                        actualValue);
                return MatchResult.fail(
                        "Record at index " + i + " did not match any remaining expected file. Unmatched files: "
                                + unmatchedFiles,
                        firstUnmatchedExpected,
                        actualValue);
            }
        }
        return MatchResult.pass(lastExpected, lastActual);
    }

    private boolean matchesExpectedContent(String expectedValue, String actualValue, MatchContext context) {
        if (context.getExcludedFields() != null && !context.getExcludedFields().isEmpty()) {
            log.debug("Excluding Avro fields: {}", context.getExcludedFields());
            return AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(
                    expectedValue, actualValue, context.getExcludedFields());
        }
        return AvroUtils.doesAvroRecordsSmartMatches(expectedValue, actualValue);
    }

    private boolean matchesExpectedContentQuietly(String expectedValue, String actualValue, MatchContext context) {
        if (context.getExcludedFields() != null && !context.getExcludedFields().isEmpty()) {
            return AvroUtils.doesAvroRecordsSmartMatchesWithExclusionsQuietly(
                    expectedValue, actualValue, context.getExcludedFields());
        }
        return AvroUtils.doesAvroRecordsSmartMatchesQuietly(expectedValue, actualValue);
    }
}
