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

import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.match.impl.AvroFieldsRecordMatcher;
import io.github.ktestify.match.impl.AvroFileKeyRecordMatcher;
import io.github.ktestify.match.impl.AvroFileRecordMatcher;
import io.github.ktestify.match.impl.AvroKeyRecordMatcher;
import io.github.ktestify.match.impl.FieldsRecordMatcher;
import io.github.ktestify.match.impl.FileKeyRecordMatcher;
import io.github.ktestify.match.impl.FileRecordMatcher;
import io.github.ktestify.match.impl.KeyRecordMatcher;
import io.github.ktestify.match.impl.NoOpRecordMatcher;
import io.github.ktestify.match.impl.XPathRecordMatcher;
import io.github.ktestify.match.impl.XmlRecordMatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;

/**
 * Pure static factory that resolves the correct {@link RecordMatcher} from a {@code matchMethod} string constant (see
 * {@code ConfigConstants}).
 *
 * <p>Two typed factory methods are provided so the compiler can enforce type safety:
 *
 * <ul>
 *   <li>{@link #forRaw(String)} — for {@code String}-valued topics
 *   <li>{@link #forAvro(String)} — for {@code GenericRecord}-valued (Avro) topics
 * </ul>
 *
 * <p>When {@code matchMethod} is {@code null} or blank a {@link NoOpRecordMatcher} is returned, making "consume-only"
 * scenarios a first-class use-case.
 *
 * @since 0.3.0
 */
@Slf4j
public final class RecordMatcherFactory {

    public static final String METHOD_MATCH_FILE = "methodMatchFile";
    public static final String METHOD_MATCH_KEY_FILE = "methodMatchKeyValue";
    public static final String METHOD_MATCH_XML = "methodMatchXML";
    public static final String METHOD_MATCH_XPATH = "methodMatchXPath";
    public static final String METHOD_FIELDS_TO_MATCH = "methodFieldsToMatch";
    public static final String METHOD_RECORD_KEY_MATCH = "methodRecordKeyMatch";

    private RecordMatcherFactory() {}

    // =========================================================================
    // Raw (String) matchers
    // =========================================================================

    /**
     * Resolves a {@link RecordMatcher} for plain {@code String} record values.
     *
     * @param matchMethod one of the {@code METHOD_*} constants in {@code ConfigConstants}, or {@code null} / blank for
     *     no-op
     * @return the appropriate matcher; never {@code null}
     * @throws ConsumerException if the method name is non-blank but unrecognised
     */
    public static RecordMatcher<String> forRaw(String matchMethod) {
        if (matchMethod == null || matchMethod.isBlank()) {
            log.debug("No matchMethod specified — using NoOpRecordMatcher.");
            return new NoOpRecordMatcher<>();
        }
        log.debug("Resolving raw RecordMatcher for method '{}'.", matchMethod);
        return switch (matchMethod) {
            case METHOD_MATCH_FILE -> new FileRecordMatcher();
            case METHOD_MATCH_KEY_FILE -> new FileKeyRecordMatcher();
            case METHOD_FIELDS_TO_MATCH -> new FieldsRecordMatcher();
            case METHOD_MATCH_XML -> new XmlRecordMatcher();
            case METHOD_MATCH_XPATH -> new XPathRecordMatcher();
            case METHOD_RECORD_KEY_MATCH -> new KeyRecordMatcher();
            default ->
                throw new ConsumerException("Unknown raw matchMethod '" + matchMethod + "'. "
                        + "Valid values: methodMatchFile, methodMatchKeyValue, methodFieldsToMatch, "
                        + "methodMatchXML, methodMatchXPath, methodRecordKeyMatch.");
        };
    }

    // =========================================================================
    // Avro (GenericRecord) matchers
    // =========================================================================

    /**
     * Resolves a {@link RecordMatcher} for Avro {@link GenericRecord} record values.
     *
     * @param matchMethod one of the {@code METHOD_*} constants in {@code ConfigConstants}, or {@code null} / blank for
     *     no-op
     * @return the appropriate matcher; never {@code null}
     * @throws ConsumerException if the method name is non-blank but unrecognised
     */
    public static RecordMatcher<GenericRecord> forAvro(String matchMethod) {
        if (matchMethod == null || matchMethod.isBlank()) {
            log.debug("No matchMethod specified — using NoOpRecordMatcher.");
            return new NoOpRecordMatcher<>();
        }
        log.debug("Resolving Avro RecordMatcher for method '{}'.", matchMethod);
        return switch (matchMethod) {
            case METHOD_MATCH_FILE -> new AvroFileRecordMatcher();
            case METHOD_MATCH_KEY_FILE -> new AvroFileKeyRecordMatcher();
            case METHOD_FIELDS_TO_MATCH -> new AvroFieldsRecordMatcher();
            case METHOD_RECORD_KEY_MATCH -> new AvroKeyRecordMatcher();
            default ->
                throw new ConsumerException("Unknown Avro matchMethod '" + matchMethod + "'. "
                        + "Valid values: methodMatchFile, methodMatchKeyValue, "
                        + "methodFieldsToMatch, methodRecordKeyMatch.");
        };
    }
}
