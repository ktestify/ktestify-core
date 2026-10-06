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
package io.github.ktestify.io.kafka;

import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.exceptions.FetchException;
import io.github.ktestify.io.core.AbstractConsumer;
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import io.github.ktestify.match.RecordMatcher;
import io.github.ktestify.models.ConsumedRecord;
import io.github.ktestify.models.Topic;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;

/**
 * Thin coordinator that wires a {@link KafkaRecordFetcher} (transport) with a {@link RecordMatcher} (assertion) and
 * exposes a single {@link #call()} entry point.
 *
 * <p>This class contains <strong>no Kafka mechanics</strong> and <strong>no matching logic</strong>. Those
 * responsibilities belong exclusively to their respective collaborators. The only decision made here is: fetch → match
 * → return result.
 *
 * <p>Concrete subclasses only need to supply a {@link RecordMatcher} — typically resolved by
 * {@code RecordMatcherFactory} — and call the {@link #AbstractKafkaConsumer(ConsumerContext, RecordMatcher)}
 * constructor.
 *
 * @param <K> the Kafka record key type
 * @param <V> the Kafka record value type
 * @since 0.3.0
 */
@Slf4j
public abstract class AbstractKafkaConsumer<K, V> extends AbstractConsumer {

    protected final ConsumerContext<K, V> context;
    protected final RecordMatcher<V> matcher;

    /**
     * Primary constructor.
     *
     * @param context the consumer context (topic, Kafka consumer, timeouts, key-filter, …)
     * @param matcher the assertion strategy to apply after fetching records
     */
    protected AbstractKafkaConsumer(ConsumerContext<K, V> context, RecordMatcher<V> matcher) {
        super(context.getProperties());
        this.context = context;
        this.matcher = matcher;
        log.debug(
                "AbstractKafkaConsumer created for topic '{}' with matcher '{}'",
                context.getTopic().getNamespacedTopic(),
                matcher.getClass().getSimpleName());
    }

    /**
     * Legacy convenience constructor for callers that previously passed topic + consumer + properties.
     *
     * @param topic the topic to consume from
     * @param consumer the Kafka consumer instance
     * @param properties the consumer properties map
     * @param matcher the assertion strategy
     * @deprecated Build a {@link ConsumerContext} and use {@link #AbstractKafkaConsumer(ConsumerContext,
     *     RecordMatcher)} instead.
     */
    @Deprecated
    protected AbstractKafkaConsumer(
            Topic topic, Consumer<K, V> consumer, Map<String, String> properties, RecordMatcher<V> matcher) {
        this(
                ConsumerContext.<K, V>builder()
                        .topic(topic)
                        .consumer(consumer)
                        .properties(properties)
                        .build(),
                matcher);
    }

    /**
     * Fetches records from Kafka, then asserts them with the configured matcher.
     *
     * <p>Lifecycle:
     *
     * <ol>
     *   <li>Create a {@link KafkaRecordFetcher} for this invocation.
     *   <li>Call {@link KafkaRecordFetcher#fetch()} — blocks until records arrive or timeout.
     *   <li>Pass the fetched records to {@link RecordMatcher#match(List, MatchContext)}.
     *   <li>Close the fetcher unconditionally in a {@code finally} block.
     * </ol>
     *
     * @return {@code true} if the matcher passed, {@code false} otherwise
     * @throws ConsumerException if the fetch or match step throws an unrecoverable error
     */
    @Override
    public Boolean call() throws ConsumerException {
        try (KafkaRecordFetcher<K, V> fetcher = new KafkaRecordFetcher<>(context)) {
            List<ConsumedRecord<V>> records = fetcher.fetch();
            MatchContext matchContext = buildMatchContext();
            MatchResult result = matcher.match(records, matchContext);

            log.debug(
                    "Match result for topic '{}': passed={}, diff={}",
                    context.getTopic().getNamespacedTopic(),
                    result.isPassed(),
                    result.getDiff());

            return result.isPassed();

        } catch (FetchException e) {
            throw new ConsumerException(e.getMessage());
        }
    }

    /**
     * Builds the {@link MatchContext} that is passed to the matcher.
     *
     * <p>The default implementation reads all relevant fields from the {@link ConsumerContext}. Subclasses may override
     * this to supply additional or computed values.
     *
     * @return the match context for this invocation
     */
    protected MatchContext buildMatchContext() {
        return MatchContext.builder()
                .matchMethod(context.getMatchMethod())
                .matchFilePaths(context.getMatchFilePaths())
                .excludedFields(context.getExcludedFields())
                .strictMatching(false)
                .build();
    }
}
