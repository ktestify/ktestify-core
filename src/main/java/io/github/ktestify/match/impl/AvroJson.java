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

import io.github.ktestify.utils.serdes.AvroDeserializer;
import io.github.ktestify.utils.serdes.AvroUtils;
import org.apache.avro.generic.GenericRecord;

/**
 * Converts an Avro {@link GenericRecord} into the pretty-printed JSON form used by every Avro matcher.
 *
 * <p>Logical types are decoded by {@link AvroDeserializer} first, so dates, timestamps and decimals are compared in
 * their readable form rather than as raw epoch numbers or bytes.
 *
 * @since 1.1.4
 */
final class AvroJson {

    private AvroJson() {}

    /**
     * Returns the JSON representation of {@code value}.
     *
     * @param value the Avro record; must not be {@code null} (callers handle tombstones first)
     * @return a pretty-printed JSON string
     */
    static String of(GenericRecord value) {
        if (value.getSchema() != null) {
            return AvroUtils.getPrettyAvroValue(
                    AvroUtils.convertMapToJsonString(AvroDeserializer.recordDeserializer(value)));
        }
        return AvroUtils.getPrettyAvroValue(value.toString());
    }
}
