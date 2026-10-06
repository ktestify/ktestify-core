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
import io.github.ktestify.utils.FieldMatcherUtils;
import io.github.ktestify.utils.FileUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Matches a fixed-position field extracted from a positional record against either an inline expected value
 * ({@link MatchContext#getMatchValue()}) or the same field position in an expected file
 * ({@link MatchContext#getMatchFilePath()}).
 *
 * <p>Requires {@link MatchContext#getMatchKey()} to encode the position as {@code "line:from:to"} (e.g.
 * {@code "0:10:20"} means line 0, characters 10–20).
 *
 * @since 0.3.0
 */
@Slf4j
public class FieldsRecordMatcher implements RecordMatcher<String> {

    @Override
    public MatchResult match(List<ConsumedRecord<String>> records, MatchContext context) throws ComparisonException {

        int[] pos = parsePosition(context);
        int line = pos[0];
        int from = pos[1];
        int to = pos[2];

        String actualValue = records.get(0).getValue();
        String actualField = FieldMatcherUtils.getFieldsToMatch(actualValue, line, from, to);

        // Option A — compare against an inline expected value
        if (context.getMatchValue() != null && !context.getMatchValue().isBlank()) {
            String expectedField = context.getMatchValue();
            log.debug("Fields match: expected='{}', actual='{}'", expectedField, actualField);
            if (expectedField.equals(actualField)) {
                return MatchResult.pass(expectedField, actualField);
            }
            return MatchResult.fail(
                    "Field at line=" + line + " from=" + from + " to=" + to + " does not match.",
                    expectedField,
                    actualField);
        }

        // Option B — compare the same field position extracted from an expected file
        if (context.getMatchFilePath() != null && !context.getMatchFilePath().isBlank()) {
            String expectedContent = FileUtils.getFileContent(FileUtils.getFile(context.getMatchFilePath()));
            String expectedField = FieldMatcherUtils.getFieldsToMatch(expectedContent, line, from, to);
            log.debug("Fields match (from file): expected='{}', actual='{}'", expectedField, actualField);
            if (expectedField.equals(actualField)) {
                return MatchResult.pass(expectedField, actualField);
            }
            return MatchResult.fail(
                    "Field at line=" + line + " from=" + from + " to=" + to + " does not match.",
                    expectedField,
                    actualField);
        }

        throw new ComparisonException("FieldsRecordMatcher requires either matchValue or matchFilePath to be set.");
    }

    /** Parses the position descriptor from {@link MatchContext#getMatchKey()} in the form "line:from:to". */
    private int[] parsePosition(MatchContext context) throws ComparisonException {
        String descriptor = context.getMatchKey();
        if (descriptor == null || descriptor.isBlank()) {
            throw new ComparisonException("FieldsRecordMatcher requires matchKey in the form 'line:from:to'.");
        }
        String[] parts = descriptor.split(":");
        if (parts.length != 3) {
            throw new ComparisonException(
                    "FieldsRecordMatcher matchKey must be 'line:from:to', got: '" + descriptor + "'.");
        }
        try {
            return new int[] {
                Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())
            };
        } catch (NumberFormatException e) {
            throw new ComparisonException(
                    "FieldsRecordMatcher matchKey contains non-integer values: '" + descriptor + "'.");
        }
    }
}
