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
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Asserts that the record key equals the expected key defined in {@link MatchContext#getMatchKey()}.
 *
 * @since 0.3.0
 */
@Slf4j
public class KeyRecordMatcher implements RecordMatcher<String> {

    @Override
    public MatchResult match(List<ConsumedRecord<String>> records, MatchContext context) throws ComparisonException {

        if (context.getMatchKey() == null || context.getMatchKey().isBlank()) {
            throw new ComparisonException("KeyRecordMatcher requires matchKey to be set.");
        }

        String expectedKey = context.getMatchKey();
        String actualKey = records.get(0).getKey();

        if (expectedKey.equals(actualKey)) {
            log.info("Record key matches expected key '{}'.", expectedKey);
            return MatchResult.pass(expectedKey, actualKey);
        }

        log.error("Record key mismatch — expected: '{}', actual: '{}'", expectedKey, actualKey);
        return MatchResult.fail(
                "Record key does not match — expected: '" + expectedKey + "', actual: '" + actualKey + "'.",
                expectedKey,
                actualKey);
    }
}
