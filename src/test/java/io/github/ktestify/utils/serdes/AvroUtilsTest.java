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
package io.github.ktestify.utils.serdes;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import io.github.ktestify.exceptions.ComparisonException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@Slf4j
class AvroUtilsTest {

    @Test
    void testGetPrettyAvroValue() {
        String uglyJson = "{\"name\":\"John\", \"age\":30}";
        String expectedPrettyJson = "{\n  \"name\": \"John\",\n  \"age\": 30\n}";

        String actualPrettyJson = AvroUtils.getPrettyAvroValue(uglyJson);

        assertEquals(expectedPrettyJson, actualPrettyJson, "The pretty JSON does not match the expected format.");
    }

    @Test
    void testDoesAvroRecordsStrictlyMatches() {
        String record1 = "{\"name\":\"John\", \"age\":30}";
        String record2 = "{\"name\":\"John\", \"age\":30}";

        Assertions.assertTrue(
                AvroUtils.doesAvroRecordsStrictlyMatches(record1, record2), "The records should strictly match.");
    }

    @Test
    void testDoesAvroRecordsStrictlyNotMatches() {
        String record1 = "{\"name\":\"John\", \"age\":30}";
        String record2 = "{\n \"age\":30,\n \"name\":\"John\"\n}";

        Assertions.assertTrue(
                AvroUtils.doesAvroRecordsStrictlyMatches(record1, record2),
                "The records should not strictly match due to different order.");
    }

    @Test
    void testDoesAvroRecordsSmartMatches() {
        String record1 = "{\"name\":\"John\", \"age\":30}";
        String record2 = "{\"age\":30, \"name\":\"John\"}";

        Assertions.assertTrue(
                AvroUtils.doesAvroRecordsSmartMatches(record1, record2),
                "The records should smartly match regardless of order.");
    }

    @Test
    void testGetJsonElementFromFile() {
        String jsonContent = "{\"test\":\"value\"}";
        JsonElement expected = new JsonParser().parse(jsonContent);

        JsonElement result = AvroUtils.getJsonElementFromFile(jsonContent);

        assertEquals(expected, result, "The returned JsonElement does not match the expected.");
    }

    @Test
    void testDoesAvroValueFromKeyMatchesRecords() {
        String key = "person.name";
        String record1 = "{\"person\":{\"name\":\"John\", \"age\":30}}";
        String record2 = "{\"person\":{\"name\":\"John\", \"age\":25}}";

        Assertions.assertTrue(
                AvroUtils.doesAvroValueFromKeyMatchesRecords(key, record1, record2),
                "The value for the key should match across records.");
    }

    @Test
    void testDoesAvroValueFromKeyNotMatchesRecords() {
        String key = "person.name";
        String record1 = "{\"person\":{\"name\":\"John\", \"age\":30}}";
        String record2 = "{\"person\":{\"name\":\"Jane\", \"age\":30}}";

        Assertions.assertFalse(
                AvroUtils.doesAvroValueFromKeyMatchesRecords(key, record1, record2),
                "The value for the key should not match across records.");
    }

    @Test
    void testDoesAvroValueFromNestedKeyMatchesRecord() {
        String expectedValue = "John";
        String key = "person.name";
        String record = "{\"person\":{\"name\":\"John\", \"age\":30}}";

        Assertions.assertTrue(
                AvroUtils.doesAvroValueFromKeyMatchesRecord(expectedValue, key, record),
                "The value for the key should match the expected value.");
    }

    @Test
    void testDoesAvroValueFromMainObjectKeyMatchesRecords() {
        String key = "amount";
        String record1 = "{\"person\":{\"name\":\"John\", \"age\":30}, \"amount\": \"7\"}";
        String record2 = "{\"person\":{\"name\":\"John\", \"age\":30}, \"amount\": \"7\"}";

        Assertions.assertTrue(
                AvroUtils.doesAvroValueFromKeyMatchesRecords(key, record1, record2),
                "The value for the key should match the expected value.");
    }

    @Test
    void testDoesAvroValueFromKeyNotMatchesRecord() {
        String expectedValue = "Jane";
        String key = "person.name";
        String record = "{\"person\":{\"name\":\"John\", \"age\":30}}";

        Assertions.assertFalse(
                AvroUtils.doesAvroValueFromKeyMatchesRecord(expectedValue, key, record),
                "The value for the key should not match the expected value.");
    }

    @Test
    void testDoesAvroValueFromMainObjectKeyMatchesRecord() {
        String key = "amount";
        String record1 = "{\"person\":{\"name\":\"John\", \"age\":30}, \"amount\": \"7\"}";

        Assertions.assertTrue(
                AvroUtils.doesAvroValueFromKeyMatchesRecord("7", key, record1),
                "The value for the key should match the expected value.");
    }

    @Test
    void testConvertJsonToAvro() {
        // Example: Assuming a simple schema and JSON
        Schema schema = Schema.create(Schema.Type.STRING);
        JsonElement jsonElement = new JsonParser().parse("\"testString\"");

        Object result = AvroUtils.convertJsonToAvro(jsonElement, schema);

        assertEquals("testString", result, "The conversion to Avro did not produce the expected result.");
    }

    @Test
    void testDeepEqualsWithSimpleMaps() {
        Map<String, Object> map1 = Map.of("key1", "value1", "key2", "value2");
        Map<String, Object> map2 = Map.of("key1", "value1", "key2", "value2");

        Assertions.assertTrue(AvroUtils.deepEquals(map1, map2), "The maps should be considered equal.");
    }

    @Test
    void testDeepNotEqualsWithSimpleMaps() {
        Map<String, Object> map1 = Map.of("key1", "value1", "key2", "value2");
        Map<String, Object> map2 = Map.of("key1", "value1", "key2", "differentValue");

        Assertions.assertFalse(AvroUtils.deepEquals(map1, map2), "The maps should not be considered equal.");
    }

    @Test
    void testDeepEqualsWithNestedMaps() {
        Map<String, Object> nestedMap1 = Map.of("nestedKey1", "nestedValue1");
        Map<String, Object> nestedMap2 = Map.of("nestedKey1", "nestedValue1");

        Map<String, Object> map1 = Map.of("key1", nestedMap1);
        Map<String, Object> map2 = Map.of("key1", nestedMap2);

        Assertions.assertTrue(
                AvroUtils.deepEquals(map1, map2), "The maps with nested maps should be considered equal.");
    }

    @Test
    void testDeepNotEqualsWithNestedMaps() {
        Map<String, Object> nestedMap1 = Map.of("nestedKey1", "nestedValue1");
        Map<String, Object> nestedMap2 = Map.of("nestedKey1", "differentNestedValue");

        Map<String, Object> map1 = Map.of("key1", nestedMap1);
        Map<String, Object> map2 = Map.of("key1", nestedMap2);

        Assertions.assertFalse(
                AvroUtils.deepEquals(map1, map2), "The maps with nested maps should not be considered equal.");
    }

    @Test
    void testDeepEqualsWithEqualNestedMaps() {
        Map<String, Object> map1 = Map.of("key1", "value1", "key2", "value2");

        Assertions.assertTrue(AvroUtils.deepEquals(map1, map1), "The maps should not be considered equal.");
    }

    @Test
    void testDeepNotEqualsWithSizeMismatchMaps() {
        Map<String, Object> map1 = Map.of("key1", "value1", "key2", "value2");
        Map<String, Object> map2 = Map.of("key1", "value1", "key2", "value2", "key3", "value2");

        Assertions.assertFalse(AvroUtils.deepEquals(map1, map2), "The maps should not be considered equal.");
    }

    @Test
    void testDeepNotEqualsWithNullMaps() {
        Map<String, Object> map1 = Map.of("key1", "value1", "key2", "value2");

        assertThrows(NullPointerException.class, () -> AvroUtils.deepEquals(map1, null));
    }

    @Test
    void testDeepNotEqualsWithMissingKeyMaps() {
        Map<String, Object> map1 = Map.of("key1", "value1", "key2", "value2");
        Map<String, Object> map2 = Map.of("key1", "value1", "key", "value2");

        Assertions.assertFalse(AvroUtils.deepEquals(map1, map2), "The maps should not be considered equal.");
    }

    @Test
    void testDeepNotEqualsWithNullValueMaps() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key1", "value1");
        map1.put("key", null);
        Map<String, Object> map2 = Map.of("key1", "value1", "key", "value2");

