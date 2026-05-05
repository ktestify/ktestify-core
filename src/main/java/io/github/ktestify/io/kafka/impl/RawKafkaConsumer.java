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
package io.github.ktestify.io.kafka.impl;

import io.github.ktestify.io.kafka.AbstractKafkaConsumer;
import io.github.ktestify.io.kafka.ConsumerContext;
import io.github.ktestify.match.RecordMatcher;
import io.github.ktestify.match.RecordMatcherFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka consumer for plain String (raw) records.
 *
 * <p>This is the standard entry point for consuming {@code String}-valued Kafka topics. It delegates all Kafka
 * mechanics to {@link io.github.ktestify.io.kafka.KafkaRecordFetcher} and all assertion logic to the supplied
 * {@link RecordMatcher}.
 *
 * <p>Typical usage — consume and assert against a file:
 *
 * <pre>
 * Consumer&lt;String, String&gt; kafkaConsumer = KafkaClientFactory.createRawConsumer();
 *
 * ConsumerContext&lt;String, String&gt; ctx = ConsumerContext.&lt;String, String&gt;builder()
 *     .topic(myTopic)
 *     .consumer(kafkaConsumer)
 *     .matchMethod(ConfigConstants.METHOD_MATCH_FILE)
 *     .matchFilePath("src/test/resources/expected/order.json")
 *     .build();
 *
 * boolean passed = new RawKafkaConsumer(ctx).call();
 * </pre>
 *
 * <p>Consume-only (no assertion):
 *
 * <pre>
 * boolean exists = new RawKafkaConsumer(ctx, new NoOpRecordMatcher&lt;&gt;()).call();
 * </pre>
 *
 * @since 0.3.0
 */
@Slf4j
public class RawKafkaConsumer extends AbstractKafkaConsumer<String, String> {

    /**
     * Creates a consumer whose matcher is resolved from the {@link ConsumerContext}.
     *
     * <p>If no {@code matchMethod} is set in the context, a {@link NoOpRecordMatcher} is used and the consumer simply
     * verifies that a record exists.
     *
     * @param context the consumer context
     */
    public RawKafkaConsumer(ConsumerContext<String, String> context) {
        this(context, resolveDefaultMatcher(context));
    }

    /**
     * Creates a consumer with an explicitly supplied matcher. Use this constructor when you want to inject a custom or
     * test-specific matcher.
     *
     * @param context the consumer context
     * @param matcher the assertion strategy to apply after fetching records
     */
    public RawKafkaConsumer(ConsumerContext<String, String> context, RecordMatcher<String> matcher) {
        super(context, matcher);
        log.debug(
                "RawKafkaConsumer ready for topic '{}' using matcher '{}'",
                context.getTopic().getNamespacedTopic(),
                matcher.getClass().getSimpleName());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves the appropriate {@link RecordMatcher} from the context. Falls back to {@link NoOpRecordMatcher} when no
     * match method is configured.
     */
    private static RecordMatcher<String> resolveDefaultMatcher(ConsumerContext<String, String> context) {
        RecordMatcher<String> matcher = RecordMatcherFactory.forRaw(context.getMatchMethod());
        log.debug(
                "Resolved RecordMatcher '{}' for matchMethod '{}'.",
                matcher.getClass().getSimpleName(),
                context.getMatchMethod());
        return matcher;
    }
}
