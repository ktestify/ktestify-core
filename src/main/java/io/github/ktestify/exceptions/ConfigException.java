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
package io.github.ktestify.exceptions;

/**
 * Exception thrown when configuration errors occur.
 *
 * <p>This includes:
 *
 * <ul>
 *   <li>Missing required configuration values
 *   <li>Invalid configuration format
 *   <li>Configuration file not found
 *   <li>Type conversion errors
 * </ul>
 *
 * @since 0.2.51
 */
public class ConfigException extends RuntimeException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception for a missing required configuration value.
     *
     * @param path the configuration path that is missing
     * @return a new ConfigException
     */
    public static ConfigException missingValue(String path) {
        return new ConfigException("Missing required configuration value: " + path);
    }

    /**
     * Creates an exception for an invalid configuration value.
     *
     * @param path the configuration path
     * @param value the invalid value
     * @param expected description of expected value
     * @return a new ConfigException
     */
    public static ConfigException invalidValue(String path, Object value, String expected) {
        return new ConfigException(
                String.format("Invalid configuration value at '%s': got '%s', expected %s", path, value, expected));
    }

    /**
     * Creates an exception for configuration file not found.
     *
     * @param filePath the path to the configuration file
     * @return a new ConfigException
     */
    public static ConfigException fileNotFound(String filePath) {
        return new ConfigException("Configuration file not found: " + filePath);
    }
}
