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
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Compares both the record <em>key</em> and <em>value</em> against a configured expected key and expected file content.
 *
 * <p>Requires:
 *
 * <ul>
 *   <li>{@link MatchContext#getMatchKey()} — expected record key
 *   <li>{@link MatchContext#getMatchFilePath()} — path to the expected value file
 * </ul>
 *
 * @since 0.3.0
 */
@Slf4j
public class FileKeyRecordMatcher implements RecordMatcher<String> {

    @Override
    public MatchResult match(List<ConsumedRecord<String>> records, MatchContext context) throws ComparisonException {

        if (context.getMatchKey() == null || context.getMatchKey().isBlank()) {
            throw new ComparisonException("FileKeyRecordMatcher requires matchKey to be set.");
        }
        if (context.getMatchFilePath() == null || context.getMatchFilePath().isBlank()) {
            throw new ComparisonException("FileKeyRecordMatcher requires matchFilePath to be set.");
        }

        ConsumedRecord<String> record = records.get(0);
        String expectedValue = FileUtils.getFileContent(FileUtils.getFile(context.getMatchFilePath()));
        String actualValue = record.getValue();
        String expectedKey = context.getMatchKey();
        String actualKey = record.getKey();

        boolean keyMatches = expectedKey.equals(actualKey);
        boolean valueMatches = actualValue.equals(expectedValue);

        if (!keyMatches) {
            log.error("Key mismatch — expected: '{}', actual: '{}'", expectedKey, actualKey);
        }
        if (!valueMatches) {
            log.error(
                    "Value mismatch.\nExpected diff:\n{}\nActual diff:\n{}",
                    StringDiffUtils.getPrettyStringDiff(expectedValue, actualValue, StringDiffUtils.Type.EXPECTED),
                    StringDiffUtils.getPrettyStringDiff(expectedValue, actualValue, StringDiffUtils.Type.ACTUAL));
        }

        if (keyMatches && valueMatches) {
            log.info("Record key and value both match.");
            return MatchResult.pass(expectedValue, actualValue);
        }
        return MatchResult.fail(
                "Key match: " + keyMatches + ", value match: " + valueMatches,
                expectedKey + " / " + expectedValue,
                actualKey + " / " + actualValue);
    }
}
