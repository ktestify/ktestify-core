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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StringDiffUtils {

    private StringDiffUtils() {}

    private static final Logger LOGGER = LoggerFactory.getLogger(StringDiffUtils.class);
    private static final String RESET_COLOR = "\033[0m";
    private static final String RED_COLOR = "\033[41m"; // Red background
    private static final String GREEN_COLOR = "\033[32m"; // Green background
    private static final String DEFAULT_TEXT_COLOR =
            "\033[37m"; // White text color for better visibility with backgrounds

    public enum Type {
        ACTUAL,
        EXPECTED
    }

    public static StringBuilder getPrettyStringDiff(String expectedString, String actualString, Type type) {
        if (expectedString == null || actualString == null) {
            LOGGER.error("Cannot compare null strings");
            return null;
        }
        StringBuilder diff = new StringBuilder();
        int maxLength = Math.max(expectedString.length(), actualString.length());

        for (int i = 0; i < maxLength; i++) {
            char charFromExpected = i < expectedString.length() ? expectedString.charAt(i) : '␣';
            char charFromActual = i < actualString.length() ? actualString.charAt(i) : '␣';

            // Replace spaces with a visible character for clarity
            charFromExpected = charFromExpected == ' ' ? '␣' : charFromExpected;
            charFromActual = charFromActual == ' ' ? '␣' : charFromActual;

            // Use the compareStrings method to append the correct representation
            compareStrings(diff, charFromExpected, charFromActual, type);
        }

        return diff;
    }

    private static void compareStrings(StringBuilder diff, char charFromExpected, char charFromActual, Type type) {
        if (charFromExpected == '\n' || charFromActual == '\n') {
            diff.append(RESET_COLOR).append("¶").append('\n');
        } else if (charFromExpected == charFromActual) {
            diff.append(DEFAULT_TEXT_COLOR).append(charFromExpected).append(RESET_COLOR);
        } else {
            if (type == Type.EXPECTED) {
                diff.append(RED_COLOR).append(charFromExpected).append(RESET_COLOR);
            } else if (type == Type.ACTUAL) {
                if (charFromExpected == '␣') {
                    diff.append(GREEN_COLOR).append('␣').append(RESET_COLOR);
                } else {
                    diff.append(RED_COLOR).append(charFromActual).append(RESET_COLOR);
                }
            }
        }
    }
}
