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
package io.github.ktestify.match;

import io.github.ktestify.exceptions.ComparisonException;
import io.github.ktestify.models.ConsumedRecord;
import java.util.List;

/**
 * Transport-agnostic assertion contract.
 *
 * <p>A {@code RecordMatcher} receives the records that were fetched by a {@link io.github.ktestify.io.core.RecordFetcher}
 * and asserts them against an expected state described by a {@link MatchContext}.
 *
 * <p>Implementations have <strong>zero dependency</strong> on Kafka, IBM MQ, or any other transport. They only know
 * about {@link ConsumedRecord} — the common currency produced by every fetcher. This means every matcher works
 * unchanged for Kafka today and IBM MQ tomorrow.
 *
 * <p>Concrete implementations live in {@code io.github.ktestify.match.impl}:
 *
 * <ul>
 *   <li>{@code NoOpRecordMatcher} — always passes; use when only consumption matters
 *   <li>{@code FileRecordMatcher} — compares record value against a file (String diff)
 *   <li>{@code JsonRecordMatcher} — structural JSON comparison with excluded-field support
 *   <li>{@code AvroRecordMatcher} — delegates to {@code AvroUtils} for Avro records
 * </ul>
 *
 * @param <V> the type of the record value (e.g. {@code String}, {@code GenericRecord})
 * @since 0.3.0
 */
@FunctionalInterface
public interface RecordMatcher<V> {

    /**
     * Asserts the given records against the expected state carried by {@code context}.
     *
     * @param records a non-null, non-empty list of records fetched from the source
     * @param context the assertion configuration (expected file, excluded fields, …)
     * @return a {@link MatchResult} describing whether the assertion passed and, if not, what the diff looks like
     * @throws ComparisonException if the comparison itself fails due to a configuration error (e.g. expected file not
     *     found, malformed JSON)
     */
    MatchResult match(List<ConsumedRecord<V>> records, MatchContext context) throws ComparisonException;
}
