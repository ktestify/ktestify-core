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
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Thin coordinator that wires a {@link RequestResponseClient} (transport) with a {@link RecordMatcher} (assertion) for
 * synchronous, caller-initiated transports (HTTP, gRPC, SOAP, …), and exposes a single {@link #call()} entry point.
 *
 * <p>Sibling to {@code io.github.ktestify.io.kafka.AbstractKafkaConsumer}: same execute → match → return shape, adapted
 * for a transport where the caller supplies the request explicitly instead of the fetcher blocking on a subscription.
 *
 * <p>This class contains <strong>no transport mechanics</strong> and <strong>no matching logic</strong>. Those
 * responsibilities belong exclusively to {@link RequestResponseClient} and {@link RecordMatcher} respectively.
 *
 * <h2>Client lifecycle</h2>
 *
 * <p>Unlike {@code AbstractKafkaConsumer}, this class does <strong>not</strong> close the client in a {@code finally}
 * block. A {@link RequestResponseClient} is expected to be a longer-lived, connection-pooled client (like
 * {@code java.net.http.HttpClient}) owned and closed by the plugin's shared scenario resources, not created and
 * discarded per request.
 *
 * @param <Req> the request type
 * @param <V> the record value type
 * @since 1.1.1
 */
@Slf4j
public abstract class AbstractSynchronousConsumer<Req, V> extends AbstractConsumer {

    protected final RequestResponseClient<Req, V> client;
    protected final RecordMatcher<V> matcher;

    /**
     * Primary constructor.
     *
     * @param properties the consumer properties map
     * @param client the synchronous transport used to send the request
     * @param matcher the assertion strategy to apply to the response
     */
    protected AbstractSynchronousConsumer(
            Map<String, String> properties, RequestResponseClient<Req, V> client, RecordMatcher<V> matcher) {
        super(properties);
        this.client = client;
        this.matcher = matcher;
        log.debug(
                "AbstractSynchronousConsumer created with client '{}' and matcher '{}'",
                client.getClass().getSimpleName(),
                matcher.getClass().getSimpleName());
    }

    /**
     * Builds the transport-specific request to send. Called once per {@link #call()} invocation.
     *
     * @return the request to hand to {@link RequestResponseClient#execute(Object)}
     */
    protected abstract Req buildRequest();

    /**
     * Builds the {@link MatchContext} that is passed to the matcher.
     *
     * <p>Subclasses typically read this from their own context object (analogous to {@code ConsumerContext} for Kafka).
     *
     * @return the match context for this invocation
     */
    protected abstract MatchContext buildMatchContext();

    /**
     * Sends the request, then asserts the response with the configured matcher.
     *
     * <p>Lifecycle:
     *
     * <ol>
     *   <li>Build the request via {@link #buildRequest()}.
     *   <li>Call {@link RequestResponseClient#execute(Object)}: blocks until the response arrives or fails.
     *   <li>Pass the resulting records to {@link RecordMatcher#match(List, MatchContext)}.
     * </ol>
     *
     * @return {@code true} if the matcher passed, {@code false} otherwise
     * @throws ConsumerException if the execute or match step throws an unrecoverable error
     */
    @Override
    public Boolean call() throws ConsumerException {
        try {
            Req request = buildRequest();
            List<ConsumedRecord<V>> records = client.execute(request);
            MatchContext matchContext = buildMatchContext();
            MatchResult result = matcher.match(records, matchContext);

            log.debug("Match result: passed={}, diff={}", result.isPassed(), result.getDiff());

            return result.isPassed();

        } catch (FetchException e) {
            AssertionError assertion = findCause(e, AssertionError.class);
            if (assertion != null) throw assertion;
            throw new ConsumerException(e.getMessage(), e);
        }
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) return type.cast(current);
            current = current.getCause();
        }
        return null;
    }
}
