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

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable value object that carries all configuration needed by a {@code RecordMatcher}.
 *
 * <p>Replaces the raw {@code Map<String, String>} that was previously threaded through the entire consumer/matcher
 * stack. Each field is explicitly typed and documented so that matcher implementations are self-documenting and easy to
 * test.
 *
 * <p>Build with the fluent builder:
 *
 * <pre>
 * MatchContext ctx = MatchContext.builder()
 *     .matchFilePaths(List.of("src/test/resources/expected/order-created.json"))
 *     .excludedFields(List.of("timestamp", "correlationId"))
 *     .strictMatching(true)
 *     .build();
 * </pre>
 *
 * @since 0.3.0
 */
@Value
@Builder
public class MatchContext {

    /**
     * Logical name of the match strategy to use. Resolved by {@code RecordMatcherFactory} to a concrete
     * {@code RecordMatcher}. Examples: {@code "methodMatchFile"}, {@code "methodMatchXML"}.
     */
    String matchMethod;

    /**
     * Paths to the files containing the expected record content. Single-record matchers use {@code get(0)}, batch
     * matchers iterate by index. Interpreted relative to the configured assets directory when set.
     */
    @Builder.Default
    List<String> matchFilePaths = Collections.emptyList();

    /** Fields / keys to exclude from the comparison. Useful for volatile fields such as timestamps or generated IDs. */
    @Builder.Default
    List<String> excludedFields = Collections.emptyList();

    /**
     * When {@code true}, every field in the expected record must be present in the actual record with exactly the same
     * value. When {@code false}, extra fields in the actual record are tolerated.
     */
    boolean strictMatching;

    /** Optional key whose value must match a specific value (key-value matching mode). */
    String matchKey;

    /** Expected value for {@link #matchKey}. */
    String matchValue;

    /**
     * Convenience accessor for single-record matchers. Returns the first element of {@link #matchFilePaths}, or
     * {@code null} if the list is empty.
     */
    public String getMatchFilePath() {
        return matchFilePaths != null && !matchFilePaths.isEmpty() ? matchFilePaths.get(0) : null;
    }

    /**
     * Extends the Lombok-generated builder with a single-path convenience setter. Callers that set only one expected
     * file can use {@code .matchFilePath("path/to/file")} instead of {@code .matchFilePaths(List.of(...))}.
     */
    public static class MatchContextBuilder {

        /**
         * Convenience setter for single-record matchers. Wraps {@code path} in an immutable single-element list and
         * delegates to {@link #matchFilePaths(List)}.
         */
        public MatchContextBuilder matchFilePath(String path) {
            return matchFilePaths(path != null ? List.of(path) : Collections.emptyList());
        }
    }
}
