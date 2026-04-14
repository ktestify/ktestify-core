/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
 * Compares the record value as XML against an expected XML file. Supports optional element exclusion via
 * {@link MatchContext#getExcludedFields()}.
 *
 * <p>Requires {@link MatchContext#getMatchFilePath()} to be set.
 *
 * @since 0.3.0
 */
@Slf4j
public class XmlRecordMatcher implements RecordMatcher<String> {

    @Override
    public MatchResult match(List<ConsumedRecord<String>> records, MatchContext context) throws ComparisonException {

        if (context.getMatchFilePath() == null || context.getMatchFilePath().isBlank()) {
            throw new ComparisonException("XmlRecordMatcher requires matchFilePath to be set.");
        }

        String expected = FileUtils.getFileContent(FileUtils.getFile(context.getMatchFilePath()));
        String actual = records.get(0).getValue();
        log.debug("XML comparison — actual:\n{}\nExpected:\n{}", actual, expected);

        boolean result;
        if (context.getExcludedFields() != null && !context.getExcludedFields().isEmpty()) {
            log.debug("Excluding XML elements: {}", context.getExcludedFields());
            result = XMLUtils.compareXML(actual, expected, context.getExcludedFields());
        } else {
            result = XMLUtils.compareXML(actual, expected);
        }

        if (result) {
            return MatchResult.pass(expected, actual);
        }
        return MatchResult.fail(
                "XML documents do not match (excluded: " + context.getExcludedFields() + ").", expected, actual);
    }
}
