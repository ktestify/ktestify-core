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
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import io.github.ktestify.match.RecordMatcher;
import io.github.ktestify.models.ConsumedRecord;
import io.github.ktestify.utils.FileUtils;
import io.github.ktestify.utils.StringDiffUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Compares both the record <em>key</em> and <em>value</em> against a configured expected key and expected file content.
 *
 * <p>Requires:
 *
 * <ul>
 *   <li>{@link MatchContext#getMatchKey()}: the expected record key
 *   <li>{@link MatchContext#getMatchFilePath()}: the path to the expected value file
 * </ul>
 *
 * <p>A record with a {@code null} value (tombstone) never matches; the key is still compared and reported.
 *
 * @since 0.3.0
 */
@Slf4j
public class FileKeyRecordMatcher implements RecordMatcher<String> {

    @Override
    public MatchResult match(List<ConsumedRecord<String>> records, MatchContext context) throws ComparisonException {
        if (records == null || records.isEmpty()) {
            return MatchResult.noRecords();
        }
        if (context.getMatchKey() == null || context.getMatchKey().isBlank()) {
            throw new ComparisonException("FileKeyRecordMatcher requires matchKey to be set.");
        }
        if (context.getMatchFilePath() == null || context.getMatchFilePath().isBlank()) {
            throw new ComparisonException("FileKeyRecordMatcher requires matchFilePath to be set.");
        }

        ConsumedRecord<String> record = records.getFirst();
        String expectedValue = FileUtils.getFileContent(FileUtils.getFile(context.getMatchFilePath()));
        String actualValue = record.getValue();
        String expectedKey = context.getMatchKey();
        String actualKey = record.getKey();

        boolean keyMatches = expectedKey.equals(actualKey);
        boolean valueMatches = expectedValue.equals(actualValue);

        if (!keyMatches) {
            log.error("Key mismatch, expected: '{}', actual: '{}'", expectedKey, actualKey);
        }
        if (actualValue == null) {
            log.error("Value mismatch: record value is null (tombstone).");
        } else if (!valueMatches) {
            log.error(
                    "Value mismatch.\nExpected diff:\n{}\nActual diff:\n{}",
                    StringDiffUtils.getPrettyStringDiff(expectedValue, actualValue, StringDiffUtils.Type.EXPECTED),
                    StringDiffUtils.getPrettyStringDiff(expectedValue, actualValue, StringDiffUtils.Type.ACTUAL));
        }

        if (keyMatches && valueMatches) {
            log.info("Record key and value both match.");
            return MatchResult.pass(expectedValue, actualValue);
        }
        String diff = "Key match: " + keyMatches + ", value match: " + valueMatches
                + (actualValue == null ? " (" + MatchResult.NULL_VALUE_MESSAGE + ")" : "");
        return MatchResult.fail(diff, expectedKey + " / " + expectedValue, actualKey + " / " + actualValue);
    }
}
