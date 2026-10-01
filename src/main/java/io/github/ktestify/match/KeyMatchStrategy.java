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
package io.github.ktestify.match;

/**
 * Strategies for comparing a record key against an expected key.
 *
 * <p>Used in two places:
 *
 * <ul>
 *   <li>{@code KafkaRecordFetcher.passesKeyFilter()} as a pre-filter during the Kafka poll loop
 *   <li>{@code KeyRecordMatcher}, {@code FileKeyRecordMatcher}, {@code AvroKeyRecordMatcher}, and
 *       {@code AvroFileKeyRecordMatcher} as the post-fetch assertion
 * </ul>
 *
 * <p>The default strategy is {@link #EXACT}, which preserves the original {@code String.equals()} behavior. Other
 * strategies allow matching dynamically generated keys by prefix, suffix, substring, or regular expression.
 *
 * @since 1.1.1
 */
public enum KeyMatchStrategy {

    /**
     * Exact equality: {@code expected.equals(actual)}.
     *
     * <p>This is the default and preserves backward compatibility for feature files that do not specify a
     * {@code keyMatchStrategy} column.
     *
     * @since 1.1.1
     */
    EXACT {
        @Override
        public boolean matches(String expected, String actual) {
            return expected != null && expected.equals(actual);
        }
    },

    /**
     * Substring match: {@code actual.contains(expected)}.
     *
     * <p>Useful when the record key contains a known fragment embedded in a larger dynamically generated value.
     *
     * @since 1.1.1
     */
    CONTAINS {
        @Override
        public boolean matches(String expected, String actual) {
            return expected != null && actual != null && actual.contains(expected);
        }
    },

    /**
     * Prefix match: {@code actual.startsWith(expected)}.
     *
     * <p>Useful when the record key starts with a known prefix followed by a dynamically generated suffix (e.g.
     * {@code ORD-<uuid>}).
     *
     * @since 1.1.1
     */
    STARTS_WITH {
        @Override
        public boolean matches(String expected, String actual) {
            return expected != null && actual != null && actual.startsWith(expected);
        }
    },

    /**
     * Suffix match: {@code actual.endsWith(expected)}.
     *
     * <p>Useful when the record key ends with a known suffix preceded by a dynamically generated prefix.
     *
     * @since 1.1.1
     */
    ENDS_WITH {
        @Override
        public boolean matches(String expected, String actual) {
            return expected != null && actual != null && actual.endsWith(expected);
        }
    },

    /**
     * Regular expression match: {@code actual.matches(expected)}.
     *
     * <p>The {@code expected} string is interpreted as a Java regular expression. Useful for arbitrary patterns such as
     * {@code ORD-\d{6}} that cannot be expressed with prefix, suffix, or substring matching.
     *
     * @since 1.1.1
     */
    REGEX {
        @Override
        public boolean matches(String expected, String actual) {
            return expected != null && actual != null && actual.matches(expected);
        }
    };

    /**
     * Tests whether the {@code actual} record key satisfies this strategy given the {@code expected} key.
     *
     * @param expected the expected key value (or pattern for {@link #REGEX})
     * @param actual the actual record key, may be {@code null} when the Kafka record has no key
     * @return {@code true} if the actual key matches according to this strategy
     * @since 1.1.1
     */
    public abstract boolean matches(String expected, String actual);

    /**
     * Parses a strategy name from a DataTable column value.
     *
     * <p>Matching is case-insensitive and tolerant of hyphens, underscores, and spaces. For example,
     * {@code "starts_with"}, {@code "starts-with"}, and {@code "STARTS WITH"} all resolve to {@link #STARTS_WITH}.
     *
     * @param value the raw column value, may be {@code null} or blank
     * @return the parsed strategy, or {@link #EXACT} when the value is {@code null}, blank, or unrecognized
     * @since 1.1.1
     */
    public static KeyMatchStrategy fromString(String value) {
        if (value == null || value.isBlank()) {
            return EXACT;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        try {
            return KeyMatchStrategy.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return EXACT;
        }
    }
}
