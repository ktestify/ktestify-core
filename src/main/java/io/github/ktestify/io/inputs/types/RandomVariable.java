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
package io.github.ktestify.io.inputs.types;

import io.github.ktestify.io.inputs.DynamicVariable;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;

public class RandomVariable implements DynamicVariable {

    private static final String UUID_TYPE = "uuid";
    private static final String STRING_TYPE = "str";
    private static final String NUMBER_TYPE = "num";
    private static final int DEFAULT_LENGTH = 8;

    @Override
    public String getName() {
        return "random";
    }

    @Override
    public String process(String format) {
        if (format == null || format.isEmpty()) {
            // Default to UUID if no format is specified
            return UUID.randomUUID().toString();
        }

        String[] parts = format.split(":");
        String type = parts[0].toLowerCase();

        // Default length if not specified
        int length = (parts.length > 1) ? parseInt(parts[1]) : DEFAULT_LENGTH;

        switch (type) {
            case UUID_TYPE:
                return generateUUID();
            case STRING_TYPE:
                return generateRandomString(length);
            case NUMBER_TYPE:
                return generateRandomNumber(length);
            default:
                // If type is unrecognized, assume it's a length for a string
                try {
                    int specificLength = Integer.parseInt(type);
                    return generateRandomString(specificLength);
                } catch (NumberFormatException e) {
                    // If parsing fails, return UUID as fallback
                    return UUID.randomUUID().toString();
                }
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 8;
        }
    }

    private static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private String generateRandomNumber(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        // Ensure first digit isn't 0 for a natural-looking number
        if (length > 0) {
            sb.append(1 + random.nextInt(9));
            length--;
        }

        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private static String generateRandomString(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }
}