        Assertions.assertFalse(AvroUtils.deepEquals(map1, map2), "The maps should not be considered equal.");
    }

    @Test
    void testConvertJsonToAvroUnionType() {
        // Setup JSON element and UNION schema
        JsonElement jsonElement = new JsonParser().parse("\"testString\"");
        Schema nonNullSchema = Schema.create(Schema.Type.STRING);
        Schema unionSchema = Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), nonNullSchema));

        Object result = AvroUtils.convertJsonToAvro(jsonElement, unionSchema);

        assertEquals("testString", result, "The conversion for UNION type did not produce the expected result.");
    }

    @Test
    void testConvertJsonToAvroMapType() {
        // Setup JSON object and MAP schema
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("key", "value");
        JsonElement jsonElement = jsonObject;
        Schema mapSchema = Schema.createMap(Schema.create(Schema.Type.STRING));

        Object result = AvroUtils.convertJsonToAvro(jsonElement, mapSchema);

        // Assert that the result is a map with the correct contents
        assertNotNull(result);
        assertInstanceOf(Map.class, result);
        assertEquals(
                "value",
                ((Map<?, ?>) result).get("key"),
                "The conversion for MAP type did not produce the expected result.");
    }

    @Test
    void testConvertJsonToAvroArrayType() {
        // Setup JSON array and ARRAY schema
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(new JsonPrimitive("element1"));
        jsonArray.add(new JsonPrimitive("element2"));
        JsonElement jsonElement = jsonArray;
        Schema arraySchema = Schema.createArray(Schema.create(Schema.Type.STRING));

        Object result = AvroUtils.convertJsonToAvro(jsonElement, arraySchema);

        // Assert that the result is an array with the correct contents
        assertNotNull(result, "The conversion result should not be null.");
        assertInstanceOf(List.class, result, "The result should be an instance of List.");
        assertEquals(
                "element1", ((List<?>) result).get(0), "The first element of the conversion result does not match.");
    }

    @Test
    void testConvertJsonToAvroBytesType() {
        // Setup JSON element and BYTES schema with decimal logical type
        JsonElement jsonElement = new JsonParser().parse("\"123.45\"");
        LogicalTypes.Decimal decimalLogicalType = LogicalTypes.decimal(5, 2);
        Schema bytesSchema = Schema.create(Schema.Type.BYTES);
        decimalLogicalType.addToSchema(bytesSchema);

        Object result = AvroUtils.convertJsonToAvro(jsonElement, bytesSchema);

        // Assert that the result is a ByteBuffer with the correct content
        assertNotNull(result);
        assertInstanceOf(ByteBuffer.class, result);
        // Additional assertions can be added to validate the decimal value
    }

    @Test
    void testConvertJsonToAvroRecordType() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("stringField", "stringValue");
        JsonElement jsonElement = jsonObject;
        Schema recordSchema = SchemaBuilder.record("TestRecord")
                .fields()
                .name("stringField")
                .type()
                .stringType()
                .noDefault()
                .endRecord();

        Object result = AvroUtils.convertJsonToAvro(jsonElement, recordSchema);

        assertNotNull(result, "The conversion result should not be null.");
        assertInstanceOf(GenericRecord.class, result, "The result should be an instance of GenericRecord.");
        assertEquals(
                "stringValue", ((GenericRecord) result).get("stringField"), "The field in the record does not match.");
    }

    @Test
    void testConvertJsonToAvroBooleanType() {
        JsonElement jsonElement = new JsonPrimitive(true);
        Schema booleanSchema = Schema.create(Schema.Type.BOOLEAN);

        Object result = AvroUtils.convertJsonToAvro(jsonElement, booleanSchema);

        assertNotNull(result, "The conversion result should not be null.");
        assertInstanceOf(Boolean.class, result, "The result should be an instance of Boolean.");
        Assertions.assertTrue((Boolean) result, "The Boolean value does not match the expected result.");
    }

    @Test
    void testConvertJsonToAvroIntType() {
        JsonElement jsonElement = new JsonPrimitive(123);
        Schema intSchema = Schema.create(Schema.Type.INT);

        Object result = AvroUtils.convertJsonToAvro(jsonElement, intSchema);

        assertNotNull(result, "The conversion result should not be null.");
        assertInstanceOf(Integer.class, result, "The result should be an instance of Integer.");
        assertEquals(123, result, "The Integer value does not match the expected result.");
    }

    @Test
    void testConvertJsonToAvroLongType() {
        JsonElement jsonElement = new JsonPrimitive(123L);
        Schema longSchema = Schema.create(Schema.Type.LONG);

        Object result = AvroUtils.convertJsonToAvro(jsonElement, longSchema);

        assertNotNull(result, "The conversion result should not be null.");
        assertInstanceOf(Long.class, result, "The result should be an instance of Long.");
        assertEquals(123L, result, "The Long value does not match the expected result.");
    }

    @Test
    void testConvertJsonToAvroFloatType() {
        JsonElement jsonElement = new JsonPrimitive(123.45f);
        Schema floatSchema = Schema.create(Schema.Type.FLOAT);

        Object result = AvroUtils.convertJsonToAvro(jsonElement, floatSchema);

        assertNotNull(result, "The conversion result should not be null.");
        assertInstanceOf(Float.class, result, "The result should be an instance of Float.");
        assertEquals(123.45f, result, "The Float value does not match the expected result.");
    }

    @Test
    void testConvertJsonToAvroDoubleType() {
        JsonElement jsonElement = new JsonPrimitive(123.45);
        Schema doubleSchema = Schema.create(Schema.Type.DOUBLE);

        Object result = AvroUtils.convertJsonToAvro(jsonElement, doubleSchema);

        assertNotNull(result, "The conversion result should not be null.");
        assertInstanceOf(Double.class, result, "The result should be an instance of Double.");
        assertEquals(123.45, result, "The Double value does not match the expected result.");
    }

    @Test
    void testKeyEvalWithArrayItem() {
        String jsonPayload = "{\"users\":[{\"name\":\"Alice\"},{\"name\":\"Bob\"},{\"name\":\"Charlie\"}]}";
        String jsonPayloadF = "{\"users\":[{\"name\":\"Alicse\"},{\"name\":\"Bob\"},{\"name\":\"Charlie\"}]}";

        Assertions.assertTrue(AvroUtils.doesAvroValueFromKeyMatchesRecords("users[2].name", jsonPayload, jsonPayloadF));
    }

    @Test
    void testKeyEvalWithArrayItemOutOfBounds() {
        String jsonPayload = "{\"users\":[{\"name\":\"Alice\"},{\"name\":\"Bob\"},{\"name\":\"Charlie\"}]}";
        String jsonPayloadF = "{\"users\":[{\"name\":\"Alicse\"},{\"name\":\"Bob\"},{\"name\":\"Charlie\"}]}";

        Assertions.assertFalse(
                AvroUtils.doesAvroValueFromKeyMatchesRecords("users[5].name", jsonPayload, jsonPayloadF));
    }

    @Test
    void testConvertJsonArrayToAvroArrayWithRecordType() {
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("stringField", "stringValue");
        jsonArray.add(jsonObject);

        Schema recordSchema = SchemaBuilder.record("TestRecord")
                .fields()
                .name("stringField")
                .type()
                .stringType()
                .noDefault()
                .endRecord();
        Schema arraySchema = Schema.createArray(recordSchema);

        Object result = AvroUtils.convertJsonToAvro(jsonArray, arraySchema);

        assertNotNull(result, "The conversion result should not be null.");
        assertInstanceOf(GenericData.Array.class, result, "The result should be an instance of GenericData.Array.");
        GenericData.Array<?> avroArray = (GenericData.Array<?>) result;
        Assertions.assertFalse(avroArray.isEmpty(), "The Avro array should not be empty.");
        assertInstanceOf(
                GenericRecord.class, avroArray.get(0), "The first element of the array should be a GenericRecord.");
        assertEquals(
                "stringValue",
                ((GenericRecord) avroArray.get(0)).get("stringField"),
                "The field in the record does not match.");
    }

    @Test
    void testConvertJsonToAvroArrayWithMapElements() {
        JsonArray jsonArray = new JsonArray();

        JsonObject mapObject1 = new JsonObject();
        JsonObject innerMap1 = new JsonObject();
        innerMap1.addProperty("key", "value1");
        mapObject1.add("map1", innerMap1);
        jsonArray.add(mapObject1);

        JsonObject mapObject2 = new JsonObject();
        JsonObject innerMap2 = new JsonObject();
        innerMap2.addProperty("key", "value2");
        mapObject2.add("map2", innerMap2);
        jsonArray.add(mapObject2);

        // Define Avro schema for a MAP type
        Schema mapSchema = Schema.createMap(Schema.create(Schema.Type.STRING));
        // Define Avro schema for an ARRAY containing these MAP elements
        Schema arraySchema = Schema.createArray(mapSchema);

        // Convert the JSON array to an Avro array using the convertJsonToAvro function
        Object result = AvroUtils.convertJsonToAvro(jsonArray, arraySchema);

        // Assert the result
        assertNotNull(result, "The conversion result should not be null.");
        assertInstanceOf(GenericData.Array.class, result, "The result should be an instance of GenericData.Array.");

        GenericData.Array<?> avroArray = (GenericData.Array<?>) result;
        assertEquals(
                jsonArray.size(),
                avroArray.size(),
                "The Avro array should have the same number of elements as the JSON array.");

        // Additional assertions can be made to check the contents of the Avro maps
        for (int i = 0; i < avroArray.size(); i++) {
            Object avroElement = avroArray.get(i);
            assertInstanceOf(Map.class, avroElement, "Each element of the Avro array should be a Map.");
            // Further checks can be added to validate the contents of each map
        }
    }

    @Test
    void testDoesAvroRecordsSmartMatchesWithExclusions_MatchingRecords() {
        String expectedRecord = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        String actualRecord = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        List<String> excludedKeys = List.of("key2");

        boolean result =
                AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(expectedRecord, actualRecord, excludedKeys);

        Assertions.assertTrue(result);
    }

    @Test
    void testDoesAvroRecordsSmartMatchesWithExclusions_NonMatchingRecords() {
        String expectedRecord = "{\"key1\":\"value1\",\"key2\":\"value3\"}";
        String actualRecord = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        List<String> excludedKeys = List.of("key1");

        boolean result =
                AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(expectedRecord, actualRecord, excludedKeys);

        Assertions.assertFalse(result);
    }

    @Test
    void testDoesAvroRecordsSmartMatchesWithExclusions_ExcludedKeys() {
        String expectedRecord = "{\"key1\":\"value1\",\"key2\":\"value3\"}";
        String actualRecord = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        List<String> excludedKeys = List.of("key2");

        boolean result =
                AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(expectedRecord, actualRecord, excludedKeys);

        Assertions.assertTrue(result);
    }

    @Test
    void deepEqualsWithExclusions_ShouldReturnTrue_WhenMapsAreEqualAndExcludedKeysAreNotConsidered() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key1", "value1");
        map1.put("key2", "value2");
        Map<String, Object> map2 = new HashMap<>();
        map2.put("key1", "value1");
        map2.put("key2", "value3");
        List<String> excludedKeys = List.of("key2");

        boolean result = AvroUtils.deepEquals(map1, map2, excludedKeys);

        Assertions.assertTrue(result);
    }

    @Test
    void deepEqualsWithExclusions_ShouldReturnFalse_WhenMapsAreNotEqualEvenAfterExcludingKeys() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key1", "value1");
        map1.put("key2", "value2");
        Map<String, Object> map2 = new HashMap<>();
        map2.put("key1", "value3");
        map2.put("key2", "value2");
        List<String> excludedKeys = List.of("key2");

        boolean result = AvroUtils.deepEquals(map1, map2, excludedKeys);

        Assertions.assertFalse(result);
    }

    @Test
    void deepEqualsWithExclusions_ShouldReturnFalse_WhenActualMapDoesNotContainKeyFromExpectedMap() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key1", "value1");
        map1.put("key2", "value2");
        Map<String, Object> map2 = new HashMap<>();
        map2.put("key1", "value1");
        List<String> excludedKeys = List.of();

        boolean result = AvroUtils.deepEquals(map1, map2, excludedKeys);

        Assertions.assertFalse(result);
    }

    @Test
    void deepEqualsWithExclusions_ShouldReturnTrue_WhenMapsAreEqualAndNoKeysAreExcluded() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key1", "value1");
        map1.put("key2", "value2");
        Map<String, Object> map2 = new HashMap<>();
        map2.put("key1", "value1");
        map2.put("key2", "value2");
        List<String> excludedKeys = List.of();

        boolean result = AvroUtils.deepEquals(map1, map2, excludedKeys);

        Assertions.assertTrue(result);
    }

    @Test
    void convertMapToJsonString_ShouldReturnPrettyPrintedJson_WhenMapIsNotEmpty() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        String result = AvroUtils.convertMapToJsonString(map);

        String expected = "{\n  \"key1\": \"value1\",\n  \"key2\": \"value2\"\n}";
        assertEquals(expected, result);
    }

    @Test
    void convertMapToJsonString_ShouldReturnEmptyJsonObject_WhenMapIsEmpty() {
        Map<String, Object> map = new HashMap<>();

        String result = AvroUtils.convertMapToJsonString(map);

        String expected = "{}";
        assertEquals(expected, result);
    }

    @Test
    void convertMapToJsonString_ShouldHandleNestedMaps() {
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("nestedKey", "nestedValue");

        Map<String, Object> map = new HashMap<>();
        map.put("key", nestedMap);

        String result = AvroUtils.convertMapToJsonString(map);

        String expected = "{\n  \"key\": {\n    \"nestedKey\": \"nestedValue\"\n  }\n}";
        assertEquals(expected, result);
    }

    @Test
    void convertDatesToTimestamps_ShouldNotConvertDateLikeStrings_SinceNoTypeInformationIsAvailable() {
        // Content-sniffing was removed (issue #49): a Map on its own carries no schema information, so
        // date-like strings must be left untouched. Type-driven conversion only happens in compareValues(),
        // where the actual side's real Java type (Instant/LocalDateTime/LocalDate) is known.
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("date1", "2022-01-01T00:00:00Z");
        inputMap.put("date2", "2022-01-02T00:00:00Z");

        Map<String, Object> result = AvroUtils.convertDatesToTimestamps(inputMap);

        assertEquals("2022-01-01T00:00:00Z", result.get("date1"));
        assertEquals("2022-01-02T00:00:00Z", result.get("date2"));
    }

    @Test
    void convertDatesToTimestamps_ShouldReturnSameMap_WhenInputMapHasNoDateStrings() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("key1", "value1");
        inputMap.put("key2", "value2");

        Map<String, Object> result = AvroUtils.convertDatesToTimestamps(inputMap);

        assertEquals(inputMap, result);
    }

    @Test
    void convertDateStringToTimestamp_ShouldReturnTimestamp_WhenInputIsDateString() throws ParseException {
        String dateString = "2022-01-01T00:00:00Z";

        long result = AvroUtils.convertDateStringToTimestamp(dateString);

        assertEquals(1640995200000L, result);
    }

    @Test
    void convertDateStringToTimestamp_ShouldThrowParseException_WhenInputIsNotDateString() {
        String notDateString = "Not a date string";

        Assertions.assertThrows(ParseException.class, () -> AvroUtils.convertDateStringToTimestamp(notDateString));
    }

    @Test
    void testGetHeadersMapWithValidJson() {
        String jsonString =
                "{\"headers\": {\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer token\"}}";
        Map<String, String> result = AvroUtils.getHeadersMap(jsonString);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("application/json", result.get("Content-Type"));
        assertEquals("Bearer token", result.get("Authorization"));
    }

    @Test
    void testGetHeadersMapWithNoHeaders() {
        String jsonString = "{\"data\": {\"name\": \"value\"}}";
        Map<String, String> result = AvroUtils.getHeadersMap(jsonString);
        assertNull(result);
    }

    @Test
    void testGetHeadersMapWithEmptyJson() {
        String jsonString = "{}";
        Map<String, String> result = AvroUtils.getHeadersMap(jsonString);
        assertNull(result);
    }

    @Test
    void testGetHeadersMapWithMalformedJson() {
        String jsonString = "{headers: {\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer token\"}";
        assertThrows(com.google.gson.JsonSyntaxException.class, () -> AvroUtils.getHeadersMap(jsonString));
    }

    @Test
    void testGetHeadersMapWithEmptyHeaders() {
        String jsonString = "{\"headers\": {}}";
        Map<String, String> result = AvroUtils.getHeadersMap(jsonString);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testConvertJsonPrimitiveToAvro_LongRegular() {
        // Arrange
        JsonElement jsonElement = new JsonPrimitive(1234567890L);
        Schema schema = Schema.create(Schema.Type.LONG);

        // Act
        Object result = AvroUtils.convertJsonToAvro(jsonElement, schema);

        // Assert
        assertAll(() -> assertInstanceOf(Long.class, result), () -> assertEquals(1234567890L, result));
    }

    @Test
    void testConvertJsonPrimitiveToAvro_LongTimestampMillis() {
        // Arrange
        JsonElement jsonElement = new JsonPrimitive("2024-06-11T14:41:43.014Z");
        Schema schema = LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG));

        // Act
        Object result = AvroUtils.convertJsonToAvro(jsonElement, schema);

        // Assert
        assertAll(
                () -> assertInstanceOf(Long.class, result),
                () -> assertEquals(1718116903014L, result) // Expected timestamp in milliseconds
                );
    }

    @Test
    void testConvertJsonPrimitiveToAvro_LongTimestampMillis_with_no_ms() {
        // Arrange
        JsonElement jsonElement = new JsonPrimitive("2024-06-11T14:41:43Z");
        Schema schema = LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG));

        Object result = AvroUtils.convertJsonToAvro(jsonElement, schema);

        assertAll(
                () -> assertInstanceOf(Long.class, result),
                () -> assertEquals(1718116903000L, result) // Expected timestamp in milliseconds
                );
    }

    @Test
    void testConvertDatesToTimestamps_MainObject() {
        String json = """
            {
              "date": "2025-01-02T00:00:00Z",
              "value": 123
            }
            """;
        Map<String, Object> jsonMap = AvroUtils.convertJsonToMap(json);
        Map<String, Object> result = AvroUtils.convertDatesToTimestamps(jsonMap);

        // No type information is available for a standalone map, so the date-like string is left untouched.
        assertEquals("2025-01-02T00:00:00Z", result.get("date"));
    }

    @Test
    void testConvertDatesToTimestamps_NestedObject() {
        String json = """
            {
              "nested": {
                "date": "2025-01-02T00:00:00Z",
                "value": 456
              }
            }
            """;
        Map<String, Object> jsonMap = AvroUtils.convertJsonToMap(json);
        Map<String, Object> result = AvroUtils.convertDatesToTimestamps(jsonMap);

        // Recursion into nested maps now works, but no conversion is performed: the nested date-like string
        // must remain untouched.
        assertEquals("2025-01-02T00:00:00Z", ((Map<?, ?>) result.get("nested")).get("date"));
    }

    @Test
    void testConvertDatesToTimestamps_NestedList() {
        String json = """
            {
              "list": [
                {
                  "date": "2025-01-02T00:00:00Z",
                  "value": 789
                },
                {
                  "date": "2025-01-03T00:00:00Z",
                  "value": 1011
                }
              ]
            }
            """;
        Map<String, Object> jsonMap = AvroUtils.convertJsonToMap(json);
        Map<String, Object> result = AvroUtils.convertDatesToTimestamps(jsonMap);

        List<?> list = (List<?>) result.get("list");

        // Recursion into nested list items works, but no conversion is performed: dates remain untouched.
        assertEquals("2025-01-02T00:00:00Z", ((Map<?, ?>) list.get(0)).get("date"));
        assertEquals("2025-01-03T00:00:00Z", ((Map<?, ?>) list.get(1)).get("date"));
    }

    @Test
    void testConvertDatesToTimestamps_NoDates() {
        String json = """
            {
              "value": 123,
              "nested": {
                "value": 456
              }
            }
            """;
        Map<String, Object> jsonMap = AvroUtils.convertJsonToMap(json);
        Map<String, Object> result = AvroUtils.convertDatesToTimestamps(jsonMap);

        assertEquals(123L, result.get("value"));
        assertEquals(456L, ((Map<?, ?>) result.get("nested")).get("value"));
    }

    @Test
    void testConvertDatesToTimestamps_InvalidDateFormat() {
        String json = """
            {
              "date": "invalid-date-format",
              "value": 123
            }
            """;
        Map<String, Object> jsonMap = AvroUtils.convertJsonToMap(json);
        Map<String, Object> result = AvroUtils.convertDatesToTimestamps(jsonMap);

        assertEquals("invalid-date-format", result.get("date"));
    }

    @Test
    void testConvertJsonPrimitiveToAvro_LongTimestampMicros() {
        // Arrange
        JsonElement jsonElement = new JsonPrimitive("2024-06-11T14:41:43.123456Z");
        Schema schema = LogicalTypes.timestampMicros().addToSchema(Schema.create(Schema.Type.LONG));

        // Act
        Object result = AvroUtils.convertJsonToAvro(jsonElement, schema);

        // Assert
        assertAll(
                () -> assertInstanceOf(Long.class, result),
                () -> assertEquals(1718116903123456L, result) // Expected timestamp in microseconds
                );
    }

    @Test
    void testConvertJsonPrimitiveToAvro_LongTimestampMicros_with_no_micros() {
        // Arrange
        JsonElement jsonElement = new JsonPrimitive("2024-06-11T14:41:43Z");
        Schema schema = LogicalTypes.timestampMicros().addToSchema(Schema.create(Schema.Type.LONG));

        // Act
        Object result = AvroUtils.convertJsonToAvro(jsonElement, schema);

        // Assert
        assertAll(
                () -> assertInstanceOf(Long.class, result),
                () -> assertEquals(1718116903000000L, result) // Expected timestamp in microseconds
                );
    }

    @Test
    void testConvertDateStringToTimestampMicros_ValidDateString() throws ParseException {
        // Arrange
        String dateString = "2024-06-11T14:41:43.123456Z";

        // Act
        long result = AvroUtils.convertDateStringToTimestampMicros(dateString);

        // Assert
        assertEquals(1718116903123456L, result);
    }

    @Test
    void testConvertDateStringToTimestampMicros_ValidDateString_with_no_micros() throws ParseException {
        // Arrange
        String dateString = "2024-06-11T14:41:43Z";

        // Act
        long result = AvroUtils.convertDateStringToTimestampMicros(dateString);

        // Assert
        assertEquals(1718116903000000L, result);
    }

    @Test
    void testConvertDateStringToTimestampMicros_InvalidDateString() {
        // Arrange
        String invalidDateString = "invalid-date-format";

        // Act & Assert
        assertThrows(ParseException.class, () -> AvroUtils.convertDateStringToTimestampMicros(invalidDateString));
    }

    @Test
    void testConvertDateStringToDateInt_ValidDateString() {
        // Arrange
        String dateString = "2025-04-25";

        // Act
        int result = AvroUtils.convertDateStringToDateInt(dateString);

        // Assert
        // Calculate expected days since epoch (1970-01-01)
        LocalDate date = LocalDate.parse(dateString);
        int expected = (int) date.toEpochDay();
        assertEquals(expected, result);
    }

    @Test
    void testConvertDateStringToDateInt_InvalidDateString() {
        // Arrange
        String invalidDateString = "invalid-date-format";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> AvroUtils.convertDateStringToDateInt(invalidDateString));
    }

    @Test
    void testConvertTimeStringToTimeMillis_ValidTimeString() {
        // Arrange
        String timeString = "12:30:45";

        // Act
        int result = AvroUtils.convertTimeStringToTimeMillis(timeString);

        // Assert
        // Calculate expected milliseconds since midnight
        LocalTime time = LocalTime.parse(timeString);
        int expected = (int) (time.toNanoOfDay() / 1_000_000);
        assertEquals(expected, result);
    }

    @Test
    void testConvertTimeStringToTimeMillis_InvalidTimeString() {
        // Arrange
        String invalidTimeString = "invalid-time-format";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> AvroUtils.convertTimeStringToTimeMillis(invalidTimeString));
    }

    @Test
    void testConvertJsonPrimitiveToAvro_IntDate() {
        // Arrange
        JsonElement jsonElement = new JsonPrimitive("2025-04-25");
        Schema schema = LogicalTypes.date().addToSchema(Schema.create(Schema.Type.INT));

        // Act
        Object result = AvroUtils.convertJsonToAvro(jsonElement, schema);

        // Assert
        assertAll(() -> assertInstanceOf(Integer.class, result), () -> {
            LocalDate date = LocalDate.parse("2025-04-25");
            int expected = (int) date.toEpochDay();
            assertEquals(expected, result);
        });
    }

    @Test
    void testConvertJsonPrimitiveToAvro_IntTimeMillis() {
        // Arrange
        JsonElement jsonElement = new JsonPrimitive("12:30:45");
        Schema schema = LogicalTypes.timeMillis().addToSchema(Schema.create(Schema.Type.INT));

        // Act
        Object result = AvroUtils.convertJsonToAvro(jsonElement, schema);

        // Assert
        assertAll(() -> assertInstanceOf(Integer.class, result), () -> {
            LocalTime time = LocalTime.parse("12:30:45");
            int expected = (int) (time.toNanoOfDay() / 1_000_000);
            assertEquals(expected, result);
        });
    }

    // Test for empty map scenarios
    @Test
    void testConvertMapToJsonString_WithNullMap() {
        Map<String, Object> map = null;
        String result = AvroUtils.convertMapToJsonString(map);
        assertEquals("{}", result, "Should return empty JSON object for null map");
    }

    // Test for handling different header scenarios
    @Test
    void testGetHeadersMapWithMalformedHeaders() {
        String jsonString = "{\"headers\": {\"Content-Type\": 123}}";
        Map<String, String> result = AvroUtils.getHeadersMap(jsonString);
        assertNotNull(result);
        assertEquals("123", result.get("Content-Type"), "Should convert non-string values to strings");
    }

    // Test array handling in nested value matching
    @Test
    void testMatchNestedValueWithInvalidArrayIndex() {
        String key = "users[10].name"; // Index out of bounds
        String json1 = "{\"users\":[{\"name\":\"Alice\"}]}";
        String json2 = "{\"users\":[{\"name\":\"Alice\"}]}";

        assertFalse(
                AvroUtils.doesAvroValueFromKeyMatchesRecords(key, json1, json2),
                "Should return false for out-of-bounds array index");
    }

    // Test invalid class cast scenarios in matchNestedValue
    @Test
    void testMatchNestedValueWithInvalidCast() {
        String key = "users.0.name"; // Trying to use dot notation for array
        String json1 = "{\"users\":[{\"name\":\"Alice\"}]}";
        String json2 = "{\"users\":[{\"name\":\"Alice\"}]}";

        assertFalse(
                AvroUtils.doesAvroValueFromKeyMatchesRecords(key, json1, json2),
                "Should return false for invalid path format causing ClassCastException");
    }

    // Test null values in matchNestedValue
    @Test
    void testMatchNestedValueWithNullValue() {
        String expectedValue = null;
        String nestedKey = "person.address";
        String json = "{\"person\":{\"name\":\"John\",\"address\":null}}";

        assertThrows(
                NullPointerException.class,
                () -> {
                    AvroUtils.doesAvroValueFromKeyMatchesRecords(nestedKey, json, expectedValue);
                },
                "Should throw NullPointerException when trying to match null value");
    }

    // Test exception handling in convertDateStringToTimestamp
    @Test
    void testConvertDateStringToTimestampWithInvalidFormat() {
        String invalidDate = "2023-13-32T25:61:61Z"; // Invalid month, day, hour, minute, second

        assertThrows(
                ParseException.class,
                () -> AvroUtils.convertDateStringToTimestamp(invalidDate),
                "Should throw ParseException for invalid date format");
    }

    // Test for UNION schemas with only NULL type
    @Test
    void testConvertJsonToAvroWithNullUnion() {
        JsonElement jsonElement = JsonNull.INSTANCE;
        Schema nullSchema = Schema.create(Schema.Type.NULL);
        Schema unionSchema = Schema.createUnion(Collections.singletonList(nullSchema));

        Object result = AvroUtils.convertJsonToAvro(jsonElement, unionSchema);

        assertNull(result, "Should return null for UNION schema with only NULL type");
    }

    // Test bytes conversion with non-decimal logical type
    @Test
    void testConvertJsonToAvroWithRegularBytes() {
        JsonElement jsonElement = new JsonPrimitive("123");
        Schema bytesSchema = Schema.create(Schema.Type.BYTES);

        Object result = AvroUtils.convertJsonToAvro(jsonElement, bytesSchema);

        assertInstanceOf(Byte.class, result, "Should convert to Byte when BYTES schema has no logical type");
    }

    // Test convertJsonObjectToRecord with missing fields
    @Test
    void testConvertJsonObjectToRecordWithMissingFields() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("existingField", "value");

        Schema recordSchema = SchemaBuilder.record("TestRecord")
                .fields()
                .name("existingField")
                .type()
                .stringType()
                .noDefault()
                .name("missingField")
                .type()
                .stringType()
                .noDefault() // Field not in JSON
                .endRecord();

        // Create a method to access the private method
        Schema.Type schemaType = Schema.Type.RECORD;

        Object result = AvroUtils.convertJsonToAvro(jsonObject, recordSchema);

        assertInstanceOf(GenericRecord.class, result, "Should return a GenericRecord");
        GenericRecord record = (GenericRecord) result;
        assertEquals("value", record.get("existingField"), "Should set the existing field value");
        assertNull(record.get("missingField"), "Missing field should be null");
    }

    // Test complex nested conversions
    @Test
    void testConvertJsonToAvroWithNestedComplexTypes() {
        // Create a complex record schema with nested record, array, and map
        Schema innerRecordSchema = SchemaBuilder.record("InnerRecord")
                .fields()
                .name("innerField")
                .type()
                .stringType()
                .noDefault()
                .endRecord();

        Schema recordSchema = SchemaBuilder.record("OuterRecord")
                .fields()
                .name("stringField")
                .type()
                .stringType()
                .noDefault()
                .name("arrayField")
                .type()
                .array()
                .items(innerRecordSchema)
                .noDefault()
                .name("mapField")
                .type()
                .map()
                .values(Schema.create(Schema.Type.STRING))
                .noDefault()
                .endRecord();

        // Create complex JSON to match the schema
        JsonObject innerObject = new JsonObject();
        innerObject.addProperty("innerField", "innerValue");

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(innerObject);

        JsonObject mapObject = new JsonObject();
        mapObject.addProperty("mapKey", "mapValue");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("stringField", "stringValue");
        jsonObject.add("arrayField", jsonArray);
        jsonObject.add("mapField", mapObject);

        Object result = AvroUtils.convertJsonToAvro(jsonObject, recordSchema);

        assertNotNull(result, "Conversion result should not be null");
        assertInstanceOf(GenericRecord.class, result, "Result should be a GenericRecord");

        GenericRecord record = (GenericRecord) result;
        assertEquals("stringValue", record.get("stringField"), "String field should match");

        assertInstanceOf(
                GenericData.Array.class, record.get("arrayField"), "Array field should be a GenericData.Array");
        GenericData.Array<?> array = (GenericData.Array<?>) record.get("arrayField");
        assertFalse(array.isEmpty(), "Array should not be empty");
        assertInstanceOf(GenericRecord.class, array.getFirst(), "Array item should be a GenericRecord");
        assertEquals("innerValue", ((GenericRecord) array.getFirst()).get("innerField"), "Inner field should match");

        assertInstanceOf(Map.class, record.get("mapField"), "Map field should be a Map");
        Map<?, ?> map = (Map<?, ?>) record.get("mapField");
        assertEquals("mapValue", map.get("mapKey"), "Map value should match");
    }

    // Test date conversion in nested structures
    @Test
    void testConvertDatesToTimestampsInNestedStructures() {
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("nestedDate", "2022-01-01T00:00:00Z");

        List<Object> dateList = new ArrayList<>();
        Map<String, Object> listItem = new HashMap<>();
        listItem.put("itemDate", "2022-01-02T00:00:00Z");
        dateList.add(listItem);

        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("date", "2022-01-03T00:00:00Z");
        inputMap.put("nested", nestedMap);
        inputMap.put("list", dateList);

        Map<String, Object> result = AvroUtils.convertDatesToTimestamps(inputMap);

        // No conversion is performed anymore (content-sniffing removed): dates remain untouched at all levels,
        // but the recursion still produces a correctly structured (deep-copied) map.
        assertEquals("2022-01-03T00:00:00Z", result.get("date"), "Top level date should remain unconverted");
        assertEquals(
                "2022-01-01T00:00:00Z",
                ((Map<?, ?>) result.get("nested")).get("nestedDate"),
                "Nested date should remain unconverted");

        List<?> resultList = (List<?>) result.get("list");
        assertEquals(
                "2022-01-02T00:00:00Z",
                ((Map<?, ?>) resultList.get(0)).get("itemDate"),
                "Date in list item should remain unconverted");
    }

    // Test for logical type INT conversions
    @Test
    void testConvertJsonToAvroWithIntDateLogicalType() {
        // Create a schema with date logical type
        Schema dateSchema = LogicalTypes.date().addToSchema(Schema.create(Schema.Type.INT));
        JsonElement jsonElement = new JsonPrimitive("2022-01-01");

        Object result = AvroUtils.convertJsonToAvro(jsonElement, dateSchema);

        assertInstanceOf(Integer.class, result, "Result should be an Integer");
        LocalDate date = LocalDate.parse("2022-01-01");
        assertEquals((int) date.toEpochDay(), result, "Should convert to days since epoch");
    }

    // Test the exception path for date conversion
    @Test
    void testConvertJsonToAvroWithInvalidDateFormat() {
        Schema dateSchema = LogicalTypes.date().addToSchema(Schema.create(Schema.Type.INT));
        JsonElement jsonElement = new JsonPrimitive("not-a-date");

        assertThrows(
                RuntimeException.class,
                () -> AvroUtils.convertJsonToAvro(jsonElement, dateSchema),
                "Should throw RuntimeException for invalid date format");
    }

    // Test for logical type TIME_MILLIS conversions
    @Test
    void testConvertJsonToAvroWithTimeMillisLogicalType() {
        Schema timeSchema = LogicalTypes.timeMillis().addToSchema(Schema.create(Schema.Type.INT));
        JsonElement jsonElement = new JsonPrimitive("12:30:45");

        Object result = AvroUtils.convertJsonToAvro(jsonElement, timeSchema);

        assertInstanceOf(Integer.class, result, "Result should be an Integer");
        LocalTime time = LocalTime.parse("12:30:45");
        assertEquals((int) (time.toNanoOfDay() / 1_000_000), result, "Should convert to milliseconds since midnight");
    }

    // Test the exception path for time conversion
    @Test
    void testConvertJsonToAvroWithInvalidTimeFormat() {
        Schema timeSchema = LogicalTypes.timeMillis().addToSchema(Schema.create(Schema.Type.INT));
        JsonElement jsonElement = new JsonPrimitive("not-a-time");

        assertThrows(
                RuntimeException.class,
                () -> AvroUtils.convertJsonToAvro(jsonElement, timeSchema),
                "Should throw RuntimeException for invalid time format");
    }

    // Test edge cases for convertDateStringToDateInt
    @Test
    void testConvertDateStringToDateIntWithInvalidFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AvroUtils.convertDateStringToDateInt("not-a-date"),
                "Should throw IllegalArgumentException for invalid date format");
    }

    // Test edge cases for convertTimeStringToTimeMillis
    @Test
    void testConvertTimeStringToTimeMillisWithInvalidFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AvroUtils.convertTimeStringToTimeMillis("not-a-time"),
                "Should throw IllegalArgumentException for invalid time format");
    }

    // Test deepEquals with excludedKeys containing nested keys
    @Test
    void testDeepEqualsWithNestedExcludedKeys() {
        Map<String, Object> nestedMap1 = new HashMap<>();
        nestedMap1.put("nestedKey", "value1");

        Map<String, Object> nestedMap2 = new HashMap<>();
        nestedMap2.put("nestedKey", "value2"); // Different value

        Map<String, Object> map1 = new HashMap<>();
        map1.put("key1", "value1");
        map1.put("nested", nestedMap1);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("key1", "value1");
        map2.put("nested", nestedMap2);

        List<String> excludedKeys = Arrays.asList("nested");

        assertTrue(
                AvroUtils.deepEquals(map1, map2, excludedKeys),
                "Should be equal when nested object with different values is excluded");
    }

    // Test for convertDateStringToTimestampMicros with edge cases
    @Test
    void testConvertDateStringToTimestampMicrosWithVariousFormats() throws ParseException {
        // Test with microseconds
        String dateWithMicros = "2022-01-01T00:00:00.123456Z";
        long resultWithMicros = AvroUtils.convertDateStringToTimestampMicros(dateWithMicros);

        // Calculate expected: seconds * 10^6 + nanos / 10^3
        Instant instant = Instant.parse(dateWithMicros);
        long expected = instant.getEpochSecond() * 1_000_000 + instant.getNano() / 1_000;

        assertEquals(expected, resultWithMicros, "Should correctly convert timestamp with microseconds");

        // Test without microseconds
        String dateWithoutMicros = "2022-01-01T00:00:00Z";
        long resultWithoutMicros = AvroUtils.convertDateStringToTimestampMicros(dateWithoutMicros);

        Instant instantWithoutMicros = Instant.parse(dateWithoutMicros);
        long expectedWithoutMicros =
                instantWithoutMicros.getEpochSecond() * 1_000_000 + instantWithoutMicros.getNano() / 1_000;

        assertEquals(
                expectedWithoutMicros, resultWithoutMicros, "Should correctly convert timestamp without microseconds");
    }

    // Test for convertJsonToAvro with timestamp-micros logical type
    @Test
    void testConvertJsonToAvroWithTimestampMicrosLogicalType() {
        Schema schema = LogicalTypes.timestampMicros().addToSchema(Schema.create(Schema.Type.LONG));
        JsonElement jsonElement = new JsonPrimitive("2022-01-01T00:00:00.123456Z");

        Object result = AvroUtils.convertJsonToAvro(jsonElement, schema);

        assertInstanceOf(Long.class, result, "Result should be a Long");

        // Calculate expected microseconds manually to verify
        Instant instant = Instant.parse("2022-01-01T00:00:00.123456Z");
        long expected = instant.getEpochSecond() * 1_000_000 + instant.getNano() / 1_000;

        assertEquals(expected, result, "Should convert to microseconds since epoch");
    }

    // Test exception in logical type timestamp-micros conversion
    @Test
    void testConvertJsonToAvroWithInvalidTimestampMicros() {
        Schema schema = LogicalTypes.timestampMicros().addToSchema(Schema.create(Schema.Type.LONG));
        JsonElement jsonElement = new JsonPrimitive("not-a-timestamp");

        assertThrows(
                RuntimeException.class,
                () -> AvroUtils.convertJsonToAvro(jsonElement, schema),
                "Should throw RuntimeException for invalid timestamp format");
    }

    // Additional tests for corner cases in deepEquals
    @Test
    void testDeepEqualsWithEmptyMaps() {
        Map<String, Object> map1 = new HashMap<>();
        Map<String, Object> map2 = new HashMap<>();

        assertTrue(AvroUtils.deepEquals(map1, map2), "Empty maps should be equal");
    }

    @Test
    void testDeepEqualsWithNestedNullValues() {
        Map<String, Object> nestedMap1 = new HashMap<>();
        nestedMap1.put("nullValue", null);

        Map<String, Object> nestedMap2 = new HashMap<>();
        nestedMap2.put("nullValue", null);

        Map<String, Object> map1 = new HashMap<>();
        map1.put("nested", nestedMap1);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("nested", nestedMap2);

        assertTrue(AvroUtils.deepEquals(map1, map2), "Maps with nested null values should be equal");
    }

    // Test for getHeadersMap with different scenarios
    @Test
    void testGetHeadersMapWithNonStringValues() {
        JsonObject headers = new JsonObject();
        headers.addProperty("numericHeader", 123);
        headers.addProperty("booleanHeader", true);

        JsonObject json = new JsonObject();
        json.add("headers", headers);

        String jsonString = json.toString();

        Map<String, String> result = AvroUtils.getHeadersMap(jsonString);

        assertNotNull(result, "Result should not be null");
        assertEquals("123", result.get("numericHeader"), "Numeric header should be converted to string");
        assertEquals("true", result.get("booleanHeader"), "Boolean header should be converted to string");
    }

    @Test
    void testDeepEqualsWithEqualLists() {
        String record1 = "{ \"items\": [ { \"id\": 1, \"value\": \"a\" }, { \"id\": 2, \"value\": \"b\" } ] }";
        String record2 = "{ \"items\": [ { \"id\": 1, \"value\": \"a\" }, { \"id\": 2, \"value\": \"b\" } ] }";
        assertTrue(AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(record1, record2, Collections.emptyList()));
    }

    @Test
    void testDeepEqualsWithDifferentLists() {
        String record1 = "{ \"items\": [ { \"id\": 1, \"value\": \"a\" }, { \"id\": 2, \"value\": \"b\" } ] }";
        String record2 = "{ \"items\": [ { \"id\": 1, \"value\": \"a\" }, { \"id\": 2, \"value\": \"c\" } ] }";
        assertFalse(AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(record1, record2, Collections.emptyList()));
    }

    @Test
    void testDeepEqualsWithExcludedKeyInList() {
        String record1 =
                "{ \"items\": [ { \"id\": 1, \"value\": \"a\", \"extra\": \"x\" }, { \"id\": 2, \"value\": \"b\", \"extra\": \"y\" } ] }";
        String record2 =
                "{ \"items\": [ { \"id\": 1, \"value\": \"a\", \"extra\": \"z\" }, { \"id\": 2, \"value\": \"b\", \"extra\": \"w\" } ] }";
        assertTrue(AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(record1, record2, List.of("extra")));
    }

    @Test
    void testDeepEqualsWithDifferentListSizes() {
        String record1 = "{ \"items\": [ { \"id\": 1, \"value\": \"a\" } ] }";
        String record2 = "{ \"items\": [ { \"id\": 1, \"value\": \"a\" }, { \"id\": 2, \"value\": \"b\" } ] }";
        assertFalse(AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(record1, record2, Collections.emptyList()));
    }

    @Test
    void testPrivateConstructorThrowsException() throws Exception {
        // Test the private constructor to get 100% method coverage
        Constructor<AvroUtils> constructor = AvroUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThrows(InvocationTargetException.class, constructor::newInstance);

        try {
            constructor.newInstance();
        } catch (InvocationTargetException e) {
            assertInstanceOf(UnsupportedOperationException.class, e.getCause());
            assertEquals("Utility class cannot be instantiated", e.getCause().getMessage());
        }
    }

    @Nested
    @DisplayName("Smart Array Comparison - Unordered Object Arrays")
    class UnorderedObjectArrayTests {

        @Test
        @DisplayName("Should match arrays with same objects in different order")
        void shouldMatchArraysWithSameObjectsInDifferentOrder() {
            String actual = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 1,
                            "CAI": "123861",
                            "orderLineQuantity": 6.0
                        },
                        {
                            "orderLineNumber": 3,
                            "alLineNumber": 2,
                            "CAI": "123871",
                            "orderLineQuantity": 30.0
                        },
                        {
                            "orderLineNumber": 4,
                            "alLineNumber": 3,
                            "CAI": "899613",
                            "orderLineQuantity": 10.0
                        }
                    ]
                }
                """;

            String expected = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 3,
                            "CAI": "123861",
                            "orderLineQuantity": 6.0
                        },
                        {
                            "orderLineNumber": 3,
                            "alLineNumber": 2,
                            "CAI": "123871",
                            "orderLineQuantity": 30.0
                        },
                        {
                            "orderLineNumber": 4,
                            "alLineNumber": 1,
                            "CAI": "899613",
                            "orderLineQuantity": 10.0
                        }
                    ]
                }
                """;

            boolean result =
                    AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(expected, actual, List.of("alLineNumber"));

            assertTrue(result, "Arrays with same objects in different order should match when excluding alLineNumber");
        }

        @Test
        @DisplayName("Should not match arrays when objects have different content")
        void shouldNotMatchArraysWithDifferentContent() {
            String actual = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 1,
                            "CAI": "123861",
                            "orderLineQuantity": 6.0
                        },
                        {
                            "orderLineNumber": 3,
                            "alLineNumber": 2,
                            "CAI": "123871",
                            "orderLineQuantity": 30.0
                        }
                    ]
                }
                """;

            String expected = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 1,
                            "CAI": "DIFFERENT",
                            "orderLineQuantity": 6.0
                        },
                        {
                            "orderLineNumber": 3,
                            "alLineNumber": 2,
                            "CAI": "123871",
                            "orderLineQuantity": 30.0
                        }
                    ]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);

            assertFalse(result, "Arrays with different object content should not match");
        }

        @Test
        @DisplayName("Should match arrays with nested objects in different order")
        void shouldMatchArraysWithNestedObjectsInDifferentOrder() {
            String actual = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 1,
                            "instructions": [
                                {
                                    "code": "LC55",
                                    "parameters": "3M5"
                                },
                                {
                                    "code": "DM62",
                                    "parameters": "OENA"
                                }
                            ]
                        },
                        {
                            "orderLineNumber": 2,
                            "alLineNumber": 2,
                            "instructions": [
                                {
                                    "code": "DC05",
                                    "parameters": "TEST"
                                }
                            ]
                        }
                    ]
                }
                """;

            String expected = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 2,
                            "alLineNumber": 2,
                            "instructions": [
                                {
                                    "code": "DC05",
                                    "parameters": "TEST"
                                }
                            ]
                        },
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 1,
                            "instructions": [
                                {
                                    "code": "DM62",
                                    "parameters": "OENA"
                                },
                                {
                                    "code": "LC55",
                                    "parameters": "3M5"
                                }
                            ]
                        }
                    ]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);

            assertTrue(result, "Arrays with nested objects should match regardless of order");
        }

        @Test
        @DisplayName("Should not match arrays with different sizes")
        void shouldNotMatchArraysWithDifferentSizes() {
            String actual = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 1,
                            "CAI": "123861"
                        },
                        {
                            "orderLineNumber": 2,
                            "alLineNumber": 2,
                            "CAI": "123871"
                        }
                    ]
                }
                """;

            String expected = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 1,
                            "CAI": "123861"
                        },
                        {
                            "orderLineNumber": 2,
                            "alLineNumber": 2,
                            "CAI": "123871"
                        },
                        {
                            "orderLineNumber": 3,
                            "alLineNumber": 3,
                            "CAI": "999999"
                        }
                    ]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);

            assertFalse(result, "Arrays with different sizes should not match");
        }

        @Test
        @DisplayName("Should match empty arrays")
        void shouldMatchEmptyArrays() {
            String actual = """
                {
                    "alLines": []
                }
                """;

            String expected = """
                {
                    "alLines": []
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);

            assertTrue(result, "Empty arrays should match");
        }

        @Test
        @DisplayName("Should handle complex real-world scenario from main method")
        void shouldHandleComplexRealWorldScenario() {
            String actual = """
                 {
                  "destinationMailBox": "U2",
                  "originMailBoxCode": "APS",
                  "creationDate": 1757471752575,
                  "messageType": "ORDLIV",
                  "alMessages": [
                    {
                      "alNumber": "350000",
                      "alNumberFull": "CAND350000",
                      "creationDate": 1757471752570,
                      "alLines": [
                        {
                          "orderLineNumber": 1,
                          "alLineNumber": 1,
                          "CAI": "123861",
                          "orderLineQuantity": 6.0000
                        },
                        {
                          "orderLineNumber": 3,
                          "alLineNumber": 2,
                          "CAI": "123871",
                          "orderLineQuantity": 30.0000
                        },
                        {
                          "orderLineNumber": 4,
                          "alLineNumber": 3,
                          "CAI": "899613",
                          "orderLineQuantity": 10.0000
                        }
                      ]
                    }
                  ]
                }
                """;

            String expected = """
                 {
                  "destinationMailBox": "U2",
                  "originMailBoxCode": "APS",
                  "creationDate": 1754801123022,
                  "messageType": "ORDLIV",
                  "alMessages": [
                    {
                      "alNumber": "350001",
                      "alNumberFull": "CAND350001",
                      "creationDate": 1754801123022,
                      "alLines": [
                        {
                          "orderLineNumber": 1,
                          "alLineNumber": 3,
                          "CAI": "123861",
                          "orderLineQuantity": 6.0
                        },
                        {
                          "orderLineNumber": 3,
                          "alLineNumber": 2,
                          "CAI": "123871",
                          "orderLineQuantity": 30.0
                        },
                        {
                          "orderLineNumber": 4,
                          "alLineNumber": 1,
                          "CAI": "899613",
                          "orderLineQuantity": 10.0
                        }
                      ]
                    }
                  ]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(
                    expected, actual, List.of("alNumberFull", "alLineNumber", "alNumber", "creationDate"));

            assertTrue(result, "Complex real-world scenario should match with exclusions");
        }
    }

    @Nested
    @DisplayName("Smart Array Comparison - Ordered Primitive Arrays")
    class OrderedPrimitiveArrayTests {

        @Test
        @DisplayName("Should use ordered comparison for primitive arrays")
        void shouldUseOrderedComparisonForPrimitiveArrays() {
            String actual = """
                {
                    "numbers": [1, 2, 3, 4, 5]
                }
                """;

            String expected = """
                {
                    "numbers": [5, 4, 3, 2, 1]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);

            assertFalse(result, "Primitive arrays should be compared in order and should not match when order differs");
        }

        @Test
        @DisplayName("Should match identical primitive arrays")
        void shouldMatchIdenticalPrimitiveArrays() {
            String actual = """
                {
                    "numbers": [1, 2, 3, 4, 5]
                }
                """;

            String expected = """
                {
                    "numbers": [1, 2, 3, 4, 5]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);

            assertTrue(result, "Identical primitive arrays should match");
        }

        @Test
        @DisplayName("Should use ordered comparison for string arrays")
        void shouldUseOrderedComparisonForStringArrays() {
            String actual = """
                {
                    "codes": ["A", "B", "C"]
                }
                """;

            String expected = """
                {
                    "codes": ["C", "B", "A"]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);

            assertFalse(result, "String arrays should be compared in order and should not match when order differs");
        }

        @Test
        @DisplayName("Should use ordered comparison for mixed type arrays")
        void shouldUseOrderedComparisonForMixedTypeArrays() {
            String actual = """
                {
                    "mixed": ["string", 123, true]
                }
                """;

            String expected = """
                {
                    "mixed": [123, "string", true]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);

            assertFalse(
                    result, "Mixed type arrays should be compared in order and should not match when order differs");
        }
    }

    @Nested
    @DisplayName("Smart Array Comparison - Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle single object arrays")
        void shouldHandleSingleObjectArrays() {
            String actual = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 1,
                            "CAI": "123861"
                        }
                    ]
                }
                """;

            String expected = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 1,
                            "CAI": "123861"
                        }
                    ]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);

            assertTrue(result, "Single object arrays should match");
        }

        @Test
        @DisplayName("Should handle arrays with null values")
        void shouldHandleArraysWithNullValues() {
            String actual = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "nullField": null,
                            "CAI": "123861"
                        },
                        {
                            "orderLineNumber": 2,
                            "nullField": null,
                            "CAI": "123871"
                        }
                    ]
                }
                """;

            String expected = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 2,
                            "nullField": null,
                            "CAI": "123871"
                        },
                        {
                            "orderLineNumber": 1,
                            "nullField": null,
                            "CAI": "123861"
                        }
                    ]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);

            assertTrue(result, "Arrays with null values should match when objects are equivalent regardless of order");
        }

        @Test
        @DisplayName("Should not match when one object is missing a matching counterpart")
        void shouldNotMatchWhenObjectIsMissingCounterpart() {
            String actual = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "CAI": "123861"
                        },
                        {
                            "orderLineNumber": 2,
                            "CAI": "123871"
                        },
                        {
                            "orderLineNumber": 3,
                            "CAI": "UNIQUE_TO_ACTUAL"
                        }
                    ]
                }
                """;

            String expected = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "CAI": "123861"
                        },
                        {
                            "orderLineNumber": 2,
                            "CAI": "123871"
                        },
                        {
                            "orderLineNumber": 3,
                            "CAI": "UNIQUE_TO_EXPECTED"
                        }
                    ]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);

            assertFalse(result, "Should not match when objects don't have matching counterparts");
        }

        @Test
        @DisplayName("Should handle deeply nested object arrays")
        void shouldHandleDeeplyNestedObjectArrays() {
            String actual = """
                {
                    "level1": [
                        {
                            "id": 1,
                            "level2": [
                                {
                                    "nestedId": "A",
                                    "level3": [
                                        {"deepId": "X", "value": "test1"},
                                        {"deepId": "Y", "value": "test2"}
                                    ]
                                },
                                {
                                    "nestedId": "B",
                                    "level3": [
                                        {"deepId": "Z", "value": "test3"}
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """;

            String expected = """
                {
                    "level1": [
                        {
                            "id": 1,
                            "level2": [
                                {
                                    "nestedId": "B",
                                    "level3": [
                                        {"deepId": "Z", "value": "test3"}
                                    ]
                                },
                                {
                                    "nestedId": "A",
                                    "level3": [
                                        {"deepId": "Y", "value": "test2"},
                                        {"deepId": "X", "value": "test1"}
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);

            assertTrue(result, "Deeply nested object arrays should match regardless of order at all levels");
        }
    }

    @Nested
    @DisplayName("Smart Array Comparison - With Exclusions")
    class ExclusionTests {

        @Test
        @DisplayName("Should apply exclusions to object arrays")
        void shouldApplyExclusionsToObjectArrays() {
            String actual = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 1,
                            "timestamp": "2023-01-01T10:00:00Z",
                            "CAI": "123861"
                        },
                        {
                            "orderLineNumber": 2,
                            "alLineNumber": 2,
                            "timestamp": "2023-01-01T11:00:00Z",
                            "CAI": "123871"
                        }
                    ]
                }
                """;

            String expected = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 2,
                            "alLineNumber": 99,
                            "timestamp": "2023-01-01T12:00:00Z",
                            "CAI": "123871"
                        },
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 88,
                            "timestamp": "2023-01-01T13:00:00Z",
                            "CAI": "123861"
                        }
                    ]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(
                    expected, actual, List.of("alLineNumber", "timestamp"));

            assertTrue(result, "Should match when excluding volatile fields like timestamps and line numbers");
        }

        @Test
        @DisplayName("Should still fail when non-excluded fields differ")
        void shouldFailWhenNonExcludedFieldsDiffer() {
            String actual = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 1,
                            "timestamp": "2023-01-01T10:00:00Z",
                            "CAI": "123861"
                        }
                    ]
                }
                """;

            String expected = """
                {
                    "alLines": [
                        {
                            "orderLineNumber": 1,
                            "alLineNumber": 99,
                            "timestamp": "2023-01-01T12:00:00Z",
                            "CAI": "DIFFERENT"
                        }
                    ]
                }
                """;

            boolean result = AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(
                    expected, actual, List.of("alLineNumber", "timestamp"));

            assertFalse(result, "Should not match when non-excluded fields differ");
        }
    }

    @Nested
    @DisplayName("Smart Array Comparison - Performance and Scalability")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle larger arrays efficiently")
        void shouldHandleLargerArraysEfficiently() {
            // Build larger test data
            StringBuilder actualBuilder = new StringBuilder();
            StringBuilder expectedBuilder = new StringBuilder();

            actualBuilder.append("{\"alLines\": [");
            expectedBuilder.append("{\"alLines\": [");

            // Create 50 objects in different orders
            for (int i = 1; i <= 50; i++) {
                String objActual = String.format("{\"id\": %d, \"alLineNumber\": %d, \"value\": \"item_%d\"}", i, i, i);
                String objExpected = String.format(
                        "{\"id\": %d, \"alLineNumber\": %d, \"value\": \"item_%d\"}",
                        i, 51 - i, i // Different alLineNumber
                        );

                actualBuilder.append(objActual);
                expectedBuilder.append(objExpected);

                if (i < 50) {
                    actualBuilder.append(", ");
                    expectedBuilder.append(", ");
                }
            }

            actualBuilder.append("]}");
            expectedBuilder.append("]}");

            String actual = actualBuilder.toString();
            String expected = expectedBuilder.toString();

            long startTime = System.currentTimeMillis();
            boolean result =
                    AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(expected, actual, List.of("alLineNumber"));
            long endTime = System.currentTimeMillis();

            assertTrue(result, "Larger arrays should match when excluding alLineNumber");
            assertTrue(endTime - startTime < 5000, "Should complete within reasonable time (< 5 seconds)");
        }
    }

    // ===========================================
    // ASSERTION METHODS TESTS (THROWS EXCEPTIONS)
    // ===========================================

    @Nested
    @DisplayName("Assertion Methods That Throw ComparisonException")
    class AssertionMethodsTests {

        @Test
        @DisplayName("assertAvroRecordsStrictlyMatch - should pass when records strictly match")
        void testAssertAvroRecordsStrictlyMatch_Success() {
            String record1 = "{\"name\":\"John\", \"age\":30}";
            String record2 = "{\"name\":\"John\", \"age\":30}";

            // Should not throw exception
            assertDoesNotThrow(() -> AvroUtils.assertAvroRecordsStrictlyMatch(record1, record2));
        }

        @Test
        @DisplayName("assertAvroRecordsStrictlyMatch - should throw when records don't strictly match")
        void testAssertAvroRecordsStrictlyMatch_Failure() {
            String record1 = "{\"name\":\"John\", \"age\":30}";
            String record2 = "{\"name\":\"Jane\", \"age\":25}"; // Different values

            var exception = assertThrows(
                    ComparisonException.class, () -> AvroUtils.assertAvroRecordsStrictlyMatch(record1, record2));
            log.error(exception.getMessage());
            assertTrue(exception.getMessage().contains("Avro records do not strictly match"));
            assertTrue(exception.getMessage().contains("Expected:"));
            assertTrue(exception.getMessage().contains("Actual:"));
        }

        @Test
        @DisplayName("assertAvroRecordsSmartMatch - should pass when records semantically match")
        void testAssertAvroRecordsSmartMatch_Success() {
            String record1 = "{\"name\":\"John\", \"age\":30}";
            String record2 = "{\"age\":30, \"name\":\"John\"}"; // Different order but same content

            // Should not throw exception
            assertDoesNotThrow(() -> AvroUtils.assertAvroRecordsSmartMatch(record1, record2));
        }

        @Test
        @DisplayName("assertAvroRecordsSmartMatch - should throw when records don't match")
        void testAssertAvroRecordsSmartMatch_Failure() {
            String record1 = "{\"name\":\"John\", \"age\":30}";
            String record2 = "{\"name\":\"Jane\", \"age\":25}";

            var exception = assertThrows(
                    ComparisonException.class, () -> AvroUtils.assertAvroRecordsSmartMatch(record1, record2));

            assertTrue(exception.getMessage().contains("Avro records do not match"));
            assertTrue(exception.getMessage().contains("Expected:"));
            assertTrue(exception.getMessage().contains("Actual:"));
        }

        @Test
        @DisplayName("assertAvroRecordsSmartMatch - should show pretty-printed JSON in error")
        void testAssertAvroRecordsSmartMatch_PrettyPrintedError() {
            String record1 = "{\"name\":\"John\",\"age\":30}";
            String record2 = "{\"name\":\"Jane\",\"age\":25}";

            var exception = assertThrows(
                    ComparisonException.class, () -> AvroUtils.assertAvroRecordsSmartMatch(record1, record2));

            // Should contain pretty-printed JSON (with newlines and indentation)
            assertTrue(exception.getMessage().contains("\n"));
        }

        @Test
        @DisplayName("assertAvroRecordsSmartMatchWithExclusions - should pass excluding specified keys")
        void testAssertAvroRecordsSmartMatchWithExclusions_Success() {
            String record1 = "{\"name\":\"John\", \"age\":30, \"timestamp\":\"2026-01-01T10:00:00Z\"}";
            String record2 = "{\"name\":\"John\", \"age\":30, \"timestamp\":\"2026-02-22T15:30:00Z\"}";

            List<String> excludedKeys = List.of("timestamp");

            // Should not throw exception when excluding timestamp
            assertDoesNotThrow(
                    () -> AvroUtils.assertAvroRecordsSmartMatchWithExclusions(record1, record2, excludedKeys));
        }

        @Test
        @DisplayName("assertAvroRecordsSmartMatchWithExclusions - should throw when non-excluded keys don't match")
        void testAssertAvroRecordsSmartMatchWithExclusions_Failure() {
            String record1 = "{\"name\":\"John\", \"age\":30, \"timestamp\":\"2026-01-01T10:00:00Z\"}";
            String record2 = "{\"name\":\"Jane\", \"age\":25, \"timestamp\":\"2026-02-22T15:30:00Z\"}";

            List<String> excludedKeys = List.of("timestamp");

            var exception = assertThrows(
                    ComparisonException.class,
                    () -> AvroUtils.assertAvroRecordsSmartMatchWithExclusions(record1, record2, excludedKeys));

            assertTrue(exception.getMessage().contains("Avro records do not match"));
            assertTrue(exception.getMessage().contains("with exclusions: [timestamp]"));
        }

        @Test
        @DisplayName("assertAvroRecordsSmartMatchWithExclusions - should work with multiple exclusions")
        void testAssertAvroRecordsSmartMatchWithExclusions_MultipleExclusions() {
            String record1 = "{\"name\":\"John\", \"age\":30, \"id\":\"123\", \"timestamp\":\"2026-01-01T10:00:00Z\"}";
            String record2 = "{\"name\":\"John\", \"age\":30, \"id\":\"456\", \"timestamp\":\"2026-02-22T15:30:00Z\"}";

            List<String> excludedKeys = List.of("timestamp", "id");

            // Should not throw exception when excluding multiple keys
            assertDoesNotThrow(
                    () -> AvroUtils.assertAvroRecordsSmartMatchWithExclusions(record1, record2, excludedKeys));
        }

        @Test
        @DisplayName("assertDeepEquals - should pass when maps are deeply equal")
        void testAssertDeepEquals_Success() {
            Map<String, Object> map1 = new HashMap<>();
            map1.put("name", "John");
            map1.put("age", 30.0);

            Map<String, Object> map2 = new HashMap<>();
            map2.put("name", "John");
            map2.put("age", 30.0);

            // Should not throw exception
            assertDoesNotThrow(() -> AvroUtils.assertDeepEquals(map1, map2));
        }

        @Test
        @DisplayName("assertDeepEquals - should throw when maps are not equal")
        void testAssertDeepEquals_Failure() {
            Map<String, Object> map1 = new HashMap<>();
            map1.put("name", "John");
            map1.put("age", 30.0);

            Map<String, Object> map2 = new HashMap<>();
            map2.put("name", "Jane");
            map2.put("age", 25.0);

            var exception = assertThrows(ComparisonException.class, () -> AvroUtils.assertDeepEquals(map1, map2));

            assertTrue(exception.getMessage().contains("Avro value maps do not match"));
            assertTrue(exception.getMessage().contains("Expected:"));
            assertTrue(exception.getMessage().contains("Actual:"));
        }

        @Test
        @DisplayName("assertDeepEquals - should handle nested maps")
        void testAssertDeepEquals_NestedMaps() {
            Map<String, Object> nested1 = new HashMap<>();
            nested1.put("city", "NYC");
            Map<String, Object> map1 = new HashMap<>();
            map1.put("name", "John");
            map1.put("address", nested1);

            Map<String, Object> nested2 = new HashMap<>();
            nested2.put("city", "NYC");
            Map<String, Object> map2 = new HashMap<>();
            map2.put("name", "John");
            map2.put("address", nested2);

            // Should not throw exception for equal nested maps
            assertDoesNotThrow(() -> AvroUtils.assertDeepEquals(map1, map2));
        }

        @Test
        @DisplayName("assertDeepEquals with exclusions - should pass excluding specified keys")
        void testAssertDeepEqualsWithExclusions_Success() {
            Map<String, Object> map1 = new HashMap<>();
            map1.put("name", "John");
            map1.put("age", 30.0);
            map1.put("id", "123");

            Map<String, Object> map2 = new HashMap<>();
            map2.put("name", "John");
            map2.put("age", 30.0);
            map2.put("id", "456"); // Different ID

            List<String> excludedKeys = List.of("id");

            // Should not throw exception when excluding id
            assertDoesNotThrow(() -> AvroUtils.assertDeepEquals(map1, map2, excludedKeys));
        }

        @Test
        @DisplayName("assertDeepEquals with exclusions - should throw when non-excluded keys differ")
        void testAssertDeepEqualsWithExclusions_Failure() {
            Map<String, Object> map1 = new HashMap<>();
            map1.put("name", "John");
            map1.put("age", 30.0);
            map1.put("id", "123");

            Map<String, Object> map2 = new HashMap<>();
            map2.put("name", "Jane"); // Different name
            map2.put("age", 25.0);
            map2.put("id", "456");

            List<String> excludedKeys = List.of("id");

            var exception =
                    assertThrows(ComparisonException.class, () -> AvroUtils.assertDeepEquals(map1, map2, excludedKeys));

            assertTrue(exception.getMessage().contains("Avro value maps do not match"));
            assertTrue(exception.getMessage().contains("with exclusions: [id]"));
        }

        @Test
        @DisplayName("assertAvroValueFromKeyMatches - should pass when key value matches")
        void testAssertAvroValueFromKeyMatches_Success() {
            String expected = "{\"name\":\"John\", \"age\":30}";
            String actual = "{\"name\":\"John\", \"age\":30}";

            // Should not throw exception
            assertDoesNotThrow(() -> AvroUtils.assertAvroValueFromKeyMatches("name", expected, actual));
        }

        @Test
        @DisplayName("assertAvroValueFromKeyMatches - should throw when key value doesn't match")
        void testAssertAvroValueFromKeyMatches_Failure() {
            String expected = "{\"name\":\"John\", \"age\":30}";
            String actual = "{\"name\":\"Jane\", \"age\":30}";

            var exception = assertThrows(
                    ComparisonException.class, () -> AvroUtils.assertAvroValueFromKeyMatches("name", expected, actual));

            assertTrue(exception.getMessage().contains("Avro value for key 'name' does not match"));
            assertTrue(exception.getMessage().contains("Expected: John"));
            assertTrue(exception.getMessage().contains("Actual: Jane"));
        }

        @Test
        @DisplayName("assertAvroValueFromKeyMatches - should support nested keys")
        void testAssertAvroValueFromKeyMatches_NestedKey() {
            String expected = "{\"person\":{\"name\":\"John\", \"age\":30}}";
            String actual = "{\"person\":{\"name\":\"John\", \"age\":30}}";

            // Should not throw exception for matching nested key
            assertDoesNotThrow(() -> AvroUtils.assertAvroValueFromKeyMatches("person.name", expected, actual));
        }

        @Test
        @DisplayName("assertAvroValueFromKeyMatches - should throw for mismatched nested keys")
        void testAssertAvroValueFromKeyMatches_NestedKeyMismatch() {
            String expected = "{\"person\":{\"name\":\"John\", \"age\":30}}";
            String actual = "{\"person\":{\"name\":\"Jane\", \"age\":25}}";

            var exception = assertThrows(
                    ComparisonException.class,
                    () -> AvroUtils.assertAvroValueFromKeyMatches("person.name", expected, actual));

            assertTrue(exception.getMessage().contains("Avro value for key 'person.name' does not match"));
        }

        @Test
        @DisplayName("assertAvroValueFromKeyMatches - should support array indexing")
        void testAssertAvroValueFromKeyMatches_ArrayIndex() {
            String expected = "{\"users\":[{\"name\":\"John\"},{\"name\":\"Jane\"}]}";
            String actual = "{\"users\":[{\"name\":\"John\"},{\"name\":\"Jane\"}]}";

            // Should not throw exception for matching array element
            assertDoesNotThrow(() -> AvroUtils.assertAvroValueFromKeyMatches("users[0].name", expected, actual));
        }

        @Test
        @DisplayName("assertAvroValueFromKeyMatches - should throw for mismatched array elements")
        void testAssertAvroValueFromKeyMatches_ArrayIndexMismatch() {
            String expected = "{\"users\":[{\"name\":\"John\"},{\"name\":\"Jane\"}]}";
            String actual = "{\"users\":[{\"name\":\"Bob\"},{\"name\":\"Jane\"}]}";

            var exception = assertThrows(
                    ComparisonException.class,
                    () -> AvroUtils.assertAvroValueFromKeyMatches("users[0].name", expected, actual));

            assertTrue(exception.getMessage().contains("Avro value for key 'users[0].name' does not match"));
        }

        @Test
        @DisplayName("assertAvroRecordsSmartMatch - should handle complex record with multiple fields")
        void testAssertAvroRecordsSmartMatch_ComplexRecord() {
            String record1 = "{\"id\":\"001\",\"value\":72,\"status\":\"active\",\"metadata\":\"test\"}";
            String record2 = "{\"id\":\"001\",\"value\":72,\"status\":\"active\",\"metadata\":\"test\"}";

            assertDoesNotThrow(() -> AvroUtils.assertAvroRecordsSmartMatch(record1, record2));
        }

        @Test
        @DisplayName("assertAvroRecordsSmartMatch - should detect numeric value mismatch")
        void testAssertAvroRecordsSmartMatch_NumericMismatch() {
            String record1 = "{\"id\":\"device-42\",\"measurement\":95,\"status\":\"active\"}";
            String record2 = "{\"id\":\"device-42\",\"measurement\":85,\"status\":\"active\"}";

            var exception = assertThrows(
                    ComparisonException.class, () -> AvroUtils.assertAvroRecordsSmartMatch(record1, record2));

            assertTrue(exception.getMessage().contains("Avro records do not match"));
        }

        @Test
        @DisplayName("assertAvroRecordsSmartMatchWithExclusions - should ignore timestamp in comparison")
        void testAssertAvroRecordsSmartMatchWithExclusions_IgnoreTimestamp() {
            String expected = "{\"id\":\"123\",\"value\":72,\"timestamp\":\"2026-01-01T10:00:00Z\"}";
            String actual = "{\"id\":\"123\",\"value\":72,\"timestamp\":\"2026-02-22T15:30:00Z\"}";

            List<String> excludedKeys = List.of("timestamp");

            assertDoesNotThrow(
                    () -> AvroUtils.assertAvroRecordsSmartMatchWithExclusions(expected, actual, excludedKeys));
        }

        @Test
        @DisplayName("assertAvroValueFromKeyMatches - should validate deeply nested value")
        void testAssertAvroValueFromKeyMatches_DeeplyNested() {
            String record1 = "{\"data\":{\"id\":\"12345\",\"metrics\":{\"value\":72,\"unit\":\"bpm\"}}}";
            String record2 = "{\"data\":{\"id\":\"12345\",\"metrics\":{\"value\":72,\"unit\":\"bpm\"}}}";

            assertDoesNotThrow(() -> AvroUtils.assertAvroValueFromKeyMatches("data.metrics.value", record1, record2));
        }

        @Test
        @DisplayName("assertAvroValueFromKeyMatches - should detect deeply nested value mismatch")
        void testAssertAvroValueFromKeyMatches_DeeplyNestedMismatch() {
            String record1 = "{\"data\":{\"id\":\"12345\",\"metrics\":{\"value\":85,\"unit\":\"bpm\"}}}";
            String record2 = "{\"data\":{\"id\":\"12345\",\"metrics\":{\"value\":72,\"unit\":\"bpm\"}}}";

            var exception = assertThrows(
                    ComparisonException.class,
                    () -> AvroUtils.assertAvroValueFromKeyMatches("data.metrics.value", record1, record2));

            assertTrue(exception.getMessage().contains("Avro value for key 'data.metrics.value' does not match"));
            assertTrue(exception.getMessage().contains("Expected: 85"));
            assertTrue(exception.getMessage().contains("Actual: 72"));
        }
    }

    @Nested
    @DisplayName("performDeepEqualsComparison - Effective Size Check With Excluded Keys")
    class EffectiveSizeCheckWithExcludedKeysTests {

        @Test
        @DisplayName("Should return true when expected has an extra key that is excluded (core bug regression)")
        void shouldReturnTrue_WhenExpectedHasExtraExcludedKey() {
            // expected has key3 which is excluded: old code failed here (size 3 != 2)
            String expected = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
            String actual = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
            List<String> excludedKeys = List.of("key3");

            assertTrue(
                    AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(expected, actual, excludedKeys),
                    "Expected map has an extra key that is excluded: effective sizes are equal, should match");
        }

        @Test
        @DisplayName("Should return true when actual has an extra key that is excluded")
        void shouldReturnTrue_WhenActualHasExtraExcludedKey() {
            // Symmetric case: the extra key lives in actual instead
            String expected = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
            String actual = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
            List<String> excludedKeys = List.of("key3");

            assertTrue(
                    AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(expected, actual, excludedKeys),
                    "Actual map has an extra key that is excluded: effective sizes are equal, should match");
        }

        @Test
        @DisplayName("Should return true when each map has a distinct extra excluded key")
        void shouldReturnTrue_WhenEachMapHasDistinctExtraExcludedKey() {
            // expected has excludedA (size 3), actual has excludedB (size 3)
            // Both effective sizes = 2 after subtracting their own excluded key
            String expected = "{\"key1\":\"value1\",\"key2\":\"value2\",\"excludedA\":\"foo\"}";
            String actual = "{\"key1\":\"value1\",\"key2\":\"value2\",\"excludedB\":\"bar\"}";
            List<String> excludedKeys = List.of("excludedA", "excludedB");

            assertTrue(
                    AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(expected, actual, excludedKeys),
                    "Each map carries a different extra excluded key: effective sizes are equal, should match");
        }

        @Test
        @DisplayName("Should return false when effective sizes still differ after accounting for excluded keys")
        void shouldReturnFalse_WhenEffectiveSizesStillDifferAfterExclusion() {
            // expected has key2 + key3 (key3 excluded), actual has only key1: effective 2 vs 1
            String expected = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
            String actual = "{\"key1\":\"value1\"}";
            List<String> excludedKeys = List.of("key3");

            assertFalse(
                    AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(expected, actual, excludedKeys),
                    "Even after excluding key3, expected still has more non-excluded keys than actual: should not match");
        }

        @Test
        @DisplayName("Should not change effective size when excluded key is absent from both maps")
        void shouldNotAffectEffectiveSize_WhenExcludedKeyAbsentFromBothMaps() {
            // The exclusion list contains a key that exists in neither map
            String expected = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
            String actual = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
            List<String> excludedKeys = List.of("phantomKey");

            assertTrue(
                    AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(expected, actual, excludedKeys),
                    "An excluded key absent from both maps must not distort effective sizes: equal maps should still match");
        }

        @Test
        @DisplayName("Should return true when multiple extra excluded keys are present only in expected")
        void shouldReturnTrue_WhenMultipleExtraExcludedKeysOnlyInExpected() {
            // expected has 4 keys, two of which are excluded; actual has 2 non-excluded keys
            String expected = "{\"key1\":\"value1\",\"key2\":\"value2\",\"excl1\":\"x\",\"excl2\":\"y\"}";
            String actual = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
            List<String> excludedKeys = List.of("excl1", "excl2");

            assertTrue(
                    AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(expected, actual, excludedKeys),
                    "Multiple extra excluded keys only in expected: effective sizes both 2, should match");
        }
    }

    @Nested
    @DisplayName("Type-driven date comparison")
    class TypeDrivenDateComparisonTests {

        @Test
        @DisplayName("Should compare literally when both sides are plain strings, even if date-like")
        void shouldCompareLiterally_WhenBothSidesAreDateLikeStrings() {
            // A genuine Avro `string` field whose content happens to look like a date/timestamp must never be
            // converted or reinterpreted: it must be compared as a literal string on both sides.
            Map<String, Object> expected = new HashMap<>();
            expected.put("MyDate", "2026-02-10T16:19:14.123Z");

            Map<String, Object> actual = new HashMap<>();
            actual.put("MyDate", "2026-02-10T16:19:14.123Z");

            assertTrue(AvroUtils.deepEquals(expected, actual));
        }

        @Test
        @DisplayName("Should not match date-like strings that differ, comparing them literally")
        void shouldNotMatch_WhenDateLikeStringsDifferLiterally() {
            Map<String, Object> expected = new HashMap<>();
            expected.put("MyDate", "2026-02-10T16:19:14.000000Z");

            Map<String, Object> actual = new HashMap<>();
            actual.put("MyDate", "2026-02-10T16:19:14.123Z");

            assertFalse(AvroUtils.deepEquals(expected, actual));
        }

        @Test
        @DisplayName(
                "Should match an actual Instant (timestamp-micros) against an expected microsecond-precision string")
        void shouldMatch_ActualInstant_vs_ExpectedMicrosecondString() {
            Instant actualInstant = Instant.parse("2026-02-10T16:19:14.000000Z");

            Map<String, Object> expected = new HashMap<>();
            expected.put("timestamp", "2026-02-10T16:19:14.000000Z");

            Map<String, Object> actual = new HashMap<>();
            actual.put("timestamp", actualInstant);

            assertTrue(AvroUtils.deepEquals(expected, actual));
        }

        @Test
        @DisplayName("Should match an actual Instant against an expected millisecond-precision string")
        void shouldMatch_ActualInstant_vs_ExpectedMillisecondString() {
            Instant actualInstant = Instant.parse("2026-02-10T16:19:14.123Z");

            Map<String, Object> expected = new HashMap<>();
            expected.put("timestamp", "2026-02-10T16:19:14.123Z");

            Map<String, Object> actual = new HashMap<>();
            actual.put("timestamp", actualInstant);

            assertTrue(AvroUtils.deepEquals(expected, actual));
        }

        @Test
        @DisplayName("Should match an actual Instant against an expected string with no fractional seconds")
        void shouldMatch_ActualInstant_vs_ExpectedStringWithNoFraction() {
            Instant actualInstant = Instant.parse("2026-02-10T16:19:14Z");

            Map<String, Object> expected = new HashMap<>();
            expected.put("timestamp", "2026-02-10T16:19:14Z");

            Map<String, Object> actual = new HashMap<>();
            actual.put("timestamp", actualInstant);

            assertTrue(AvroUtils.deepEquals(expected, actual));
        }

        @Test
        @DisplayName("Should match an actual LocalDateTime against an expected local-timestamp string")
        void shouldMatch_ActualLocalDateTime_vs_ExpectedString() {
            LocalDateTime actualLocalDateTime = LocalDateTime.parse("2026-02-10T16:19:14.123456");

            Map<String, Object> expected = new HashMap<>();
            expected.put("localTimestamp", "2026-02-10T16:19:14.123456");

            Map<String, Object> actual = new HashMap<>();
            actual.put("localTimestamp", actualLocalDateTime);

            assertTrue(AvroUtils.deepEquals(expected, actual));
        }

        @Test
        @DisplayName("Should match an actual LocalDate against an expected yyyy-MM-dd string")
        void shouldMatch_ActualLocalDate_vs_ExpectedString() {
            LocalDate actualLocalDate = LocalDate.parse("2026-02-10");

            Map<String, Object> expected = new HashMap<>();
            expected.put("birthDate", "2026-02-10");

            Map<String, Object> actual = new HashMap<>();
            actual.put("birthDate", actualLocalDate);

            assertTrue(AvroUtils.deepEquals(expected, actual));
        }

        @Test
        @DisplayName("Should not match an actual LocalDate against a mismatching expected string")
        void shouldNotMatch_ActualLocalDate_vs_MismatchingExpectedString() {
            LocalDate actualLocalDate = LocalDate.parse("2026-02-10");

            Map<String, Object> expected = new HashMap<>();
            expected.put("birthDate", "2026-02-11");

            Map<String, Object> actual = new HashMap<>();
            actual.put("birthDate", actualLocalDate);

            assertFalse(AvroUtils.deepEquals(expected, actual));
        }

        @Test
        @DisplayName("Should apply type-driven date comparison inside nested objects")
        void shouldMatch_NestedObjectWithDateField() {
            Instant actualInstant = Instant.parse("2026-02-10T16:19:14.123456Z");

            Map<String, Object> nestedExpected = new HashMap<>();
            nestedExpected.put("timestamp", "2026-02-10T16:19:14.123456Z");
            nestedExpected.put("label", "hello");

            Map<String, Object> nestedActual = new HashMap<>();
            nestedActual.put("timestamp", actualInstant);
            nestedActual.put("label", "hello");

            Map<String, Object> expected = new HashMap<>();
            expected.put("nested", nestedExpected);

            Map<String, Object> actual = new HashMap<>();
            actual.put("nested", nestedActual);

            assertTrue(AvroUtils.deepEquals(expected, actual));
        }

        @Test
        @DisplayName("Should apply type-driven date comparison inside nested lists")
        void shouldMatch_NestedListWithDateField() {
            Instant actualInstant1 = Instant.parse("2026-02-10T16:19:14.000Z");
            Instant actualInstant2 = Instant.parse("2026-02-11T08:30:00.000000Z");

            Map<String, Object> item1Expected = new HashMap<>();
            item1Expected.put("timestamp", "2026-02-10T16:19:14Z");
            Map<String, Object> item2Expected = new HashMap<>();
            item2Expected.put("timestamp", "2026-02-11T08:30:00.000000Z");

            Map<String, Object> item1Actual = new HashMap<>();
            item1Actual.put("timestamp", actualInstant1);
            Map<String, Object> item2Actual = new HashMap<>();
            item2Actual.put("timestamp", actualInstant2);

            Map<String, Object> expected = new HashMap<>();
            expected.put("items", List.of(item1Expected, item2Expected));

            Map<String, Object> actual = new HashMap<>();
            actual.put("items", List.of(item1Actual, item2Actual));

            assertTrue(AvroUtils.deepEquals(expected, actual));
        }
    }

    @Nested
    @DisplayName("Dot-notation excludedKeys (nested field exclusion)")
    class DotNotationExcludedKeysTests {

        @Test
        @DisplayName(
                "Should exclude only the nested field when a dot-notation path is used, keeping sibling fields compared")
        void shouldExcludeOnlyNestedField_WhenDotNotationPathIsUsed() {
            // Mirrors the reported schema: RootField is a nested record with NestedField1 / NestedField2.
            Map<String, Object> expectedNested = new HashMap<>();
            expectedNested.put("NestedField1", "same-value");
            expectedNested.put("NestedField2", "expected-value");

            Map<String, Object> actualNested = new HashMap<>();
            actualNested.put("NestedField1", "same-value");
            actualNested.put("NestedField2", "actual-value"); // differs, but excluded via dot-notation

            Map<String, Object> expected = new HashMap<>();
            expected.put("RootField", expectedNested);

            Map<String, Object> actual = new HashMap<>();
            actual.put("RootField", actualNested);

            assertTrue(
                    AvroUtils.deepEquals(expected, actual, List.of("RootField.NestedField2")),
                    "RootField.NestedField2 should be excluded, leaving NestedField1 (which matches) to make the"
                            + " records equal");
        }

        @Test
        @DisplayName("Should still fail when a sibling (non-excluded) nested field differs")
        void shouldStillFail_WhenNonExcludedNestedFieldDiffers() {
            Map<String, Object> expectedNested = new HashMap<>();
            expectedNested.put("NestedField1", "expected-value");
            expectedNested.put("NestedField2", "same-value");

            Map<String, Object> actualNested = new HashMap<>();
            actualNested.put("NestedField1", "actual-value"); // differs and is NOT excluded
            actualNested.put("NestedField2", "same-value");

            Map<String, Object> expected = new HashMap<>();
            expected.put("RootField", expectedNested);

            Map<String, Object> actual = new HashMap<>();
            actual.put("RootField", actualNested);

            assertFalse(
                    AvroUtils.deepEquals(expected, actual, List.of("RootField.NestedField2")),
                    "NestedField1 differs and is not excluded, so the records should not match");
        }

        @Test
        @DisplayName("Should support multi-level dot-notation paths")
        void shouldSupportMultiLevelDotNotationPaths() {
            Map<String, Object> expectedLevel3 = new HashMap<>();
            expectedLevel3.put("value", "expected");

            Map<String, Object> actualLevel3 = new HashMap<>();
            actualLevel3.put("value", "actual"); // differs, excluded via "a.b.c.value"

            Map<String, Object> expectedLevel2 = new HashMap<>();
            expectedLevel2.put("c", expectedLevel3);
            Map<String, Object> actualLevel2 = new HashMap<>();
            actualLevel2.put("c", actualLevel3);

            Map<String, Object> expectedLevel1 = new HashMap<>();
            expectedLevel1.put("b", expectedLevel2);
            Map<String, Object> actualLevel1 = new HashMap<>();
            actualLevel1.put("b", actualLevel2);

            Map<String, Object> expected = new HashMap<>();
            expected.put("a", expectedLevel1);
            Map<String, Object> actual = new HashMap<>();
            actual.put("a", actualLevel1);

            assertTrue(AvroUtils.deepEquals(expected, actual, List.of("a.b.c.value")));
        }

        @Test
        @DisplayName("Should apply dot-notation exclusion to every element of an array field")
        void shouldApplyDotNotationExclusionToArrayElements() {
            Map<String, Object> expectedItem1 = new HashMap<>();
            expectedItem1.put("id", 1);
            expectedItem1.put("volatileField", "expected-1");

            Map<String, Object> expectedItem2 = new HashMap<>();
            expectedItem2.put("id", 2);
            expectedItem2.put("volatileField", "expected-2");

            Map<String, Object> actualItem1 = new HashMap<>();
            actualItem1.put("id", 1);
            actualItem1.put("volatileField", "actual-1");

            Map<String, Object> actualItem2 = new HashMap<>();
            actualItem2.put("id", 2);
            actualItem2.put("volatileField", "actual-2");

            Map<String, Object> expected = new HashMap<>();
            expected.put("items", List.of(expectedItem1, expectedItem2));

            Map<String, Object> actual = new HashMap<>();
            actual.put("items", List.of(actualItem1, actualItem2));

            assertTrue(AvroUtils.deepEquals(expected, actual, List.of("items.volatileField")));
        }

        @Test
        @DisplayName("Should keep excluding a field at every depth when a simple (non-dotted) name is used")
        void shouldStillSupportSimpleNameExclusion_ForBackwardCompatibility() {
            Map<String, Object> expectedNested = new HashMap<>();
            expectedNested.put("NestedField1", "same-value");
            expectedNested.put("NestedField2", "expected-value");

            Map<String, Object> actualNested = new HashMap<>();
            actualNested.put("NestedField1", "same-value");
            actualNested.put("NestedField2", "actual-value");

            Map<String, Object> expected = new HashMap<>();
            expected.put("RootField", expectedNested);

            Map<String, Object> actual = new HashMap<>();
            actual.put("RootField", actualNested);

            // Simple (non-dotted) exclusion still works at any nesting depth, as before.
            assertTrue(AvroUtils.deepEquals(expected, actual, List.of("NestedField2")));
        }

        @Test
        @DisplayName("Should correctly account for effective size when a nested field is excluded via dot-notation")
        void shouldAccountForEffectiveSize_WhenNestedFieldExcludedViaDotNotation() {
            // Even though "RootField.NestedField2" is excluded, RootField itself must remain counted at the
            // top level (only NestedField2 is skipped at the nested level), so this must not be treated as a
            // record-size mismatch.
            Map<String, Object> expectedNested = new HashMap<>();
            expectedNested.put("NestedField1", "value1");
            expectedNested.put("NestedField2", "expected-only");

            Map<String, Object> actualNested = new HashMap<>();
            actualNested.put("NestedField1", "value1");
            actualNested.put("NestedField2", "actual-only");

            Map<String, Object> expected = new HashMap<>();
            expected.put("RootField", expectedNested);
            expected.put("otherField", "same");

            Map<String, Object> actual = new HashMap<>();
            actual.put("RootField", actualNested);
            actual.put("otherField", "same");

            assertTrue(AvroUtils.deepEquals(expected, actual, List.of("RootField.NestedField2")));
        }
    }
}
