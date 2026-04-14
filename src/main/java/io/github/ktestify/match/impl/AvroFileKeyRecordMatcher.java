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
 * Asserts both the record <em>key</em> and Avro <em>value</em> against a configured expected key and expected JSON
 * file.
 *
 * <p>Requires:
 *
 * <ul>
 *   <li>{@link MatchContext#getMatchKey()} — the expected record key
 *   <li>{@link MatchContext#getMatchFilePath()} — path to the expected value JSON file
 * </ul>
 *
 * @since 0.3.0
 */
@Slf4j
public class AvroFileKeyRecordMatcher implements RecordMatcher<GenericRecord> {

    @Override
    public MatchResult match(List<ConsumedRecord<GenericRecord>> records, MatchContext context)
            throws ComparisonException {

        if (context.getMatchKey() == null || context.getMatchKey().isBlank()) {
            throw new ComparisonException("AvroFileKeyRecordMatcher requires matchKey to be set.");
        }
        if (context.getMatchFilePath() == null || context.getMatchFilePath().isBlank()) {
            throw new ComparisonException("AvroFileKeyRecordMatcher requires matchFilePath to be set.");
        }

        ConsumedRecord<GenericRecord> record = records.get(0);
        String actualKey = record.getKey();
        String expectedKey = context.getMatchKey();
        String expectedValue = FileUtils.getFileContent(FileUtils.getFile(context.getMatchFilePath()));
        String actualValue = toJson(record.getValue());

        boolean keyMatches = expectedKey.equals(actualKey);
        boolean valueMatches =
                AvroUtils.doesAvroRecordsSmartMatches(AvroUtils.getPrettyAvroValue(expectedValue), actualValue);

        if (!keyMatches) {
            log.error("Avro key mismatch — expected: '{}', actual: '{}'", expectedKey, actualKey);
        }
        if (!valueMatches) {
            log.error(
                    "Avro value does not match file '{}'.\nExpected:\n{}\nActual:\n{}",
                    context.getMatchFilePath(),
                    expectedValue,
                    actualValue);
        }

        if (keyMatches && valueMatches) {
            log.info("Avro record key and value both match.");
            return MatchResult.pass(expectedKey + " / " + expectedValue, actualKey + " / " + actualValue);
        }
        return MatchResult.fail(
                "Key match: " + keyMatches + ", value match: " + valueMatches,
                expectedKey + " / " + expectedValue,
                actualKey + " / " + actualValue);
    }

    private String toJson(GenericRecord value) {
        if (value.getSchema() != null) {
            return AvroUtils.getPrettyAvroValue(
                    AvroUtils.convertMapToJsonString(AvroDeserializer.recordDeserializer(value)));
        }
        return AvroUtils.getPrettyAvroValue(value.toString());
    }
}
