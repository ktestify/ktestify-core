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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ktestify.exceptions.TopicMismatchException;
import io.github.ktestify.models.Topic;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TopicUtils")
class TopicUtilsTest {

    private Topic topic(String name, String alias, Topic.Type type) {
        return Topic.builder().topicName(name).topicAlias(alias).topicType(type).build();
    }

    @Test
    @DisplayName("returns the common topic when all rows resolve to the same topic")
    void returnsCommonTopicWhenAllSame() {
        Topic byName = topic("orders", null, Topic.Type.INPUT);
        Topic byAlias = topic("orders", "orders-alias", Topic.Type.INPUT);

        Topic result = TopicUtils.assertSingleTopic(List.of(byName, byAlias));

        assertEquals("orders", result.getTopicName());
    }

    @Test
    @DisplayName("throws TopicMismatchException when rows resolve to different topics")
    void throwsWhenTopicsDiffer() {
        Topic ordersTopic = topic("orders", null, Topic.Type.INPUT);
        Topic paymentsTopic = topic("payments", null, Topic.Type.INPUT);

        assertThrows(
                TopicMismatchException.class, () -> TopicUtils.assertSingleTopic(List.of(ordersTopic, paymentsTopic)));
    }

    @Test
    @DisplayName("throws TopicMismatchException when the same topic name has different types")
    void throwsWhenTopicTypesDiffer() {
        Topic input = topic("orders", null, Topic.Type.INPUT);
        Topic output = topic("orders", null, Topic.Type.OUTPUT);

        assertThrows(TopicMismatchException.class, () -> TopicUtils.assertSingleTopic(List.of(input, output)));
    }

    @Test
    @DisplayName("throws IllegalArgumentException when the list is null or empty")
    void throwsOnNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> TopicUtils.assertSingleTopic(null));
        assertThrows(IllegalArgumentException.class, () -> TopicUtils.assertSingleTopic(List.of()));
    }
}
