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
import io.github.ktestify.utils.serdes.AvroDeserializer;
import io.github.ktestify.utils.serdes.AvroUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;

/**
 * Compares an Avro record's value against the content of an expected JSON file using smart matching. Supports optional
 * field exclusion via {@link MatchContext#getExcludedFields()}.
 *
 * <p>Requires {@link MatchContext#getMatchFilePath()} to be set.
 *
 * @since 0.3.0
 */
@Slf4j
public class AvroFileRecordMatcher implements RecordMatcher<GenericRecord> {

    @Override
    public MatchResult match(List<ConsumedRecord<GenericRecord>> records, MatchContext context)
            throws ComparisonException {

        if (context.getMatchFilePath() == null || context.getMatchFilePath().isBlank()) {
            throw new ComparisonException("AvroFileRecordMatcher requires matchFilePath to be set.");
        }

        GenericRecord value = records.get(0).getValue();
        String actualValue = toJson(value);
        String expectedValue = FileUtils.getFileContent(FileUtils.getFile(context.getMatchFilePath()));

        log.debug(
                "Avro file match — actual:\n{}\nExpected:\n{}",
                actualValue,
                AvroUtils.getPrettyAvroValue(AvroUtils.convertMapToJsonString(
                        AvroUtils.convertDatesToTimestamps(AvroUtils.convertJsonToMap(expectedValue)))));

        boolean result;
        if (context.getExcludedFields() != null && !context.getExcludedFields().isEmpty()) {
            log.debug("Excluding Avro fields: {}", context.getExcludedFields());
            result = AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(
                    expectedValue, actualValue, context.getExcludedFields());
        } else {
            result = AvroUtils.doesAvroRecordsSmartMatches(expectedValue, actualValue);
        }

        if (result) {
            log.info("Avro record matches expected file '{}'.", context.getMatchFilePath());
            return MatchResult.pass(expectedValue, actualValue);
        }
        log.error(
                "Avro record does not match expected file '{}'.\nExpected:\n{}\nActual:\n{}",
                context.getMatchFilePath(),
                expectedValue,
                actualValue);
        return MatchResult.fail(
                "Avro record does not match file '" + context.getMatchFilePath() + "'.", expectedValue, actualValue);
    }

    private String toJson(GenericRecord value) {
        if (value.getSchema() != null) {
            return AvroUtils.getPrettyAvroValue(
                    AvroUtils.convertMapToJsonString(AvroDeserializer.recordDeserializer(value)));
        }
        return AvroUtils.getPrettyAvroValue(value.toString());
    }
}
