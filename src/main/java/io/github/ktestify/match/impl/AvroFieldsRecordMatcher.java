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
import io.github.ktestify.utils.serdes.AvroUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Matches a specific field (or set of fields) within an Avro record, using either an inline expected value or an
 * expected file.
 *
 * <p>Requires {@link MatchContext#getMatchKey()} to specify the JSON field name to examine. Either
 * {@link MatchContext#getMatchValue()} (inline) or {@link MatchContext#getMatchFilePath()} (file-based) must also be
 * set. Alternatively, {@link MatchContext#getMatchKeyValues()} checks several fields at once.
 *
 * <p>A record with a {@code null} value (tombstone) fails with {@link MatchResult#nullValue(String)}.
 *
 * @since 0.3.0
 */
@Slf4j
public class AvroFieldsRecordMatcher implements RecordMatcher<GenericRecord> {

    @Override
    public MatchResult match(List<ConsumedRecord<GenericRecord>> records, MatchContext context)
            throws ComparisonException {
        if (records == null || records.isEmpty()) {
            return MatchResult.noRecords();
        }

        // Multi-field inline matching (keys/values columns)
        if (context.getMatchKeyValues() != null && !context.getMatchKeyValues().isEmpty()) {
            return matchMultipleFields(records, context);
        }

        if (context.getMatchKey() == null || context.getMatchKey().isBlank()) {
            throw new ComparisonException("AvroFieldsRecordMatcher requires matchKey (the field name) to be set.");
        }
        String key = context.getMatchKey();

        // Option A: inline expected value
        if (context.getMatchValue() != null && !context.getMatchValue().isBlank()) {
            String expected = context.getMatchValue();
            GenericRecord value = records.getFirst().getValue();
            if (value == null) {
                log.error("Avro record value is null (tombstone), expected field '{}' = '{}'.", key, expected);
                return MatchResult.nullValue(expected);
            }
            String actualValue = AvroJson.of(value);
            log.debug("Avro field match against inline value, key: '{}', expected: '{}'", key, expected);
            if (AvroUtils.doesAvroValueFromKeyMatchesRecord(expected, key, actualValue)) {
                return MatchResult.pass(expected, actualValue);
            }
            return MatchResult.fail(
                    "Avro field '" + key + "' does not match expected value '" + expected + "'.",
                    expected,
                    actualValue);
        }

        // Option B: field comparison against expected file
        if (context.getMatchFilePath() != null && !context.getMatchFilePath().isBlank()) {
            String expectedRecord = FileUtils.getFileContent(FileUtils.getFile(context.getMatchFilePath()));
            GenericRecord value = records.getFirst().getValue();
            if (value == null) {
                log.error(
                        "Avro record value is null (tombstone), expected field '{}' from '{}'.",
                        key,
                        context.getMatchFilePath());
                return MatchResult.nullValue(expectedRecord);
            }
            String actualValue = AvroJson.of(value);
            log.debug("Avro field match against file, key: '{}', file: '{}'", key, context.getMatchFilePath());
            if (AvroUtils.doesAvroValueFromKeyMatchesRecords(key, expectedRecord, actualValue)) {
                return MatchResult.pass(expectedRecord, actualValue);
            }
            return MatchResult.fail(
                    "Avro field '" + key + "' does not match fields in file '" + context.getMatchFilePath() + "'.",
                    expectedRecord,
                    actualValue);
        }

        throw new ComparisonException("AvroFieldsRecordMatcher requires either matchValue or matchFilePath to be set.");
    }

    /**
     * Validates every key/value pair in {@link MatchContext#getMatchKeyValues()} against the actual Avro record.
     *
     * <p>All pairs must match for the result to pass. Every mismatch is reported in the diff.
     *
     * @param records the consumed records (only the first is examined)
     * @param context the match context carrying the key/value pairs
     * @return a {@link MatchResult} indicating whether all fields matched
     * @throws ComparisonException if a field lookup cannot be performed
     * @since 1.1.1
     */
    private MatchResult matchMultipleFields(List<ConsumedRecord<GenericRecord>> records, MatchContext context)
            throws ComparisonException {
        Map<String, String> keyValues = context.getMatchKeyValues();
        GenericRecord value = records.getFirst().getValue();
        if (value == null) {
            log.error("Avro record value is null (tombstone), expected fields {}.", keyValues);
            return MatchResult.nullValue(keyValues.toString());
        }
        String actualValue = AvroJson.of(value);

        log.debug("Avro multi-field match against inline values, pairs: {}", keyValues);

        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, String> entry : keyValues.entrySet()) {
            String key = entry.getKey();
            String expectedVal = entry.getValue();
            if (!AvroUtils.doesAvroValueFromKeyMatchesRecord(expectedVal, key, actualValue)) {
                mismatches.add("Avro field '" + key + "' does not match expected value '" + expectedVal + "'.");
            }
        }

        if (mismatches.isEmpty()) {
            return MatchResult.pass(keyValues.toString(), actualValue);
        }
        return MatchResult.fail(String.join(" ", mismatches), keyValues.toString(), actualValue);
    }
}
