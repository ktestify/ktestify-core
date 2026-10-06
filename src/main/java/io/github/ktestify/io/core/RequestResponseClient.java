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
 * Transport-agnostic contract for synchronous, caller-initiated request/response transports (HTTP, gRPC, SOAP, …).
 *
 * <p>Sibling contract to {@link RecordFetcher}. Where {@code RecordFetcher} models "background stream, block until a
 * record appears" (Kafka, Azure Blob polling), {@code RequestResponseClient} models "send a request right now and get
 * an answer immediately".
 *
 * <p>Both contracts return {@link ConsumedRecord}, the common currency shared with every {@code RecordMatcher}, so the
 * entire assertion layer is reused unchanged regardless of which contract a transport implements.
 *
 * <p>Implementations exist per transport:
 *
 * <ul>
 *   <li>{@code HttpRequestResponseClient}: HTTP / HTTPS (ktestify-plugin-http)
 *   <li>{@code GrpcRequestResponseClient}: gRPC (future)
 * </ul>
 *
 * @param <Req> the request type specific to the transport (e.g. an HTTP request spec)
 * @param <V> the type of the resulting record value (e.g. {@code String} for an HTTP body)
 * @since 1.1.1
 */
public interface RequestResponseClient<Req, V> extends AutoCloseable {

    /**
     * Sends {@code request} and returns the result wrapped as a (typically single-element) list of
     * {@link ConsumedRecord}.
     *
     * <p>A list is used, not a single object, purely to keep perfect symmetry with {@link RecordFetcher#fetch()} so
     * both contracts feed the same {@code RecordMatcher} signature unchanged.
     *
     * @param request the request to send
     * @return a non-null, non-empty list of consumed records (normally exactly one)
     * @throws FetchException if the call fails (connection error, timeout, non-recoverable transport error)
     */
    List<ConsumedRecord<V>> execute(Req request) throws FetchException;

    /**
     * Releases all resources held by this client (connection pools, threads, etc.). Idempotent, calling {@code close()}
     * more than once must be safe.
     */
    @Override
    void close();
}
