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
package io.github.ktestify.io.inputs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicVariableFactoryTest {

    @Mock
    private DynamicVariable mockVariable1;

    @Mock
    private DynamicVariable mockVariable2;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Reset the factory for each test
        DynamicVariableFactory.clearRegisteredVariables();

        // Setup mocks
        when(mockVariable1.getName()).thenReturn("var1");
        when(mockVariable2.getName()).thenReturn("var2");
    }

    @AfterEach
    public void tearDown() {
        // Restore the built-ins so other test classes are not affected
        DynamicVariableFactory.resetToDefaults();
    }

    @Test
    public void testLookupIsCaseInsensitive() {
        DynamicVariableFactory.resetToDefaults();

        assertTrue(DynamicVariableFactory.isRegistered("env"));
        assertTrue(DynamicVariableFactory.isRegistered("ENV"));
        assertTrue(DynamicVariableFactory.isRegistered("Env"));
        assertSame(DynamicVariableFactory.getVariable("env"), DynamicVariableFactory.getVariable("ENV"));
    }

    @Test
    public void testGetVariableWithNullNameReturnsNull() {
        assertNull(DynamicVariableFactory.getVariable(null));
        assertFalse(DynamicVariableFactory.isRegistered(null));
    }

    @Test
    public void testResetToDefaultsRestoresBuiltIns() {
        DynamicVariableFactory.resetToDefaults();

        assertEquals(Set.of("date", "timestamp", "random", "env"), DynamicVariableFactory.getRegisteredVariableNames());
    }

    @Test
    public void testRegisterAndGetVariable() {
        DynamicVariableFactory.registerVariable(mockVariable1);

        assertTrue(DynamicVariableFactory.isRegistered("var1"), "Variable should be registered");
        assertSame(mockVariable1, DynamicVariableFactory.getVariable("var1"), "Should return the registered variable");
    }

    @Test
    public void testIsRegisteredWithNonexistentVariable() {
        assertFalse(DynamicVariableFactory.isRegistered("nonexistent"), "Should return false for nonexistent variable");
    }

    @Test
    public void testGetNonexistentVariable() {
        assertNull(DynamicVariableFactory.getVariable("nonexistent"), "Should return null for nonexistent variable");
    }

    @Test
    public void testRegisterMultipleVariables() {
        DynamicVariableFactory.registerVariable(mockVariable1);
        DynamicVariableFactory.registerVariable(mockVariable2);

        assertTrue(DynamicVariableFactory.isRegistered("var1"), "First variable should be registered");
        assertTrue(DynamicVariableFactory.isRegistered("var2"), "Second variable should be registered");
    }

    @Test
    public void testOverrideRegisteredVariable() {
        DynamicVariableFactory.registerVariable(mockVariable1);

        DynamicVariable anotherMockVar = mock(DynamicVariable.class);
        when(anotherMockVar.getName()).thenReturn("var1");

        DynamicVariableFactory.registerVariable(anotherMockVar);

        assertSame(anotherMockVar, DynamicVariableFactory.getVariable("var1"), "Should override the previous variable");
    }

    @Test
    public void testGetRegisteredVariableNames() {
        DynamicVariableFactory.registerVariable(mockVariable1);
        DynamicVariableFactory.registerVariable(mockVariable2);

        Set<String> names = DynamicVariableFactory.getRegisteredVariableNames();

        assertEquals(2, names.size(), "Should have two variable names");
        assertTrue(names.contains("var1"), "Should contain first variable name");
        assertTrue(names.contains("var2"), "Should contain second variable name");
    }

    @Test
    public void testClearRegisteredVariables() {
        DynamicVariableFactory.registerVariable(mockVariable1);
        DynamicVariableFactory.clearRegisteredVariables();

        assertFalse(DynamicVariableFactory.isRegistered("var1"), "Should have no registered variables after clear");
        assertEquals(0, DynamicVariableFactory.getRegisteredVariableNames().size(), "Should have empty set of names");
    }
}
