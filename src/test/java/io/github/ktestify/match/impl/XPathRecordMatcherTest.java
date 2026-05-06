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

import static io.github.ktestify.match.impl.MatcherTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import io.github.ktestify.exceptions.ComparisonException;
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("XPathRecordMatcher")
class XPathRecordMatcherTest {

    private final XPathRecordMatcher matcher = new XPathRecordMatcher();

    private static final String MATCHING_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <order>
                <orderId>ORD-001</orderId>
                <customerId>CUST-42</customerId>
                <amount>99.99</amount>
                <status>CREATED</status>
            </order>""";

    private static final String DIFFERENT_STATUS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <order>
                <orderId>ORD-001</orderId>
                <customerId>CUST-42</customerId>
                <amount>99.99</amount>
                <status>SHIPPED</status>
            </order>""";

    @Nested
    @DisplayName("Passing scenarios")
    class Passing {

        @Test
        @DisplayName("passes when XPath-selected node matches expected")
        void xpathNodeMatches() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePath(resourcePath("expected-order.xml"))
                    .excludedFields(List.of("//order/orderId"))
                    .build();
            MatchResult result = matcher.match(rawRecord(MATCHING_XML), ctx);
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("passes when all provided XPaths match")
        void allXpathsMatch() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePath(resourcePath("expected-order.xml"))
                    .excludedFields(List.of("//order/orderId", "//order/customerId"))
                    .build();
            MatchResult result = matcher.match(rawRecord(MATCHING_XML), ctx);
            assertTrue(result.isPassed());
        }
    }

    @Nested
    @DisplayName("Failing scenarios")
    class Failing {

        @Test
        @DisplayName("fails when XPath-selected node differs")
        void xpathNodeDiffers() throws ComparisonException {
            MatchContext ctx = MatchContext.builder()
                    .matchFilePath(resourcePath("expected-order.xml"))
                    .excludedFields(List.of("//order/status"))
                    .build();
            MatchResult result = matcher.match(rawRecord(DIFFERENT_STATUS_XML), ctx);
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("throws ComparisonException when XPath list is not set")
        void throwsWhenNoXPaths() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(rawRecord(MATCHING_XML), ctxWithFile("expected-order.xml")));
        }

        @Test
        @DisplayName("throws ComparisonException when matchFilePath is not set")
        void throwsWhenNoFilePath() {
            assertThrows(
                    ComparisonException.class,
                    () -> matcher.match(
                            rawRecord(MATCHING_XML),
                            MatchContext.builder()
                                    .excludedFields(List.of("//order/orderId"))
                                    .build()));
        }
    }
}
