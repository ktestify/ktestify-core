/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.ktestify.utils;

import lombok.experimental.UtilityClass;

/**
 * Utility for extracting fixed-position fields from flat-file / positional record formats.
 *
 * @since 0.3.0
 */
@UtilityClass
public final class FieldMatcherUtils {

    /**
     * Returns the line at the given 0-based index from a multi-line string.
     *
     * @param content the full record content
     * @param line 0-based line index
     * @return the content of the requested line
     */
    public static String getLine(String content, int line) {
        return content.split("\n")[line];
    }

    /**
     * Extracts a substring from {@code content} in the range [{@code from}, {@code to}).
     *
     * @param content the string to extract from
     * @param from inclusive start index
     * @param to exclusive end index
     * @return the extracted substring
     * @throws IllegalArgumentException if the content is null or the range is invalid
     */
    public static String getChars(String content, int from, int to) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null.");
        }
        if (from < 0 || to > content.length() || from > to) {
            throw new IllegalArgumentException(
                    "Invalid range: from " + from + " to " + to + " (content length: " + content.length() + ")");
        }
        return content.substring(from, to);
    }

    /**
     * Extracts a fixed-position field from a specific line of a multi-line record.
     *
     * @param content the full record content
     * @param line 0-based line index
     * @param from inclusive start character index within the line
     * @param to exclusive end character index within the line
     * @return the extracted field value
     */
    public static String getFieldsToMatch(String content, int line, int from, int to) {
        return getChars(getLine(content, line), from, to);
    }
}
