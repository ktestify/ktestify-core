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
package io.github.ktestify.utils;

import io.github.ktestify.exceptions.TopicMismatchException;
import io.github.ktestify.models.Topic;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.experimental.UtilityClass;

/**
 * Utilities for validating and comparing {@link Topic} instances.
 *
 * <p>Transport-agnostic on purpose: any adapter (Kafka today, IBM MQ or others in the future) that drives one physical
 * operation from a multi-row {@code DataTable} can reuse {@link #assertSingleTopic(List)} to enforce that all rows
 * target the same topic.
 *
 * @since 0.4.0
 */
@UtilityClass
public final class TopicUtils {

    /**
     * Asserts that every {@link Topic} in the given list resolves to the same physical topic (namespaced topic name +
     * type). Returns that single topic if so.
     *
     * @param topics the topics resolved from each row of a DataTable, in row order
     * @return the single common topic
     * @throws IllegalArgumentException if {@code topics} is null or empty
     * @throws TopicMismatchException if more than one distinct topic is found
     */
    public static Topic assertSingleTopic(List<Topic> topics) {
        if (topics == null || topics.isEmpty()) {
            throw new IllegalArgumentException("At least one topic must be provided.");
        }

        Set<String> distinct = new LinkedHashSet<>();
        for (Topic topic : topics) {
            distinct.add(identity(topic));
        }

        if (distinct.size() > 1) {
            throw TopicMismatchException.forTopics(distinct);
        }

        return topics.getFirst();
    }

    /**
     * Returns a stable identity string for a topic, combining its namespaced name and type. Two {@link Topic} instances
     * (e.g. resolved via alias vs. via name) that point to the same physical topic will produce the same identity even
     * if they are not the same object reference.
     */
    private static String identity(Topic topic) {
        String namespacedTopic = topic != null ? topic.getNamespacedTopic() : null;
        Topic.Type type = topic != null ? topic.getTopicType() : null;
        return namespacedTopic + "#" + type;
    }
}
