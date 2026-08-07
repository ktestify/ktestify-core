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

import java.util.*;

/**
 * Generic matcher that asserts one or more {@link ConsumedRecord#getAttributes()} entries against the expected values
 * supplied via {@link MatchContext#getExpectedAttributes()}.
 *
 * <p>Transport-agnostic by design, this matcher is reused by any transport that populates {@code attributes} (HTTP
 * status code today, gRPC status / MQ reason code / script exit code in the future). It operates on
 * {@code List<ConsumedRecord<V>>} for any {@code V} since it never inspects {@link ConsumedRecord#getValue()}.
 *
 * <p>Matching rule: every key in {@code expectedAttributes} must be present in the actual record's {@code attributes}
 * with an exactly-equal String value (case-sensitive). Only the first record in the list is used (single-record
 * semantics, consistent with {@link FileRecordMatcher} and {@link KeyRecordMatcher}).
 *
 * @param <V> the record value type, irrelevant to this matcher, kept for interface compatibility
 * @since 1.1.1
 */
@Slf4j
public class AttributeRecordMatcher<V> implements RecordMatcher<V> {

    @Override
    public MatchResult match(List<ConsumedRecord<V>> records, MatchContext context) throws ComparisonException {

        Map<String, String> expected = context.getExpectedAttributes();

        if (expected == null || expected.isEmpty()) {
            log.debug("No expected attributes configured, nothing to assert.");
            return MatchResult.pass();
        }

        if (records == null || records.isEmpty()) {
            throw new ComparisonException("AttributeRecordMatcher requires at least one record to compare.");
        }

        Map<String, String> actual = records.get(0).getAttributes();
        Map<String, String> safeActual = actual != null ? actual : Collections.emptyMap();

        List<String> diffs = new ArrayList<>();
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String actualValue = safeActual.get(entry.getKey());
            if (!Objects.equals(entry.getValue(), actualValue)) {
                diffs.add(
                        String.format("%s: expected '%s' but was '%s'", entry.getKey(), entry.getValue(), actualValue));
            }
        }

        if (!diffs.isEmpty()) {
            String diff = String.join(System.lineSeparator(), diffs);
            log.error("Record attribute mismatch:\n{}", diff);
            return MatchResult.fail(diff, expected.toString(), safeActual.toString());
        }

        log.info("All {} expected record attribute(s) matched.", expected.size());
        return MatchResult.pass(expected.toString(), safeActual.toString());
    }
}
