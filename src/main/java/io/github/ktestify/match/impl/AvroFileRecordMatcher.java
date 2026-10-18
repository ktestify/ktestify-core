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
import io.github.ktestify.match.RecordMatcher;
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

        if (records.size() == 1 && filePaths.size() == 1) {
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
            return matchBatchLazy(records, filePaths, context);
        }
        return matchBatchPositional(records, filePaths, context);
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
            List<ConsumedRecord<GenericRecord>> records, List<String> filePaths, MatchContext context) {
        String lastExpected = "";
        String lastActual = "";
        for (int i = 0; i < records.size(); i++) {
            String filePath = filePaths.get(i);
            String expectedValue = FileUtils.getFileContent(FileUtils.getFile(filePath));
            GenericRecord value = records.get(i).getValue();
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
            log.info("Avro record at index {} matches expected file '{}'.", i, filePath);
            lastExpected = expectedValue;
            lastActual = actualValue;
        }
        return MatchResult.pass(lastExpected, lastActual);
    }

    private MatchResult matchBatchLazy(
            List<ConsumedRecord<GenericRecord>> records, List<String> filePaths, MatchContext context) {
        List<String> expectedContents = filePaths.stream()
                .map(path -> FileUtils.getFileContent(FileUtils.getFile(path)))
                .toList();
        boolean[] matchedFiles = new boolean[filePaths.size()];
        String lastExpected = "";
        String lastActual = "";

        for (int i = 0; i < records.size(); i++) {
            GenericRecord value = records.get(i).getValue();
            if (value == null) {
                log.error("Avro record at index {} has null value (tombstone).", i);
                return MatchResult.nullValue(expectedContents.getFirst());
            }
            String actualValue = AvroJson.of(value);

            int matchedIndex = -1;
            for (int j = 0; j < filePaths.size(); j++) {
                if (!matchedFiles[j] && matchesExpectedContentQuietly(expectedContents.get(j), actualValue, context)) {
                    matchedIndex = j;
                    break;
                }
            }

            if (matchedIndex >= 0) {
                matchedFiles[matchedIndex] = true;
                log.info("Avro record at index {} matched expected file '{}'.", i, filePaths.get(matchedIndex));
                lastExpected = expectedContents.get(matchedIndex);
                lastActual = actualValue;
            } else {
                List<String> unmatchedFiles = new ArrayList<>();
                String firstUnmatchedExpected = "";
                for (int j = 0; j < filePaths.size(); j++) {
                    if (!matchedFiles[j]) {
                        unmatchedFiles.add(filePaths.get(j));
                        if (firstUnmatchedExpected.isEmpty()) {
                            firstUnmatchedExpected = expectedContents.get(j);
                        }
                    }
                }
                log.error(
                        "Avro record at index {} did not match any remaining expected file. Unmatched files: {}\nActual record:\n{}",
                        i,
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
