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
import io.github.ktestify.exceptions.FetchTimeoutException;
import io.github.ktestify.models.ConsumedRecord;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic {@link RequestResponseClient} decorator that retries {@link #execute(Object)} against a delegate client until
 * a caller-supplied predicate on the resulting records passes, or a strict total timeout elapses.
 *
 * <p>Useful for "eventually consistent" APIs, for example asserting that an HTTP endpoint eventually returns 200 once
 * an asynchronous side-effect completes, without every plugin re-implementing its own poll loop.
 *
 * <p>The {@code timeoutMs} budget bounds the entire polling operation, including every individual delegate invocation.
 * If an in-flight delegate call exceeds the remaining deadline, it is cancelled via interruption. On timeout the
 * <em>last</em> result obtained is returned rather than throwing, so the subsequent {@code RecordMatcher} failure
 * message shows the real final state instead of a generic timeout string. When no response has ever been obtained, a
 * {@link FetchTimeoutException} is thrown if an attempt timed out against the total deadline, or the delegate's
 * {@link FetchException} is propagated if attempts failed before the deadline.
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
     * @param timeoutMs strict total time budget in milliseconds, including delegate calls
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
     * Repeatedly executes {@code request} until the predicate passes or the strict total timeout elapses.
     *
     * @param request the request to send
     * @return the first result satisfying the predicate, or the last result obtained before the timeout
     * @throws FetchTimeoutException if the total deadline expires during a delegate call and no result was ever
     *     obtained
     * @throws FetchException if the delegate fails and no result has ever been obtained, or the thread is interrupted
     */
    @Override
    public List<ConsumedRecord<V>> execute(Req request) throws FetchException {
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(0L, timeoutMs));
        long pollIntervalNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, pollIntervalMs));
        List<ConsumedRecord<V>> lastResult = null;
        FetchException lastFailure = null;
        boolean timedOutInDelegate = false;
        int attempt = 0;

        log.info("Starting polling request (timeout={}ms, pollInterval={}ms).", timeoutMs, pollIntervalMs);

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "ktestify-polling-worker");
            thread.setDaemon(true);
            return thread;
        });

        try {
            while (true) {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0L) {
                    break;
                }

                attempt++;
                Future<List<ConsumedRecord<V>>> future = executor.submit(() -> delegate.execute(request));
                try {
                    List<ConsumedRecord<V>> result = future.get(remainingNanos, TimeUnit.NANOSECONDS);
                    lastResult = result;
                    lastFailure = null;
                    if (untilPredicate.test(result)) {
                        log.info("Polling predicate satisfied on attempt #{}.", attempt);
                        return result;
                    }
                    log.debug("Polling attempt #{} did not satisfy predicate; retrying...", attempt);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    timedOutInDelegate = true;
                    log.warn(
                            "Polling attempt #{} exceeded remaining deadline (total timeout={}ms); cancelling.",
                            attempt,
                            timeoutMs);
                    break;
                } catch (InterruptedException e) {
                    future.cancel(true);
                    Thread.currentThread().interrupt();
                    throw new FetchException("Interrupted while polling for a response.", e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    switch (cause) {
                        case FetchException fe -> {
                            log.debug("Polling attempt #{} failed: {}", attempt, fe.getMessage());
                            lastFailure = fe;
                        }
                        case RuntimeException re -> throw re;
                        case Error err -> throw err;
                        case null, default ->
                            lastFailure = new FetchException("Polling attempt #" + attempt + " failed.", cause);
                    }
                }

                long afterAttemptRemainingNanos = deadlineNanos - System.nanoTime();
                if (afterAttemptRemainingNanos <= pollIntervalNanos) {
                    break;
                }
                sleepNanos(pollIntervalNanos);
            }
        } finally {
            executor.shutdownNow();
        }

        if (lastResult == null) {
            log.error(
                    "Polling failed after {} attempt(s) within {}ms without obtaining any response.",
                    attempt,
                    timeoutMs);
            if (timedOutInDelegate) {
                throw new FetchTimeoutException(
                        "Polling timed out after " + timeoutMs + "ms during attempt #" + attempt + ".", lastFailure);
            }
            if (lastFailure != null) {
                throw lastFailure;
            }
            throw new FetchTimeoutException("Polling produced no result within " + timeoutMs + "ms.");
        }

        log.warn(
                "Polling predicate never satisfied after {} attempt(s) within {}ms, returning the last result obtained.",
                attempt,
                timeoutMs);
        return lastResult;
    }

    /** Closes the delegate client. Idempotent. */
    @Override
    public void close() {
        delegate.close();
    }

    private static void sleepNanos(long nanos) throws FetchException {
        if (nanos <= 0L) {
            return;
        }
        try {
            TimeUnit.NANOSECONDS.sleep(nanos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FetchException("Interrupted while polling for a response.", e);
        }
    }
}
