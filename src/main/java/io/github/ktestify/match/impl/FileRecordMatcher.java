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
import io.github.ktestify.utils.StringDiffUtils;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Compares the String value(s) of consumed record(s) against the content of expected file(s).
 *
 * <p>Requires {@link MatchContext#getMatchFilePaths()} to be set. In batch mode, supports both
 * {@link BatchMatchStrategy#POSITIONAL} (default) and {@link BatchMatchStrategy#LAZY} matching. A record with a
 * {@code null} value (tombstone) fails with {@link MatchResult#nullValue(String)}.
 *
 * @since 0.3.0
 */
@Slf4j
public class FileRecordMatcher implements RecordMatcher<String> {

    @Override
    public MatchResult match(List<ConsumedRecord<String>> records, MatchContext context) throws ComparisonException {
        if (records == null || records.isEmpty()) {
            return MatchResult.noRecords();
        }
        List<String> filePaths = context.getMatchFilePaths();
        if (filePaths == null || filePaths.isEmpty() || filePaths.stream().anyMatch(p -> p == null || p.isBlank())) {
            throw new ComparisonException("FileRecordMatcher requires matchFilePath(s) to be set.");
        }

        if (records.size() == 1 && filePaths.size() == 1) {
            return matchSingle(records.getFirst(), filePaths.getFirst());
        }

        if (records.size() != filePaths.size()) {
            throw new ComparisonException("Consumed record count (" + records.size()
                    + ") does not match expected file count (" + filePaths.size() + ").");
        }

        BatchMatchStrategy strategy = context.getBatchMatchStrategy() != null
                ? context.getBatchMatchStrategy()
                : BatchMatchStrategy.POSITIONAL;

        if (strategy == BatchMatchStrategy.LAZY) {
            return matchBatchLazy(records, filePaths);
        }
        return matchBatchPositional(records, filePaths);
    }

    private MatchResult matchSingle(ConsumedRecord<String> record, String filePath) {
        String expected = FileUtils.getFileContent(FileUtils.getFile(filePath));
        String actual = record.getValue();

        if (actual == null) {
            log.error("Record value is null (tombstone), expected content of '{}'.", filePath);
            return MatchResult.nullValue(expected);
        }
        if (actual.equals(expected)) {
            log.info("Record value matches expected file '{}'.", filePath);
            return MatchResult.pass(expected, actual);
        }

        log.error(
                "Record value does not match expected file '{}'.\nExpected diff:\n{}\nActual diff:\n{}",
                filePath,
                StringDiffUtils.getPrettyStringDiff(expected, actual, StringDiffUtils.Type.EXPECTED),
                StringDiffUtils.getPrettyStringDiff(expected, actual, StringDiffUtils.Type.ACTUAL));

        return MatchResult.fail("Record value does not match file '" + filePath + "'.", expected, actual);
    }

    private MatchResult matchBatchPositional(List<ConsumedRecord<String>> records, List<String> filePaths) {
        String lastExpected = "";
        String lastActual = "";
        for (int i = 0; i < records.size(); i++) {
            String filePath = filePaths.get(i);
            String expected = FileUtils.getFileContent(FileUtils.getFile(filePath));
            String actual = records.get(i).getValue();

            if (actual == null) {
                log.error("Record at index {} has null value (tombstone), expected content of '{}'.", i, filePath);
                return MatchResult.nullValue(expected);
            }
            if (!actual.equals(expected)) {
                log.error(
                        "Record at index {} does not match expected file '{}'.\nExpected diff:\n{}\nActual diff:\n{}",
                        i,
                        filePath,
                        StringDiffUtils.getPrettyStringDiff(expected, actual, StringDiffUtils.Type.EXPECTED),
                        StringDiffUtils.getPrettyStringDiff(expected, actual, StringDiffUtils.Type.ACTUAL));
                return MatchResult.fail(
                        "Record at index " + i + " does not match file '" + filePath + "'.", expected, actual);
            }
            log.info("Record at index {} matches expected file '{}'.", i, filePath);
            lastExpected = expected;
            lastActual = actual;
        }
        return MatchResult.pass(lastExpected, lastActual);
    }

    private MatchResult matchBatchLazy(List<ConsumedRecord<String>> records, List<String> filePaths) {
        List<String> expectedContents = filePaths.stream()
                .map(path -> FileUtils.getFileContent(FileUtils.getFile(path)))
                .toList();
        boolean[] matchedFiles = new boolean[filePaths.size()];
        String lastExpected = "";
        String lastActual = "";

        for (int i = 0; i < records.size(); i++) {
            String actual = records.get(i).getValue();
            if (actual == null) {
                log.error("Record at index {} has null value (tombstone).", i);
                return MatchResult.nullValue(expectedContents.getFirst());
            }

            int matchedIndex = -1;
            for (int j = 0; j < filePaths.size(); j++) {
                if (!matchedFiles[j] && actual.equals(expectedContents.get(j))) {
                    matchedIndex = j;
                    break;
                }
            }

            if (matchedIndex >= 0) {
                matchedFiles[matchedIndex] = true;
                log.info("Record at index {} matched expected file '{}'.", i, filePaths.get(matchedIndex));
                lastExpected = expectedContents.get(matchedIndex);
                lastActual = actual;
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
                        "Record at index {} did not match any remaining expected file. Unmatched files: {}",
                        i,
                        unmatchedFiles);
                return MatchResult.fail(
                        "Record at index " + i + " did not match any remaining expected file. Unmatched files: "
                                + unmatchedFiles,
                        firstUnmatchedExpected,
                        actual);
            }
        }
        return MatchResult.pass(lastExpected, lastActual);
    }
}
