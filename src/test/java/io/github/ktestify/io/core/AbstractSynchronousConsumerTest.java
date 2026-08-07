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
package io.github.ktestify.io.core;

import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.exceptions.FetchException;
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import io.github.ktestify.match.RecordMatcher;
import io.github.ktestify.models.ConsumedRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("AbstractSynchronousConsumer")
class AbstractSynchronousConsumerTest {

    private static final String REQUEST = "GET /orders";

    private RequestResponseClient<String, String> client;
    private RecordMatcher<String> matcher;
    private MatchContext matchContext;
    private TestConsumer consumer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        client = mock(RequestResponseClient.class);
        matcher = mock(RecordMatcher.class);
        matchContext = MatchContext.builder()
                .expectedAttributes(Map.of("statusCode", "200"))
                .build();
        consumer = new TestConsumer(client, matcher, matchContext);
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Nested
    @DisplayName("call — success")
    class Success {

        @Test
        @DisplayName("returns true when the matcher passes")
        void returnsTrueWhenMatcherPasses() {
            List<ConsumedRecord<String>> records = records();
            when(client.execute(REQUEST)).thenReturn(records);
            when(matcher.match(records, matchContext)).thenReturn(MatchResult.pass());

            assertTrue(consumer.call());
        }

        @Test
        @DisplayName("returns false when the matcher fails")
        void returnsFalseWhenMatcherFails() {
            List<ConsumedRecord<String>> records = records();
            when(client.execute(REQUEST)).thenReturn(records);
            when(matcher.match(records, matchContext)).thenReturn(MatchResult.fail("boom", "200", "500"));

            assertFalse(consumer.call());
        }

        @Test
        @DisplayName("sends the request built by buildRequest and matches with buildMatchContext")
        void wiresRequestAndContext() {
            when(client.execute(anyString())).thenReturn(records());
            when(matcher.match(any(), any())).thenReturn(MatchResult.pass());

            consumer.call();

            verify(client).execute(eq(REQUEST));
            verify(matcher).match(any(), eq(matchContext));
        }

        @Test
        @DisplayName("does not close the client — the client outlives a single call")
        void doesNotCloseClient() {
            when(client.execute(anyString())).thenReturn(records());
            when(matcher.match(any(), any())).thenReturn(MatchResult.pass());

            consumer.call();

            verify(client, never()).close();
        }
    }

    // =========================================================================
    // Failure path
    // =========================================================================

    @Nested
    @DisplayName("call — failure")
    class Failure {

        @Test
        @DisplayName("wraps a FetchException into a ConsumerException")
        void wrapsFetchException() {
            when(client.execute(anyString())).thenThrow(new FetchException("connection refused"));

            ConsumerException exception = assertThrows(ConsumerException.class, () -> consumer.call());
            assertEquals("connection refused", exception.getMessage());
            verifyNoInteractions(matcher);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static List<ConsumedRecord<String>> records() {
        return List.of(new ConsumedRecord<>(
                "http://localhost/orders",
                0,
                -1L,
                "GET",
                "{}",
                Instant.now(),
                Collections.emptyMap(),
                Map.of("statusCode", "200")));
    }

    /** Minimal concrete subclass exercising the abstract extension points. */
    private static final class TestConsumer extends AbstractSynchronousConsumer<String, String> {

        private final MatchContext matchContext;

        private TestConsumer(
                RequestResponseClient<String, String> client, RecordMatcher<String> matcher, MatchContext context) {
            super(Collections.emptyMap(), client, matcher);
            this.matchContext = context;
        }

        @Override
        protected String buildRequest() {
            return REQUEST;
        }

        @Override
        protected MatchContext buildMatchContext() {
            return matchContext;
        }
    }
}
