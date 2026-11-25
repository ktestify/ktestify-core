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
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Asserts that a consumed record is a tombstone (its value is {@code null}).
 *
 * <p>When {@link MatchContext#getMatchKey()} is non-blank, the record key is also checked using
 * {@link MatchContext#getKeyMatchStrategy()}. When {@code matchKey} is {@code null} or blank, any key is accepted.
 *
 * <p>Generic over {@code V} so it works for both raw ({@code String}) and Avro ({@code GenericRecord}) records.
 *
 * @param <V> the record value type
 * @since 1.2.11
 */
@Slf4j
public class TombstoneRecordMatcher<V> implements RecordMatcher<V> {

    private static final String EXPECTED_TOMBSTONE = "<null (tombstone)>";

    /**
     * Matches the first record in {@code records}, asserting that its value is {@code null} and, when configured, its
     * key matches {@link MatchContext#getMatchKey()}.
     *
     * @param records the fetched records to assert against
     * @param context the match configuration
     * @return a passing {@link MatchResult} if the record is a matching tombstone, or a failing result with a diff
     * @throws ComparisonException if comparison cannot be performed
     * @since 1.2.11
     */
    @Override
    public MatchResult match(List<ConsumedRecord<V>> records, MatchContext context) throws ComparisonException {
        if (records == null || records.isEmpty()) {
            return MatchResult.noRecords();
        }

        ConsumedRecord<V> record = records.getFirst();
        if (record.getValue() != null) {
            String actualValue = String.valueOf(record.getValue());
            log.error(
                    "Tombstone assertion failed for key '{}': expected null value, but got non-null value.",
                    record.getKey());
            return MatchResult.fail(
                    "Expected tombstone record (null value), but got non-null value: " + actualValue,
                    EXPECTED_TOMBSTONE,
                    actualValue);
        }

        String expectedKey = context != null ? context.getMatchKey() : null;
        if (expectedKey != null && !expectedKey.isBlank()) {
            String actualKey = record.getKey();
            if (!context.getKeyMatchStrategy().matches(expectedKey, actualKey)) {
                log.error(
                        "Tombstone key mismatch, expected: '{}', actual: '{}', using {} strategy.",
                        expectedKey,
                        actualKey,
                        context.getKeyMatchStrategy());
                return MatchResult.fail(
                        "Tombstone record key does not match, expected: '" + expectedKey + "', actual: '" + actualKey
                                + "', using " + context.getKeyMatchStrategy() + " strategy.",
                        expectedKey,
                        actualKey);
            }
            log.info(
                    "Tombstone record matched with key '{}' using {} strategy.",
                    actualKey,
                    context.getKeyMatchStrategy());
            return MatchResult.pass(expectedKey, actualKey);
        }

        log.info("Tombstone record matched (key='{}').", record.getKey());
        return MatchResult.pass(EXPECTED_TOMBSTONE, EXPECTED_TOMBSTONE);
    }
}
