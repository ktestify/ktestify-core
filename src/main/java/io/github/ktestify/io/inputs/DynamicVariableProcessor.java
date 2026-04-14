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
package io.github.ktestify.io.inputs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicVariableProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicVariableProcessor.class);

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)(?::([^}]*?))?\\}\\}");
    private static final int GROUP_NAME = 1;
    private static final int GROUP_FORMAT = 2;

    public String process(String input) {
        if (input == null) {
            LOGGER.trace("Input string is null, returning null.");
            return null;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        // Check if any matches exist
        if (!matcher.find()) {
            LOGGER.debug("Did not find any dynamic variables will return original string.");
            // No dynamic variables found, return the original string
            return input;
        }

        // Reset the matcher to start from the beginning
        matcher.reset();

        while (matcher.find()) {
            String variableName = matcher.group(GROUP_NAME);
            String format = matcher.group(GROUP_FORMAT) != null ? matcher.group(GROUP_FORMAT) : "";

            if (DynamicVariableFactory.isRegistered(variableName)) {
                LOGGER.info("Processing dynamic variable: {} with format: {}", variableName, format);
                DynamicVariable variable = DynamicVariableFactory.getVariable(variableName);
                String replacement = variable.process(format);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                LOGGER.warn("Dynamic variable {} is not registered, keeping it unchanged.", variableName);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        // Only call appendTail once
        matcher.appendTail(sb);
        LOGGER.debug("Dynamic variables processed successfully.");
        return sb.toString();
    }

    public static boolean doesContainDynamicVariable(String input) {
        if (input == null) {
            LOGGER.trace("Input string is null, returning false.");
            return false;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        boolean containsDynamicVariable = matcher.find();
        LOGGER.trace("Input string contains dynamic variable: {}", containsDynamicVariable);
        return containsDynamicVariable;
    }
}
