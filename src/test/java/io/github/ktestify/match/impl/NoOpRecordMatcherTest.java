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

import static io.github.ktestify.match.impl.MatcherTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import io.github.ktestify.exceptions.ComparisonException;
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NoOpRecordMatcher")
class NoOpRecordMatcherTest {

    private final NoOpRecordMatcher<String> matcher = new NoOpRecordMatcher<>();

    @Test
    @DisplayName("always returns a passing result regardless of record content")
    void alwaysPasses() throws ComparisonException {
        MatchResult result =
                matcher.match(rawRecord("anything"), MatchContext.builder().build());
        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("returns a passing result when no matchFilePath is configured")
    void passesWithNoContext() throws ComparisonException {
        MatchResult result = matcher.match(rawRecord(""), MatchContext.builder().build());
        assertTrue(result.isPassed());
        assertTrue(result.getDiff().isEmpty());
    }
}
