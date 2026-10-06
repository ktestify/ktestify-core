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
import io.github.ktestify.utils.XMLUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Compares the record value as XML against an expected file using a set of XPath expressions. All XPath comparisons
 * must pass for the matcher to return a passing result.
 *
 * <p>Requires:
 *
 * <ul>
 *   <li>{@link MatchContext#getMatchFilePath()} — path to the expected XML file
 *   <li>{@link MatchContext#getExcludedFields()} — repurposed here as the list of XPath expressions to evaluate
 * </ul>
 *
 * @since 0.3.0
 */
@Slf4j
public class XPathRecordMatcher implements RecordMatcher<String> {

    @Override
    public MatchResult match(List<ConsumedRecord<String>> records, MatchContext context) throws ComparisonException {

        if (context.getExcludedFields() == null || context.getExcludedFields().isEmpty()) {
            throw new ComparisonException(
                    "XPathRecordMatcher requires xPath expressions to be set via excludedFields.");
        }
        if (context.getMatchFilePath() == null || context.getMatchFilePath().isBlank()) {
            throw new ComparisonException("XPathRecordMatcher requires matchFilePath to be set.");
        }

        String expected = FileUtils.getFileContent(FileUtils.getFile(context.getMatchFilePath()));
        String actual = records.get(0).getValue();
        log.debug(
                "XPath comparison — XPaths: {}\nActual:\n{}\nExpected:\n{}",
                context.getExcludedFields(),
                actual,
                expected);

        boolean result = XMLUtils.compareXMLByXPaths(actual, expected, context.getExcludedFields());
        if (result) {
            return MatchResult.pass(expected, actual);
        }
        return MatchResult.fail(
                "XML XPath comparison failed (XPaths: " + context.getExcludedFields() + ").", expected, actual);
    }
}
