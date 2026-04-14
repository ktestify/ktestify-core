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

import lombok.Value;

/**
 * Structured result of a {@code RecordMatcher} assertion.
 *
 * <p>Carries more information than a plain {@code boolean} so that test-framework adapters (Cucumber, Robot Framework,
 * …) can produce meaningful failure messages without knowing anything about how the match was performed.
 *
 * <pre>
 * // Usage in a Cucumber step definition:
 * MatchResult result = matcher.match(records);
 * assertThat(result.isPassed())
 *     .as(result.getDiff())
 *     .isTrue();
 * </pre>
 *
 * @since 0.3.0
 */
@Value
public class MatchResult {

    /** Whether the assertion passed. */
    boolean passed;

    /**
     * Human-readable diff shown in the test report when the assertion fails. Empty string when {@link #passed} is
     * {@code true}.
     */
    String diff;

    /** The expected value used during comparison. May be empty if not applicable. */
    String expected;

    /** The actual value received from the source. May be empty if not applicable. */
    String actual;

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a passing result with no diff information.
     *
     * @return a passing {@code MatchResult}
     */
    public static MatchResult pass() {
        return new MatchResult(true, "", "", "");
    }

    /**
     * Creates a passing result with the values that were compared, useful for reporting.
     *
     * @param expected the expected value
     * @param actual the actual value
     * @return a passing {@code MatchResult}
     */
    public static MatchResult pass(String expected, String actual) {
        return new MatchResult(true, "", expected, actual);
    }

    /**
     * Creates a failing result with a diff and the compared values.
     *
     * @param diff human-readable description of the difference
     * @param expected the expected value
     * @param actual the actual value
     * @return a failing {@code MatchResult}
     */
    public static MatchResult fail(String diff, String expected, String actual) {
        return new MatchResult(false, diff, expected, actual);
    }

    /**
     * Creates a failing result with only a message (e.g. when values are not representable as plain strings).
     *
     * @param message the failure message
     * @return a failing {@code MatchResult}
     */
    public static MatchResult fail(String message) {
        return new MatchResult(false, message, "", "");
    }
}
