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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RandomVariableTest {

    private RandomVariable randomVariable;

    @BeforeEach
    public void setUp() {
        randomVariable = new RandomVariable();
    }

    @Test
    public void testGetName() {
        assertEquals("random", randomVariable.getName(), "Variable name should be 'random'");
    }

    @Test
    public void testProcessWithNoFormat() {
        String result = randomVariable.process("");

        // UUID should match UUID pattern
        assertTrue(
                result.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "Default should be a valid UUID");
    }

    @Test
    public void testProcessWithUUIDFormat() {
        String result = randomVariable.process("uuid");

        // UUID should match UUID pattern
        assertTrue(
                result.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "UUID format should produce a valid UUID");
    }

    @Test
    public void testProcessWithStringFormat() {
        String result = randomVariable.process("str:10");

        assertEquals(10, result.length(), "String should have the specified length");
        assertTrue(result.matches("[a-zA-Z0-9]{10}"), "Should be alphanumeric");
    }

    @Test
    public void testProcessWithNumberFormat() {
        String result = randomVariable.process("num:8");

        assertEquals(8, result.length(), "Number should have the specified length");
        assertTrue(result.matches("[0-9]{8}"), "Should be numeric only");
    }

    @Test
    public void testProcessWithLengthOnly() {
        String result = randomVariable.process("12");

        assertEquals(12, result.length(), "Should interpret a number as length for random string");
        assertTrue(result.matches("[a-zA-Z0-9]{12}"), "Should be alphanumeric");
    }

    @Test
    public void testProcessDefaultMethod() {
        String result = randomVariable.process();

        // UUID should match UUID pattern
        assertTrue(
                result.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "Default method should produce a valid UUID");
    }

    @Test
    public void testProcessWithInvalidFormat() {
        // Should not throw an exception but fall back to UUID
        String result = randomVariable.process("invalid:format");
        assertTrue(
                result.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "Should handle invalid format gracefully");
    }
}
