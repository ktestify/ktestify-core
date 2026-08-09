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

import static org.junit.jupiter.api.Assertions.*;

import io.github.ktestify.exceptions.FetchException;
import io.github.ktestify.models.ConsumedRecord;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PollingRequestResponseClient")
class PollingRequestResponseClientTest {

    /** Short intervals keep the suite fast, no real waiting of any significance. */
    private static final long TIMEOUT_MS = 300L;

    private static final long POLL_INTERVAL_MS = 20L;

    private static final Predicate<List<ConsumedRecord<String>>> STATUS_200 =
            records -> "200".equals(records.get(0).getAttributes().get("statusCode"));

    // =========================================================================
    // Predicate satisfied
    // =========================================================================

    @Nested
    @DisplayName("Predicate satisfied")
    class PredicateSatisfied {

        @Test
        @DisplayName("returns immediately when the first attempt already passes")
        void returnsOnFirstAttempt() {
            StubRequestResponseClient delegate =
                    new StubRequestResponseClient().thenReturn(Map.of("statusCode", "200"));

            try (PollingRequestResponseClient<String, String> polling =
                    new PollingRequestResponseClient<>(delegate, STATUS_200, TIMEOUT_MS, POLL_INTERVAL_MS)) {

                List<ConsumedRecord<String>> result = polling.execute("request");

                assertEquals("200", result.get(0).getAttributes().get("statusCode"));
                assertEquals(1, delegate.getExecuteCount());
            }
        }

        @Test
        @DisplayName("retries until the predicate passes, then stops")
        void retriesUntilPredicatePasses() {
            StubRequestResponseClient delegate = new StubRequestResponseClient()
                    .thenReturn(Map.of("statusCode", "404"))
                    .thenReturn(Map.of("statusCode", "404"))
                    .thenReturn(Map.of("statusCode", "200"))
                    .thenReturn(Map.of("statusCode", "500"));

            try (PollingRequestResponseClient<String, String> polling =
                    new PollingRequestResponseClient<>(delegate, STATUS_200, TIMEOUT_MS, POLL_INTERVAL_MS)) {

                List<ConsumedRecord<String>> result = polling.execute("request");

                assertEquals("200", result.get(0).getAttributes().get("statusCode"));
                assertEquals(3, delegate.getExecuteCount());
            }
        }

        @Test
        @DisplayName("recovers from a transient transport failure")
        void recoversFromTransientFailure() {
            StubRequestResponseClient delegate = new StubRequestResponseClient()
                    .thenFail("connection refused")
                    .thenReturn(Map.of("statusCode", "200"));

            try (PollingRequestResponseClient<String, String> polling =
                    new PollingRequestResponseClient<>(delegate, STATUS_200, TIMEOUT_MS, POLL_INTERVAL_MS)) {

                assertEquals(
                        "200", polling.execute("request").get(0).getAttributes().get("statusCode"));
            }
        }
    }

    // =========================================================================
    // Timeout
    // =========================================================================

    @Nested
    @DisplayName("Timeout")
    class Timeout {

        @Test
        @DisplayName("returns the last result obtained so the matcher can report the real final state")
        void returnsLastResultOnTimeout() {
            StubRequestResponseClient delegate =
                    new StubRequestResponseClient().thenReturn(Map.of("statusCode", "503"));

            try (PollingRequestResponseClient<String, String> polling =
                    new PollingRequestResponseClient<>(delegate, STATUS_200, TIMEOUT_MS, POLL_INTERVAL_MS)) {

                List<ConsumedRecord<String>> result = polling.execute("request");

                assertEquals("503", result.get(0).getAttributes().get("statusCode"));
                assertTrue(delegate.getExecuteCount() > 1, "expected several polling attempts");
            }
        }

        @Test
        @DisplayName("propagates the delegate failure when no result was ever obtained")
        void propagatesFailureWhenNoResult() {
            StubRequestResponseClient delegate = new StubRequestResponseClient().thenFail("connection refused");

            try (PollingRequestResponseClient<String, String> polling =
                    new PollingRequestResponseClient<>(delegate, STATUS_200, TIMEOUT_MS, POLL_INTERVAL_MS)) {

                FetchException exception = assertThrows(FetchException.class, () -> polling.execute("request"));
                assertEquals("connection refused", exception.getMessage());
            }
        }
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("close() delegates to the wrapped client")
        void closeDelegates() {
            StubRequestResponseClient delegate = new StubRequestResponseClient().thenReturn(Map.of());
            PollingRequestResponseClient<String, String> polling =
                    new PollingRequestResponseClient<>(delegate, STATUS_200, TIMEOUT_MS, POLL_INTERVAL_MS);

            polling.close();

            assertTrue(delegate.isClosed());
        }
    }
}
