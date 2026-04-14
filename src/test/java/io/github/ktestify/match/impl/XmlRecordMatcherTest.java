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
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("XmlRecordMatcher")
class XmlRecordMatcherTest {

    private final XmlRecordMatcher matcher = new XmlRecordMatcher();

    private static final String MATCHING_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <order>
                <orderId>ORD-001</orderId>
                <customerId>CUST-42</customerId>
                <amount>99.99</amount>
                <status>CREATED</status>
            </order>""";

    // Same structure as expected-order.xml, only <status> differs
    private static final String STATUS_ONLY_DIFF_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <order>
                <orderId>ORD-001</orderId>
                <customerId>CUST-42</customerId>
                <amount>99.99</amount>
                <status>SHIPPED</status>
            </order>""";

    // Extra <timestamp> element AND different <status>
    private static final String DIFFERENT_STATUS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <order>
                <orderId>ORD-001</orderId>
                <customerId>CUST-42</customerId>
                <amount>99.99</amount>
                <status>DIFFERENT</status>
                <timestamp>2026-01-01T00:00:00Z</timestamp>
            </order>""";

    @Nested
    @DisplayName("Passing scenarios")
    class Passing {

        @Test
        @DisplayName("passes when XML documents are identical")
        void identicalXml() throws ComparisonException {
            MatchResult result = matcher.match(rawRecord(MATCHING_XML), ctxWithFile("expected-order.xml"));
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("passes when only <status> differs and it is excluded")
        void passesExcludingStatus() throws ComparisonException {
            MatchResult result = matcher.match(
                    rawRecord(STATUS_ONLY_DIFF_XML), ctxWithFileAndExclusions("expected-order.xml", List.of("status")));
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("passes when extra element and differing element are both excluded")
        void passesExcludingExtraElementAndDiff() throws ComparisonException {
            // DIFFERENT_STATUS_XML has an extra <timestamp> AND a different <status>
            // XMLUtils must suppress the child-count difference too
            MatchResult result = matcher.match(
                    rawRecord(DIFFERENT_STATUS_XML),
                    ctxWithFileAndExclusions("expected-order.xml", List.of("status", "timestamp")));
            assertTrue(result.isPassed());
        }
    }

    @Nested
    @DisplayName("Failing scenarios")
    class Failing {

        @Test
        @DisplayName("fails when XML content differs and nothing is excluded")
        void differentXml() throws ComparisonException {
            MatchResult result = matcher.match(rawRecord(DIFFERENT_STATUS_XML), ctxWithFile("expected-order.xml"));
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("fails when differing element is not in the exclusion list")
        void failsWhenNotExcluded() throws ComparisonException {
            // status differs but only orderId is excluded — should still fail
            MatchResult result = matcher.match(
                    rawRecord(STATUS_ONLY_DIFF_XML),
                    ctxWithFileAndExclusions("expected-order.xml", List.of("orderId")));
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("throws ComparisonException when matchFilePath is missing")
        void throwsWhenNoFilePath() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            rawRecord(MATCHING_XML), MatchContext.builder().build()));
        }
    }
}
