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

import io.github.ktestify.exceptions.FetchException;
import io.github.ktestify.models.ConsumedRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RequestResponseClient contract")
class RequestResponseClientTest {

    @Test
    @DisplayName("execute returns a non-empty list of ConsumedRecord")
    void executeReturnsRecords() {
        try (StubRequestResponseClient client =
                new StubRequestResponseClient().thenReturn(Map.of("statusCode", "200"))) {
            List<ConsumedRecord<String>> records = client.execute("request");

            assertNotNull(records);
            assertEquals(1, records.size());
            assertEquals("200", records.get(0).getAttributes().get("statusCode"));
        }
    }

    @Test
    @DisplayName("execute propagates transport failures as FetchException")
    void executePropagatesFetchException() {
        StubRequestResponseClient client = new StubRequestResponseClient().thenFail("connection refused");

        FetchException exception = assertThrows(FetchException.class, () -> client.execute("request"));
        assertEquals("connection refused", exception.getMessage());
    }

    @Test
    @DisplayName("client is AutoCloseable and close() releases resources")
    void closeReleasesResources() {
        StubRequestResponseClient client = new StubRequestResponseClient().thenReturn(Map.of());

        assertFalse(client.isClosed());
        client.close();
        assertTrue(client.isClosed());
    }

    @Test
    @DisplayName("close() is idempotent")
    void closeIsIdempotent() {
        StubRequestResponseClient client = new StubRequestResponseClient().thenReturn(Map.of());

        client.close();
        assertDoesNotThrow(client::close);
        assertTrue(client.isClosed());
    }
}
