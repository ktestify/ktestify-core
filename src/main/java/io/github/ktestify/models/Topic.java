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
package io.github.ktestify.models;

import static java.util.Objects.requireNonNull;

import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.exceptions.ConfigException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class Topic {

    private String topicName;
    private String topicAlias;
    private TopicNamespace topicNamespace;
    private Topic.Type topicType;

    public String getNamespacedTopic() {
        if (topicNamespace != null) {
            return topicNamespace.getNamespace() + "." + topicName;
        }
        return topicName;
    }

    public enum Type {
        INPUT,
        OUTPUT
    }

    public static String getSimpleClassName() {
        return Topic.class.getSimpleName();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class TopicNamespace {

        private String namespace;
        private String namespaceAlias;

        public static String getSimpleClassName() {
            return TopicNamespace.class.getSimpleName();
        }
    }

    public static Topic validateTopic(Topic topic, Type topicType) {
        requireNonNull(topic, "Topic must be provided");
        String topicName = topic.getTopicName();

        if ((topic.getTopicNamespace() == null
                        || StringUtils.isBlank(topic.getTopicNamespace().getNamespace()))
                && KtestifyConfig.getOrLoad().getKafka().getTopicNamespace().isPresent()) {
            topic.setTopicNamespace(Topic.TopicNamespace.builder()
                    .namespace(KtestifyConfig.getOrLoad()
                            .getKafka()
                            .getTopicNamespace()
                            .get())
                    .build());
        }

        if (topicName == null || topicName.isEmpty()) {
            throw new ConfigException("No topic name was specified !");
        }

        if (topic.getTopicType() != topicType) {
            if (topicType == Type.INPUT) {
                throw new ConfigException(
                        "Topic type is " + topicType.toString() + ", Cannot consume from an input topic !");
            }
            throw new ConfigException(
                    "Topic type is " + topicType.toString() + ", Cannot produce to an output topic !");
        }
        return topic;
    }
}
