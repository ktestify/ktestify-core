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
package io.github.ktestify.match.impl;

import io.github.ktestify.match.MatchContext;
import io.github.ktestify.models.ConsumedRecord;
import io.github.ktestify.utils.FileUtils;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.apache.avro.generic.GenericRecord;

/**
 * Shared test helpers for {@link RecordMatcher} unit tests. Keeps each test class lean — no boilerplate record
 * construction.
 */
final class MatcherTestSupport {

    private MatcherTestSupport() {}

    /** Wraps a plain String value in a minimal {@link ConsumedRecord}. */
    static List<ConsumedRecord<String>> rawRecord(String value) {
        return rawRecord("test-key", value);
    }

    /** Wraps a String value + explicit key in a minimal {@link ConsumedRecord}. */
    static List<ConsumedRecord<String>> rawRecord(String key, String value) {
        return List.of(new ConsumedRecord<>("test-topic", 0, 0L, key, value, Instant.now(), Collections.emptyMap()));
    }

    /** Wraps an Avro {@link GenericRecord} in a minimal {@link ConsumedRecord}. */
    static List<ConsumedRecord<GenericRecord>> avroRecord(String key, GenericRecord value) {
        return List.of(new ConsumedRecord<>("test-topic", 0, 0L, key, value, Instant.now(), Collections.emptyMap()));
    }

    /**
     * Returns the absolute path of a classpath resource inside {@code src/test/resources/match/}. Throws
     * {@link IllegalStateException} if the resource is not found — catches typos early.
     */
    static String resourcePath(String filename) {
        String path = "match/" + filename;
        URL url = MatcherTestSupport.class.getClassLoader().getResource(path);
        if (url == null) {
            throw new IllegalStateException("Test resource not found on classpath: " + path);
        }
        return url.getPath();
    }

    /**
     * Reads the exact content of a test resource file the same way {@code FileUtils} does. Use this to build record
     * values that will exactly match what a file-based matcher reads.
     */
    static String fileContent(String filename) {
        return FileUtils.getFileContent(FileUtils.getFile(resourcePath(filename)));
    }

    /** Builds a minimal {@link MatchContext} with only a file path set. */
    static MatchContext ctxWithFile(String filename) {
        return MatchContext.builder().matchFilePath(resourcePath(filename)).build();
    }

    /** Builds a {@link MatchContext} with both a file path and excluded fields. */
    static MatchContext ctxWithFileAndExclusions(String filename, List<String> excluded) {
        return MatchContext.builder()
                .matchFilePath(resourcePath(filename))
                .excludedFields(excluded)
                .build();
    }

    /** Builds a {@link MatchContext} with a file path and a match key. */
    static MatchContext ctxWithFileAndKey(String filename, String matchKey) {
        return MatchContext.builder()
                .matchFilePath(resourcePath(filename))
                .matchKey(matchKey)
                .build();
    }

    /** Builds a {@link MatchContext} with only a match key set. */
    static MatchContext ctxWithKey(String matchKey) {
        return MatchContext.builder().matchKey(matchKey).build();
    }

    /** Builds a {@link MatchContext} with a match key and an inline expected value. */
    static MatchContext ctxWithKeyAndValue(String matchKey, String matchValue) {
        return MatchContext.builder().matchKey(matchKey).matchValue(matchValue).build();
    }
}
