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

/**
 * Transport-agnostic contract for fetching records from any IO source.
 *
 * <p>Implementations exist per transport:
 *
 * <ul>
 *   <li>{@code KafkaRecordFetcher} — Apache Kafka
 *   <li>{@code IbmMqRecordFetcher} — IBM MQ (future)
 * </ul>
 *
 * <p>The return type {@link ConsumedRecord} is the common currency shared between the transport layer and the assertion
 * layer ({@code RecordMatcher}). Neither side knows about the other's implementation details.
 *
 * @param <V> the type of the record value
 * @since 0.3.0
 */
public interface RecordFetcher<V> extends AutoCloseable {

    /**
     * Blocks until at least one record that passes the configured filters is available, or the configured read-timeout
     * expires.
     *
     * @return a non-empty, unmodifiable list of consumed records
     * @throws FetchException if the source cannot be reached, the timeout expires without a matching record, or any
     *     other transport-level error occurs
     */
    List<ConsumedRecord<V>> fetch() throws FetchException;

    /**
     * Releases all resources held by this fetcher (connections, threads, etc.). Idempotent — calling {@code close()}
     * more than once must be safe.
     */
    @Override
    void close();
}
