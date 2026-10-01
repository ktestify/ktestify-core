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
package io.github.ktestify.match;

import io.github.ktestify.exceptions.ConfigException;
import io.github.ktestify.match.impl.*;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.github.ktestify.match.RecordMatcherFactory.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecordMatcherFactory")
class RecordMatcherFactoryTest {

    // =========================================================================
    // forRaw
    // =========================================================================

    @Nested
    @DisplayName("forRaw — known methods")
    class ForRawKnownMethods {

        @Test
        @DisplayName("METHOD_MATCH_FILE → FileRecordMatcher")
        void matchFile() {
            assertInstanceOf(FileRecordMatcher.class, RecordMatcherFactory.forRaw(METHOD_MATCH_FILE));
        }

        @Test
        @DisplayName("METHOD_MATCH_KEY_FILE → FileKeyRecordMatcher")
        void matchKeyFile() {
            assertInstanceOf(FileKeyRecordMatcher.class, RecordMatcherFactory.forRaw(METHOD_MATCH_KEY_FILE));
        }

        @Test
        @DisplayName("METHOD_FIELDS_TO_MATCH → FieldsRecordMatcher")
        void matchFields() {
            assertInstanceOf(FieldsRecordMatcher.class, RecordMatcherFactory.forRaw(METHOD_FIELDS_TO_MATCH));
        }

        @Test
        @DisplayName("METHOD_MATCH_XML → XmlRecordMatcher")
        void matchXml() {
            assertInstanceOf(XmlRecordMatcher.class, RecordMatcherFactory.forRaw(METHOD_MATCH_XML));
        }

        @Test
        @DisplayName("METHOD_MATCH_XPATH → XPathRecordMatcher")
        void matchXPath() {
            assertInstanceOf(XPathRecordMatcher.class, RecordMatcherFactory.forRaw(METHOD_MATCH_XPATH));
        }

        @Test
        @DisplayName("METHOD_RECORD_KEY_MATCH → KeyRecordMatcher")
        void matchKey() {
            assertInstanceOf(KeyRecordMatcher.class, RecordMatcherFactory.forRaw(METHOD_RECORD_KEY_MATCH));
        }

        @Test
        @DisplayName("METHOD_MATCH_ATTRIBUTES → AttributeRecordMatcher")
        void matchAttributes() {
            assertInstanceOf(AttributeRecordMatcher.class, RecordMatcherFactory.forRaw(METHOD_MATCH_ATTRIBUTES));
        }
    }

    @Nested
    @DisplayName("forRaw — null/blank → NoOpRecordMatcher")
    class ForRawNullBlank {

        @ParameterizedTest(name = "forRaw(\"{0}\") → NoOpRecordMatcher")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void returnsNoOp(String method) {
            assertInstanceOf(NoOpRecordMatcher.class, RecordMatcherFactory.forRaw(method));
        }
    }

    @Nested
    @DisplayName("forRaw — unknown method → ConsumerException")
    class ForRawUnknown {

        @Test
        @DisplayName("throws ConsumerException for an unrecognised method name")
        void throwsForUnknown() {
            assertThrows(ConfigException.class, () -> RecordMatcherFactory.forRaw("methodDoesNotExist"));
        }
    }

    // =========================================================================
    // forAvro
    // =========================================================================

    @Nested
    @DisplayName("forAvro — known methods")
    class ForAvroKnownMethods {

        @Test
        @DisplayName("METHOD_MATCH_FILE → AvroFileRecordMatcher")
        void matchFile() {
            assertInstanceOf(AvroFileRecordMatcher.class, RecordMatcherFactory.forAvro(METHOD_MATCH_FILE));
        }

        @Test
        @DisplayName("METHOD_MATCH_KEY_FILE → AvroFileKeyRecordMatcher")
        void matchKeyFile() {
            assertInstanceOf(AvroFileKeyRecordMatcher.class, RecordMatcherFactory.forAvro(METHOD_MATCH_KEY_FILE));
        }

        @Test
        @DisplayName("METHOD_FIELDS_TO_MATCH → AvroFieldsRecordMatcher")
        void matchFields() {
            assertInstanceOf(AvroFieldsRecordMatcher.class, RecordMatcherFactory.forAvro(METHOD_FIELDS_TO_MATCH));
        }

        @Test
        @DisplayName("METHOD_RECORD_KEY_MATCH → AvroKeyRecordMatcher")
        void matchKey() {
            assertInstanceOf(AvroKeyRecordMatcher.class, RecordMatcherFactory.forAvro(METHOD_RECORD_KEY_MATCH));
        }
    }

    @Nested
    @DisplayName("forAvro — null/blank → NoOpRecordMatcher")
    class ForAvroNullBlank {

        @ParameterizedTest(name = "forAvro(\"{0}\") → NoOpRecordMatcher")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void returnsNoOp(String method) {
            RecordMatcher<GenericRecord> matcher = RecordMatcherFactory.forAvro(method);
            assertInstanceOf(NoOpRecordMatcher.class, matcher);
        }
    }

    @Nested
    @DisplayName("forAvro — unknown method → ConsumerException")
    class ForAvroUnknown {

        @Test
        @DisplayName("throws ConsumerException for an unrecognised method name")
        void throwsForUnknown() {
            assertThrows(ConfigException.class, () -> RecordMatcherFactory.forAvro("methodDoesNotExist"));
        }

        @Test
        @DisplayName("throws ConsumerException for XML method (not supported for Avro)")
        void throwsForXml() {
            assertThrows(ConfigException.class, () -> RecordMatcherFactory.forAvro(METHOD_MATCH_XML));
        }

        @Test
        @DisplayName("throws ConsumerException for XPath method (not supported for Avro)")
        void throwsForXPath() {
            assertThrows(ConfigException.class, () -> RecordMatcherFactory.forAvro(METHOD_MATCH_XPATH));
        }

        @Test
        @DisplayName("throws ConsumerException for attribute method (raw transports only)")
        void throwsForAttributes() {
            assertThrows(ConfigException.class, () -> RecordMatcherFactory.forAvro(METHOD_MATCH_ATTRIBUTES));
        }
    }

    // =========================================================================
    // Return type sanity
    // =========================================================================

    @Nested
    @DisplayName("Return type never null")
    class NeverNull {

        @Test
        @DisplayName("forRaw always returns a non-null matcher")
        void forRawNeverNull() {
            assertNotNull(RecordMatcherFactory.forRaw(null));
            assertNotNull(RecordMatcherFactory.forRaw(METHOD_MATCH_FILE));
        }

        @Test
        @DisplayName("forAvro always returns a non-null matcher")
        void forAvroNeverNull() {
            assertNotNull(RecordMatcherFactory.forAvro(null));
            assertNotNull(RecordMatcherFactory.forAvro(METHOD_MATCH_FILE));
        }
    }
}
