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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FieldMatcherUtilsTest {
    @Test
    void testGetLine() {
        String content = "First line\nSecond line\nThird line";
        Assertions.assertEquals(
                "Second line", FieldMatcherUtils.getLine(content, 1), "Should return the correct line.");
    }

    @Test
    void testGetLineOutOfBounds() {
        String content = "First line\nSecond line";
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> FieldMatcherUtils.getLine(content, 2));
    }

    @Test
    void testGetChars() {
        String content = "Hello, World!";

        Assertions.assertEquals(
                "Hello", FieldMatcherUtils.getChars(content, 0, 5), "Should return the correct substring.");
    }

    @Test
    public void testGetCharsWithInvalidRange() {
        String content = "Hello, World!";

        assertThrows(IllegalArgumentException.class, () -> FieldMatcherUtils.getChars(content, 5, 3));
    }

    @Test
    void testGetCharsWithNullContent() {
        assertThrows(IllegalArgumentException.class, () -> FieldMatcherUtils.getChars(null, 0, 1));
    }

    @Test
    void testGetFieldsToMatch() {
        String content = "Line1\nLine2 is longer\nLine3";

        Assertions.assertEquals(
                "Line2",
                FieldMatcherUtils.getFieldsToMatch(content, 1, 0, 5),
                "Should extract the correct substring from the specified line.");
    }
}
