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

import java.util.Collection;

/**
 * Thrown when a single logical operation (e.g. a multi-row Cucumber {@code DataTable} driving one producer/consumer
 * call) resolves more than one distinct topic, where exactly one is required.
 *
 * <p>This is a guard-rail exception: a DataTable listing several instructions is only allowed to target a single topic
 * per call. Mixing topics in one DataTable is almost always an authoring mistake — split it into separate step
 * invocations instead.
 *
 * @since 0.4.0
 */
public class TopicMismatchException extends RuntimeException {

    public TopicMismatchException(String message) {
        super(message);
    }

    /**
     * Creates an exception describing the distinct topics found where a single topic was expected.
     *
     * @param distinctTopics the distinct namespaced topic names encountered
     * @return a new TopicMismatchException with a descriptive message
     */
    public static TopicMismatchException forTopics(Collection<String> distinctTopics) {
        return new TopicMismatchException(
                "A DataTable can only reference a single topic per step, but " + distinctTopics.size()
                        + " distinct topics were found: " + distinctTopics
                        + ". Split this into separate step invocations, one per topic.");
    }
}
