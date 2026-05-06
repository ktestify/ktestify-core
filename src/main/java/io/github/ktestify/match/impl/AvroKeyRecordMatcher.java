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
import org.apache.avro.generic.GenericRecord;

/**
 * Asserts that the Avro record key equals the expected key in {@link MatchContext#getMatchKey()}.
 *
 * @since 0.3.0
 */
@Slf4j
public class AvroKeyRecordMatcher implements RecordMatcher<GenericRecord> {

    @Override
    public MatchResult match(List<ConsumedRecord<GenericRecord>> records, MatchContext context)
            throws ComparisonException {

        if (context.getMatchKey() == null || context.getMatchKey().isBlank()) {
            throw new ComparisonException("AvroKeyRecordMatcher requires matchKey to be set.");
        }

        String expectedKey = context.getMatchKey();
        String actualKey = records.get(0).getKey();

        if (expectedKey.equals(actualKey)) {
            log.info("Avro record key matches expected key '{}'.", expectedKey);
            return MatchResult.pass(expectedKey, actualKey);
        }

        log.error("Avro record key mismatch — expected: '{}', actual: '{}'", expectedKey, actualKey);
        return MatchResult.fail(
                "Avro record key does not match — expected: '" + expectedKey + "', actual: '" + actualKey + "'.",
                expectedKey,
                actualKey);
    }
}
