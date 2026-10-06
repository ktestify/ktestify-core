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
 * Thrown by the orchestration layer when a consumer operation fails.
 *
 * <p>Typically wraps a {@link FetchException} from the transport layer. Also thrown for orchestration-level
 * configuration errors such as consuming from an INPUT topic or a null consumer context.
 *
 * <p>This is the only exception that test-framework adapters (Cucumber steps, Robot Framework keywords, …) are expected
 * to catch and surface as a human-readable assertion failure.
 *
 * @since 0.3.0
 * @see FetchException
 */
public class ConsumerException extends RuntimeException {

    /**
     * Constructs a new {@code ConsumerException} with the supplied detail message.
     *
     * @param message a human-readable description of the consumer failure
     */
    public ConsumerException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ConsumerException} with the supplied detail message and cause.
     *
     * @param message a human-readable description of the consumer failure
     * @param cause the underlying exception that caused this failure
     */
    public ConsumerException(String message, Throwable cause) {
        super(message, cause);
    }
}
