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
 * Strategies for comparing a batch of consumed records against a list of expected files.
 *
 * <p>The default strategy is {@link #POSITIONAL}, which requires each consumed record at index {@code i} to match the
 * expected file at index {@code i}. {@link #LAZY} performs order-independent 1-to-1 matching without replacement.
 *
 * @since 1.2.5
 */
public enum BatchMatchStrategy {

    /**
     * Positional matching: record at index {@code i} must match the expected file at index {@code i}.
     *
     * <p>This is the default strategy and preserves backward compatibility for callers that do not specify a batch
     * match strategy.
     *
     * @since 1.2.5
     */
    POSITIONAL,

    /**
     * Lazy (unordered) matching: order-independent 1-to-1 matching without replacement.
     *
     * <p>Each consumed record is compared against all not-yet-matched expected files; the first matching unused file is
     * claimed for that record.
     *
     * @since 1.2.5
     */
    LAZY;

    /**
     * Parses a batch match strategy from a string or DataTable column value.
     *
     * <p>Matching is case-insensitive and tolerant of hyphens, underscores, and spaces. Supported aliases:
     *
     * <ul>
     *   <li>{@link #POSITIONAL}: {@code "POSITIONAL"}, {@code "ORDERED"}, {@code "INDEX"}, {@code "STRICT"}
     *   <li>{@link #LAZY}: {@code "LAZY"}, {@code "FUZZY"}, {@code "UNORDERED"}, {@code "ANY_ORDER"}
     * </ul>
     *
     * @param value the raw strategy name, may be {@code null} or blank
     * @return the parsed strategy, or {@link #POSITIONAL} when the value is {@code null}, blank, or unrecognized
     * @since 1.2.5
     */
    public static BatchMatchStrategy fromString(String value) {
        if (value == null || value.isBlank()) {
            return POSITIONAL;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "POSITIONAL", "ORDERED", "INDEX", "STRICT" -> POSITIONAL;
            case "LAZY", "FUZZY", "UNORDERED", "ANY_ORDER" -> LAZY;
            default -> POSITIONAL;
        };
    }
}
