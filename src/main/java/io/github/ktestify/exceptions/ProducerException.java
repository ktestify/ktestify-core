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
package io.github.ktestify.exceptions;

/**
 * Thrown when a Kafka producer fails to send a record — e.g. broker unreachable, serialization failure,
 * interrupted during send, or schema/payload resolution error.
 *
 * <p>This is a {@link RuntimeException} so callers are not forced to declare it in their {@code throws} clause.
 *
 * @since 0.3.0
 */
public class ProducerException extends RuntimeException {

    /**
     * Constructs a new {@code ProducerException} with the supplied detail message.
     *
     * @param message a human-readable description of the producer failure
     */
    public ProducerException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ProducerException} with the supplied detail message and cause.
     *
     * @param message a human-readable description of the producer failure
     * @param cause the underlying exception that caused this failure
     */
    public ProducerException(String message, Throwable cause) {
        super(message, cause);
    }
}
