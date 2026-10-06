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
package io.github.ktestify.io.inputs.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class TimestampVariableTest {

    private TimestampVariable timestampVariable;
    private final LocalDateTime FIXED_DATETIME = LocalDateTime.of(2025, 5, 14, 10, 15, 30);

    @BeforeEach
    public void setUp() {
        timestampVariable = new TimestampVariable();
    }

    @Test
    public void testGetName() {
        assertEquals("timestamp", timestampVariable.getName(), "Variable name should be 'timestamp'");
    }

    @Test
    public void testProcessWithCustomFormat() {
        try (MockedStatic<LocalDateTime> mockedDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedDateTime.when(LocalDateTime::now).thenReturn(FIXED_DATETIME);

            String format = "yyyy-MM-dd HH:mm:ss";
            String result = timestampVariable.process(format);
            String expected = FIXED_DATETIME.format(DateTimeFormatter.ofPattern(format));

            assertEquals(expected, result, "Should format according to the provided pattern");
            assertEquals("2025-05-14 10:15:30", result, "Should return the expected formatted date and time");
        }
    }

    @Test
    public void testProcessWithTimeOnlyFormat() {
        try (MockedStatic<LocalDateTime> mockedDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedDateTime.when(LocalDateTime::now).thenReturn(FIXED_DATETIME);

            String format = "HH:mm:ss";
            String result = timestampVariable.process(format);

            assertEquals("10:15:30", result, "Should format time-only correctly");
        }
    }

    @Test
    public void testProcessWithDateOnlyFormat() {
        try (MockedStatic<LocalDateTime> mockedDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedDateTime.when(LocalDateTime::now).thenReturn(FIXED_DATETIME);

            String format = "yyyy/MM/dd";
            String result = timestampVariable.process(format);

            assertEquals("2025/05/14", result, "Should format date-only correctly");
        }
    }

    @Test
    public void testProcessWithCustomTextFormat() {
        try (MockedStatic<LocalDateTime> mockedDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedDateTime.when(LocalDateTime::now).thenReturn(FIXED_DATETIME);

            String format = "'Date:'yyyy-MM-dd' Time:'HH:mm:ss";
            String result = timestampVariable.process(format);

            assertEquals("Date:2025-05-14 Time:10:15:30", result, "Should format with custom text correctly");
        }
    }

    @Test
    public void testProcessWithMonthNameFormat() {
        try (MockedStatic<LocalDateTime> mockedDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedDateTime.when(LocalDateTime::now).thenReturn(FIXED_DATETIME);

            String format = "MMMM d, yyyy";
            String result = timestampVariable.process(format);

            assertEquals("May 14, 2025", result, "Should format with month name correctly");
        }
    }

    @Test
    public void testProcessWithMillisecondsFormat() {
        LocalDateTime dateTimeWithMs = LocalDateTime.of(2025, 5, 14, 10, 15, 30, 123000000);

        try (MockedStatic<LocalDateTime> mockedDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedDateTime.when(LocalDateTime::now).thenReturn(dateTimeWithMs);

            String format = "HH:mm:ss.SSS";
            String result = timestampVariable.process(format);

            assertEquals("10:15:30.123", result, "Should format with milliseconds correctly");
        }
    }

    @Test
    public void testProcessDefaultMethod() {
        try (MockedStatic<LocalDateTime> mockedDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedDateTime.when(LocalDateTime::now).thenReturn(FIXED_DATETIME);

            String result = timestampVariable.process();
            String expected = FIXED_DATETIME.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            assertEquals(expected, result, "Default process method should use ISO_LOCAL_DATE_TIME format");
            assertEquals("2025-05-14T10:15:30", result, "Should return ISO formatted timestamp");
        }
    }

    @Test
    public void testProcessWithInvalidFormat() {
        try (MockedStatic<LocalDateTime> mockedDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedDateTime.when(LocalDateTime::now).thenReturn(FIXED_DATETIME);

            String result = timestampVariable.process();
            String expected = FIXED_DATETIME.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            assertEquals(expected, result, "Default process method should use ISO_LOCAL_DATE_TIME format");
            assertEquals("2025-05-14T10:15:30", result, "Should return ISO formatted timestamp");
        }
    }

    @Test
    public void testProcessWithNullFormat() {
        try (MockedStatic<LocalDateTime> mockedDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedDateTime.when(LocalDateTime::now).thenReturn(FIXED_DATETIME);

            assertThrows(
                    NullPointerException.class,
                    () -> timestampVariable.process(null),
                    "Should throw NullPointerException for null format");
        }
    }

    @Test
    public void testProcessWithEmptyFormat() {
        try (MockedStatic<LocalDateTime> mockedDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedDateTime.when(LocalDateTime::now).thenReturn(FIXED_DATETIME);

            String result = timestampVariable.process();
            String expected = FIXED_DATETIME.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            assertEquals(expected, result, "Default process method should use ISO_LOCAL_DATE_TIME format");
            assertEquals("2025-05-14T10:15:30", result, "Should return ISO formatted timestamp");
        }
    }

    @Test
    public void testDifferentTimestampValues() {
        LocalDateTime timestamp1 = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime timestamp2 = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

        try (MockedStatic<LocalDateTime> mockedDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            // First timestamp
            mockedDateTime.when(LocalDateTime::now).thenReturn(timestamp1);
            String format = "yyyy-MM-dd HH:mm:ss";
            String result1 = timestampVariable.process(format);
            assertEquals("2025-01-01 00:00:00", result1, "Should handle start of year timestamp");

            // Second timestamp
            mockedDateTime.when(LocalDateTime::now).thenReturn(timestamp2);
            String result2 = timestampVariable.process(format);
            assertEquals("2025-12-31 23:59:59", result2, "Should handle end of year timestamp");
        }
    }

    @Test
    public void testLocaleSpecificFormatting() {
        try (MockedStatic<LocalDateTime> mockedDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedDateTime.when(LocalDateTime::now).thenReturn(FIXED_DATETIME);

            // Day of week name
            String format = "EEEE";
            String result = timestampVariable.process(format);

            // May 14, 2025 is a Wednesday
            assertEquals("Wednesday", result, "Should format day of week correctly");
        }
    }
}
