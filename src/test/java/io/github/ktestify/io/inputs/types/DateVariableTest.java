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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DateVariableTest {

    private DateVariable dateVariable;

    @BeforeEach
    public void setUp() {
        dateVariable = new DateVariable();
    }

    @Test
    public void testGetName() {
        assertEquals("date", dateVariable.getName(), "Variable name should be 'date'");
    }

    @Test
    public void testProcessWithNoFormat() {
        String result = dateVariable.process("");
        String expected = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        assertEquals(expected, result, "Should use ISO_DATE format by default");
    }

    @Test
    public void testProcessWithNullFormat() {
        String result = dateVariable.process(null);
        String expected = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        assertEquals(expected, result, "Should handle null format");
    }

    @Test
    public void testProcessWithCustomFormat() {
        String format = "yyyy/MM/dd";
        String result = dateVariable.process(format);
        String expected = LocalDate.now().format(DateTimeFormatter.ofPattern(format));
        assertEquals(expected, result, "Should format according to the provided pattern");
    }

    @Test
    public void testProcessDefaultMethod() {
        String result = dateVariable.process();
        String expected = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        assertEquals(expected, result, "Default process method should use ISO_DATE format");
    }

    @Test
    public void testProcessWithInvalidFormat() {
        // This test expects an exception to be thrown for an invalid date format pattern
        assertThrows(
                Exception.class,
                () -> dateVariable.process("invalid-format"),
                "Should throw an exception for invalid format");
    }
}
