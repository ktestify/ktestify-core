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
import io.github.ktestify.utils.serdes.AvroDeserializer;
import io.github.ktestify.utils.serdes.AvroUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;

/**
 * Matches a specific field (or set of fields) within an Avro record, using either an inline expected value or an
 * expected file.
 *
 * <p>Requires {@link MatchContext#getMatchKey()} to specify the JSON field name to examine. Either
 * {@link MatchContext#getMatchValue()} (inline) or {@link MatchContext#getMatchFilePath()} (file-based) must also be
 * set.
 *
 * @since 0.3.0
 */
@Slf4j
public class AvroFieldsRecordMatcher implements RecordMatcher<GenericRecord> {

    @Override
    public MatchResult match(List<ConsumedRecord<GenericRecord>> records, MatchContext context)
            throws ComparisonException {

        if (context.getMatchKey() == null || context.getMatchKey().isBlank()) {
            throw new ComparisonException("AvroFieldsRecordMatcher requires matchKey (the field name) to be set.");
        }

        GenericRecord value = records.get(0).getValue();
        String actualValue = toJson(value);
        String key = context.getMatchKey();

        // Option A — inline expected value
        if (context.getMatchValue() != null && !context.getMatchValue().isBlank()) {
            log.debug(
                    "Avro field match against inline value — key: '{}', expected: '{}'", key, context.getMatchValue());
            boolean result = AvroUtils.doesAvroValueFromKeyMatchesRecord(context.getMatchValue(), key, actualValue);
            if (result) {
                return MatchResult.pass(context.getMatchValue(), actualValue);
            }
            return MatchResult.fail(
                    "Avro field '" + key + "' does not match expected value '" + context.getMatchValue() + "'.",
                    context.getMatchValue(),
                    actualValue);
        }

        // Option B — field comparison against expected file
        if (context.getMatchFilePath() != null && !context.getMatchFilePath().isBlank()) {
            String expectedRecord = FileUtils.getFileContent(FileUtils.getFile(context.getMatchFilePath()));
            log.debug("Avro field match against file — key: '{}', file: '{}'", key, context.getMatchFilePath());
            boolean result = AvroUtils.doesAvroValueFromKeyMatchesRecords(key, expectedRecord, actualValue);
            if (result) {
                return MatchResult.pass(expectedRecord, actualValue);
            }
            return MatchResult.fail(
                    "Avro field '" + key + "' does not match fields in file '" + context.getMatchFilePath() + "'.",
                    expectedRecord,
                    actualValue);
        }

        throw new ComparisonException("AvroFieldsRecordMatcher requires either matchValue or matchFilePath to be set.");
    }

    private String toJson(GenericRecord value) {
        if (value.getSchema() != null) {
            return AvroUtils.getPrettyAvroValue(
                    AvroUtils.convertMapToJsonString(AvroDeserializer.recordDeserializer(value)));
        }
        return AvroUtils.getPrettyAvroValue(value.toString());
    }
}
