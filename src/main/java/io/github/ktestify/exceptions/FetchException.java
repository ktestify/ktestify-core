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
 * Thrown when a {@code RecordFetcher} fails to retrieve records from an IO source (timeout, connectivity issue,
 * authentication failure, etc.).
 *
 * <p>This exception is transport-agnostic — Kafka, IBM MQ, and any future IO adapter all throw {@code FetchException}
 * so higher layers do not need to catch transport-specific exceptions.
 *
 * @since 0.3.0
 */
public class FetchException extends RuntimeException {

    public FetchException(String message) {
        super(message);
    }

    public FetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
