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
package io.github.ktestify.exceptions;

/**
 * Thrown when a {@link io.github.ktestify.match.RecordMatcher} encounters a configuration error or an unrecoverable
 * problem during the comparison phase.
 *
 * <p>This is a {@link RuntimeException} so callers are not forced to declare it in their {@code throws} clause, but
 * test-framework adapters (Cucumber steps, Robot Framework keywords, …) are expected to catch it and surface the
 * message as a human-readable assertion failure.
 *
 * <p>Typical causes:
 *
 * <ul>
 *   <li>A required {@link io.github.ktestify.match.MatchContext} field (e.g. {@code matchFilePath}, {@code matchKey}) is
 *       {@code null} or blank.
 *   <li>A position descriptor passed to {@link io.github.ktestify.match.impl.FieldsRecordMatcher} is malformed.
 *   <li>An expected file cannot be read or parsed.
 * </ul>
 *
 * @since 0.3.0
 */
public class ComparisonException extends RuntimeException {

    /**
     * Constructs a new {@code ComparisonException} with the supplied detail message.
     *
     * @param message a human-readable description of the comparison failure; shown directly in test reports so it
     *     should be as specific as possible
     */
    public ComparisonException(String message) {
        super(message);
    }
}
