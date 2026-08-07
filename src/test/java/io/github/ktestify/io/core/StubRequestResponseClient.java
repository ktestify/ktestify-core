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

import java.time.Instant;
import java.util.*;

/**
 * Minimal in-memory {@link RequestResponseClient} used by the {@code io.core} unit tests.
 *
 * <p>Returns a queue of pre-programmed responses, one per {@link #execute(String)} call, and repeats the last one once
 * the queue is exhausted. Records how many times it was executed and whether it was closed.
 */
class StubRequestResponseClient implements RequestResponseClient<String, String> {

    private final Deque<Object> scripted = new ArrayDeque<>();
    private Object last;
    private int executeCount;
    private boolean closed;

    /** Queues a successful response carrying the given attributes. */
    StubRequestResponseClient thenReturn(Map<String, String> attributes) {
        scripted.add(record(attributes));
        return this;
    }

    /** Queues a transport-level failure. */
    StubRequestResponseClient thenFail(String message) {
        scripted.add(new FetchException(message));
        return this;
    }

    @Override
    public List<ConsumedRecord<String>> execute(String request) throws FetchException {
        executeCount++;
        Object next = scripted.isEmpty() ? last : scripted.poll();
        last = next;
        if (next instanceof FetchException failure) {
            throw failure;
        }
        @SuppressWarnings("unchecked")
        List<ConsumedRecord<String>> records = (List<ConsumedRecord<String>>) next;
        return records;
    }

    @Override
    public void close() {
        closed = true;
    }

    int getExecuteCount() {
        return executeCount;
    }

    boolean isClosed() {
        return closed;
    }

    private static List<ConsumedRecord<String>> record(Map<String, String> attributes) {
        return List.of(
                new ConsumedRecord<>("stub", 0, -1L, "GET", "body", Instant.now(), Collections.emptyMap(), attributes));
    }
}
