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
import org.apache.avro.generic.GenericRecord;

/**
 * Kafka consumer for Avro ({@link GenericRecord}) records.
 *
 * <p>Delegates all Kafka mechanics to {@link io.github.ktestify.io.kafka.KafkaRecordFetcher} and all assertion logic to
 * the {@link RecordMatcher} resolved by {@link RecordMatcherFactory#forAvro(String)}.
 *
 * <p>Typical usage — consume and match against an expected JSON file:
 *
 * <pre>
 * Consumer&lt;String, GenericRecord&gt; kafkaConsumer = KafkaClientFactory.createAvroConsumer();
 *
 * ConsumerContext&lt;String, GenericRecord&gt; ctx = ConsumerContext.&lt;String, GenericRecord&gt;builder()
 *     .topic(myTopic)
 *     .consumer(kafkaConsumer)
 *     .matchMethod(ConfigConstants.METHOD_MATCH_FILE)
 *     .matchFilePath("src/test/resources/expected/order.json")
 *     .build();
 *
 * boolean passed = new AvroKafkaConsumer(ctx).call();
 * </pre>
 *
 * <p>Consume-only (no assertion):
 *
 * <pre>
 * boolean exists = new AvroKafkaConsumer(ctx, new NoOpRecordMatcher&lt;&gt;()).call();
 * </pre>
 *
 * @since 0.3.0
 */
@Slf4j
public class AvroKafkaConsumer extends AbstractKafkaConsumer<String, GenericRecord> {

    /**
     * Creates a consumer whose matcher is resolved from the {@link ConsumerContext}.
     *
     * <p>If no {@code matchMethod} is set in the context, a {@link io.github.ktestify.match.impl.NoOpRecordMatcher} is
     * used and the consumer simply verifies that a record arrives.
     *
     * @param context the consumer context
     */
    public AvroKafkaConsumer(ConsumerContext<String, GenericRecord> context) {
        this(context, resolveDefaultMatcher(context));
    }

    /**
     * Creates a consumer with an explicitly supplied matcher. Use this constructor to inject a custom or test-specific
     * matcher.
     *
     * @param context the consumer context
     * @param matcher the assertion strategy to apply after fetching records
     */
    public AvroKafkaConsumer(ConsumerContext<String, GenericRecord> context, RecordMatcher<GenericRecord> matcher) {
        super(context, matcher);
        log.debug(
                "AvroKafkaConsumer ready for topic '{}' using matcher '{}'",
                context.getTopic().getNamespacedTopic(),
                matcher.getClass().getSimpleName());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static RecordMatcher<GenericRecord> resolveDefaultMatcher(ConsumerContext<String, GenericRecord> context) {
        RecordMatcher<GenericRecord> matcher = RecordMatcherFactory.forAvro(context.getMatchMethod());
        log.debug(
                "Resolved RecordMatcher '{}' for matchMethod '{}'.",
                matcher.getClass().getSimpleName(),
                context.getMatchMethod());
        return matcher;
    }
}
