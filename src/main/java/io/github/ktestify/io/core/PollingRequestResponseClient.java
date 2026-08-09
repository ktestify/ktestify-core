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
import java.util.List;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic {@link RequestResponseClient} decorator that retries {@link #execute(Object)} against a delegate client until
 * a caller-supplied predicate on the resulting records passes, or a timeout elapses.
 *
 * <p>Useful for "eventually consistent" APIs, for example asserting that an HTTP endpoint eventually returns 200 once
 * an asynchronous side-effect completes, without every plugin re-implementing its own poll loop.
 *
 * <p>On timeout the <em>last</em> result obtained is returned rather than throwing, so the subsequent
 * {@code RecordMatcher} failure message shows the real final state instead of a generic timeout string. A
 * {@link FetchException} raised by the delegate is only propagated when no successful attempt has been made yet.
 *
 * @param <Req> the request type
 * @param <V> the record value type
 * @since 1.1.1
 */
@Slf4j
public class PollingRequestResponseClient<Req, V> implements RequestResponseClient<Req, V> {

    private final RequestResponseClient<Req, V> delegate;
    private final Predicate<List<ConsumedRecord<V>>> untilPredicate;
    private final long timeoutMs;
    private final long pollIntervalMs;

    /**
     * Creates a polling decorator.
     *
     * @param delegate the underlying client actually performing the call
     * @param untilPredicate the success condition evaluated against each result
     * @param timeoutMs total time budget in milliseconds
     * @param pollIntervalMs sleep between attempts in milliseconds
     */
    public PollingRequestResponseClient(
            RequestResponseClient<Req, V> delegate,
            Predicate<List<ConsumedRecord<V>>> untilPredicate,
            long timeoutMs,
            long pollIntervalMs) {
        this.delegate = delegate;
        this.untilPredicate = untilPredicate;
        this.timeoutMs = timeoutMs;
        this.pollIntervalMs = pollIntervalMs;
    }

    /**
     * Repeatedly executes {@code request} until the predicate passes or the timeout elapses.
     *
     * @param request the request to send
     * @return the first result satisfying the predicate, or the last result obtained before the timeout
     * @throws FetchException if the delegate fails and no result has ever been obtained, or the thread is interrupted
     */
    @Override
    public List<ConsumedRecord<V>> execute(Req request) throws FetchException {
        long deadlineMs = System.currentTimeMillis() + timeoutMs;
        List<ConsumedRecord<V>> lastResult = null;
        FetchException lastFailure = null;

        do {
            try {
                List<ConsumedRecord<V>> result = delegate.execute(request);
                lastResult = result;
                lastFailure = null;
                if (untilPredicate.test(result)) {
                    log.debug("Polling predicate satisfied.");
                    return result;
                }
            } catch (FetchException e) {
                log.debug("Polling attempt failed: {}", e.getMessage());
                lastFailure = e;
            }

            if (System.currentTimeMillis() + pollIntervalMs >= deadlineMs) {
                break;
            }
            sleep(pollIntervalMs);
        } while (true);

        if (lastResult == null) {
            throw lastFailure != null
                    ? lastFailure
                    : new FetchException("Polling produced no result within " + timeoutMs + "ms.");
        }

        log.warn("Polling predicate never satisfied within {}ms, returning the last result obtained.", timeoutMs);
        return lastResult;
    }

    /** Closes the delegate client. Idempotent. */
    @Override
    public void close() {
        delegate.close();
    }

    private static void sleep(long ms) throws FetchException {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FetchException("Interrupted while polling for a response.");
        }
    }
}
