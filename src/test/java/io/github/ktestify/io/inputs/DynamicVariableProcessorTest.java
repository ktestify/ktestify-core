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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

public class DynamicVariableProcessorTest {

    private DynamicVariableProcessor processor;

    @Mock
    private DynamicVariable mockDateVariable;

    private MockedStatic<DynamicVariableFactory> mockedFactory;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new DynamicVariableProcessor();

        // Setup mock variable
        when(mockDateVariable.getName()).thenReturn("testvar");
        when(mockDateVariable.process(anyString())).thenAnswer(i -> "processed:" + i.getArgument(0));
        when(mockDateVariable.process()).thenReturn("processed:default");

        // Mock static methods of DynamicVariableFactory
        mockedFactory = mockStatic(DynamicVariableFactory.class);
        mockedFactory.when(() -> DynamicVariableFactory.isRegistered("testvar")).thenReturn(true);
        mockedFactory.when(() -> DynamicVariableFactory.getVariable("testvar")).thenReturn(mockDateVariable);
        mockedFactory
                .when(() -> DynamicVariableFactory.isRegistered(argThat(name -> !"testvar".equals(name))))
                .thenReturn(false);
    }

    @AfterEach
    public void tearDown() {
        if (mockedFactory != null) {
            mockedFactory.close();
        }
    }

    @Test
    public void testProcessNull() {
        assertNull(processor.process(null), "Should return null when input is null");
    }

    @Test
    public void testProcessEmptyString() {
        assertEquals("", processor.process(""), "Should return empty string when input is empty");
    }

    @Test
    public void testProcessNoVariables() {
        String input = "This is a test string with no variables";
        assertEquals(input, processor.process(input), "Should return the original input when no variables are present");
    }

    @Test
    public void testProcessWithOneVariable() {
        String input = "This is a {{testvar}} variable";
        String expected = "This is a processed: variable";

        assertEquals(expected, processor.process(input), "Should replace the variable with its processed value");
        verify(mockDateVariable).process("");
    }

    @Test
    public void testProcessWithVariableAndFormat() {
        String input = "Format: {{testvar:yyyy-MM-dd}}";
        String expected = "Format: processed:yyyy-MM-dd";

        assertEquals(expected, processor.process(input), "Should replace the variable with its formatted value");
        verify(mockDateVariable).process("yyyy-MM-dd");
    }

    @Test
    public void testProcessWithMultipleVariables() {
        String input = "First: {{testvar}} Second: {{testvar:format}}";
        String expected = "First: processed: Second: processed:format";

        assertEquals(expected, processor.process(input), "Should replace all variables correctly");
        verify(mockDateVariable, times(1)).process("");
        verify(mockDateVariable, times(1)).process("format");
    }

    @Test
    public void testProcessWithUnregisteredVariable() {
        String input = "This contains an {{unknown}} variable";

        assertEquals(input, processor.process(input), "Should keep unregistered variables unchanged");
    }

    @Test
    public void testProcessWithMixOfRegisteredAndUnregistered() {
        String input = "{{testvar}} and {{unknown}} mixed";
        String expected = "processed: and {{unknown}} mixed";

        assertEquals(expected, processor.process(input), "Should only replace registered variables");
    }

    @Test
    public void testProcessWithSpecialCharacters() {
        String input = "Special: {{testvar:$^*()_+}} characters";
        String expected = "Special: processed:$^*()_+ characters";

        assertEquals(expected, processor.process(input), "Should handle special characters in format string");
    }

    @Test
    public void testProcessWithMultipleOccurrencesOfSameVariable() {
        String input = "{{testvar}} {{testvar}} {{testvar}}";
        String expected = "processed: processed: processed:";

        assertEquals(expected, processor.process(input), "Should replace all occurrences of the same variable");
        verify(mockDateVariable, times(3)).process("");
    }
}
