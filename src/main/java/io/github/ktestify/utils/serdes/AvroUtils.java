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

import static io.github.ktestify.constants.LogMessagesConstants.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.github.ktestify.exceptions.ComparisonException;
import io.github.ktestify.exceptions.ProducerException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import lombok.NonNull;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for Apache Avro operations, providing comprehensive functionality for:
 *
 * <ul>
 *   <li>JSON to Avro conversions with schema validation
 *   <li>Avro record comparison and matching operations
 *   <li>Date/time format conversions and logical type handling
 *   <li>Complex nested data structure processing
 * </ul>
 *
 * <p>This class is designed to be thread-safe and all methods are static. It handles various Avro logical types
 * including dates, timestamps, decimals, and provides robust error handling for malformed data.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Smart record comparison with exclusion capabilities
 *   <li>Nested key value extraction and matching
 *   <li>Automatic date string to timestamp conversion
 *   <li>Support for complex Avro schemas including unions, arrays, and maps
 * </ul>
 *
 * @version 1.1.0
 * @since 0.2.40
 */
public final class AvroUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvroUtils.class);

    /** Date format patterns supported for automatic date detection and conversion. */
    private static final String[] SUPPORTED_DATE_PATTERNS = {
        "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss:SSS", "yyyy-MM-dd'T'HH:mm:ss:SSS'Z'"
    };

    /** UTC zone formatters for timestamp conversion with optional milliseconds and microseconds. */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER_WITH_MS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]'Z'").withZone(java.time.ZoneOffset.UTC);

    private static final DateTimeFormatter TIMESTAMP_FORMATTER_WITH_MICROS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]'Z'").withZone(java.time.ZoneOffset.UTC);

    /** Private constructor to prevent instantiation of utility class. */
    private AvroUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ===========================================
    // JSON FORMATTING AND CONVERSION METHODS
    // ===========================================

    /**
     * Formats a JSON string into a more readable (pretty-printed) format.
     *
     * <p>This method parses the input JSON string and reformats it with proper indentation and line breaks for better
     * readability. Uses LONG_OR_DOUBLE number strategy to maintain precision.
     *
     * @param uglyAvroValue the JSON string to be formatted, must be valid JSON
     * @return a pretty-printed JSON string with proper indentation
     * @throws JsonSyntaxException if the input is not valid JSON
     * @throws IllegalArgumentException if uglyAvroValue is null
     */
    public static String getPrettyAvroValue(@NonNull String uglyAvroValue) {
        Objects.requireNonNull(uglyAvroValue, "JSON string cannot be null");

        var gson = new GsonBuilder()
                .setPrettyPrinting()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .create();

        var jsonElement = JsonParser.parseString(uglyAvroValue);
        return gson.toJson(jsonElement);
    }

    public static GenericRecord convertJsonToAvroRecord(JsonObject jsonEvent, Schema schema) {
        GenericRecord avroRecord = new GenericData.Record(schema);
        for (Map.Entry<String, JsonElement> entry : jsonEvent.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            Schema.Field field = schema.getField(key);
            if (field != null) {
                avroRecord.put(key, AvroUtils.convertJsonToAvro(value, field.schema()));
            }
        }
        return avroRecord;
    }

    /**
     * Converts a Map to a pretty-printed JSON string representation.
     *
     * <p>This method is useful for debugging and logging purposes. If the provided map is null or empty, it returns an
     * empty JSON object string "{}".
     *
     * @param map the Map to be converted to JSON, can be null or empty
     * @return a pretty-printed JSON string representation of the Map, or "{}" if null/empty
     */
    public static String convertMapToJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            LOGGER.error("The provided map is null or empty");
            return "{}";
        }

        var gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(map);
    }

    public static JsonObject readJsonFromFile(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            LOGGER.error("Error reading JSON from file: {}", filePath, e);
            throw new RuntimeException("Error reading JSON from file: " + filePath, e);
        }
    }

    /**
     * Parses a JSON string from file content and returns it as a JsonElement.
     *
     * <p>This method provides a consistent way to parse JSON content from files and convert it to a JsonElement for
     * further processing.
     *
     * @param fileContent the string content of the file containing JSON
     * @return the parsed JsonElement representing the JSON structure
     * @throws JsonSyntaxException if the file content is not valid JSON
     * @throws IllegalArgumentException if fileContent is null
     */
    public static JsonElement getJsonElementFromFile(@NonNull String fileContent) {
        Objects.requireNonNull(fileContent, "File content cannot be null");

        var gson = new GsonBuilder().setPrettyPrinting().create();
        var jsonElement = JsonParser.parseString(fileContent);
        return gson.toJsonTree(jsonElement);
    }

    /**
     * Converts a JSON string to a Map representation for easier manipulation.
     *
     * <p>This method is primarily used for converting JSON-formatted Avro records to a Map for easier manipulation and
     * comparison operations. Uses LONG_OR_DOUBLE number strategy to maintain numeric precision.
     *
     * @param json the JSON string to be converted, must be valid JSON
     * @return a Map representation of the JSON string
     * @throws JsonSyntaxException if the input is not valid JSON
     * @throws IllegalArgumentException if json is null
     */
    public static Map<String, Object> convertJsonToMap(@NonNull String json) {
        Objects.requireNonNull(json, "JSON string cannot be null");

        var gson = new GsonBuilder()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .create();

        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // ===========================================
    // AVRO RECORD COMPARISON METHODS
    // ===========================================

    /**
     * Performs strict equality comparison between two Avro records in JSON format.
     *
     * <p>This method compares two JSON representations of Avro records for exact equality, including key/value orders
     * and spacing. Use this when you need to ensure records are identical in every aspect.
     *
     * @param expectedRecord the expected Avro record in JSON format
     * @param actualRecord the actual Avro record in JSON format
     * @return true if the records are strictly identical, false otherwise
     * @throws JsonSyntaxException if either record is not valid JSON
     * @throws IllegalArgumentException if any parameter is null
     */
    public static boolean doesAvroRecordsStrictlyMatches(@NonNull String expectedRecord, @NonNull String actualRecord) {
        Objects.requireNonNull(expectedRecord, AVRO_UTILS_EXPECTED_RECORD_NULL);
        Objects.requireNonNull(actualRecord, AVRO_UTILS_ACTUAL_RECORD_NULL);

        var expectedJsonElement = JsonParser.parseString(expectedRecord);
        var actualJsonElement = JsonParser.parseString(actualRecord);

        return expectedJsonElement.equals(actualJsonElement);
    }

    /**
     * Performs intelligent comparison between two Avro records, allowing for differences in key/value orders and
     * spacing.
     *
     * <p>This method converts both records to Map representations and performs a deep comparison that ignores key
     * ordering and formatting differences. It also handles automatic date string to timestamp conversion for consistent
     * comparison.
     *
     * @param expectedRecord the expected Avro record in JSON format
     * @param actualRecord the actual Avro record in JSON format
     * @return true if the records match semantically, false otherwise
     * @throws JsonSyntaxException if either record is not valid JSON
     * @throws IllegalArgumentException if any parameter is null
     */
    public static boolean doesAvroRecordsSmartMatches(@NonNull String expectedRecord, @NonNull String actualRecord) {
        Objects.requireNonNull(expectedRecord, AVRO_UTILS_EXPECTED_RECORD_NULL);
        Objects.requireNonNull(actualRecord, AVRO_UTILS_ACTUAL_RECORD_NULL);

        var expectedMap = convertJsonToMap(expectedRecord);
        var actualMap = convertJsonToMap(actualRecord);

        return deepEquals(expectedMap, actualMap);
    }

    /**
     * Performs intelligent comparison between two Avro records with the ability to exclude specific keys from the
     * comparison.
     *
     * <p>This method is particularly useful when you want to compare records while ignoring certain fields like
     * timestamps, IDs, or other fields that may vary between records but don't affect the semantic equality.
     *
     * @param expectedRecord the expected Avro record in JSON format
     * @param actualRecord the actual Avro record in JSON format
     * @param excludedKeys a list of JSON keys that will be excluded from comparison
     * @return true if the records match (excluding specified keys), false otherwise
     * @throws JsonSyntaxException if either record is not valid JSON
     * @throws IllegalArgumentException if any parameter is null
     */
    public static boolean doesAvroRecordsSmartMatchesWithExclusions(
            @NonNull String expectedRecord, @NonNull String actualRecord, @NonNull List<String> excludedKeys) {
        Objects.requireNonNull(expectedRecord, AVRO_UTILS_EXPECTED_RECORD_NULL);
        Objects.requireNonNull(actualRecord, AVRO_UTILS_ACTUAL_RECORD_NULL);
        Objects.requireNonNull(excludedKeys, "Excluded keys list cannot be null");

        var expectedMap = convertJsonToMap(expectedRecord);
        var actualMap = convertJsonToMap(actualRecord);

        return deepEquals(expectedMap, actualMap, excludedKeys);
    }

    /**
     * Performs deep equality comparison between two maps representing Avro records.
     *
     * <p>This method handles nested structures, collections, and automatically converts date strings to timestamps for
     * consistent comparison. It provides comprehensive logging for debugging purposes.
     *
     * @param expectedValueMap the map representing the expected Avro record
     * @param actualValueMap the map representing the actual Avro record
     * @return true if the maps are deeply equal, false otherwise
     * @throws IllegalArgumentException if any parameter is null
     */
    public static boolean deepEquals(
            @NonNull Map<String, Object> expectedValueMap, @NonNull Map<String, Object> actualValueMap) {
        Objects.requireNonNull(expectedValueMap, "Expected value map cannot be null");
        Objects.requireNonNull(actualValueMap, "Actual value map cannot be null");

        LOGGER.debug(AVRO_UTILS_DATE_CONVERSION_INFO);

        var expectedConverted = convertDatesToTimestamps(new HashMap<>(expectedValueMap));
        var actualConverted = convertDatesToTimestamps(new HashMap<>(actualValueMap));

        return performDeepEqualsComparison(expectedConverted, actualConverted, Collections.emptyList());
    }

    /**
     * Performs deep equality comparison between two maps with the ability to exclude specific keys.
     *
     * <p>This method extends the basic deep equality comparison by allowing certain keys to be excluded from the
     * comparison. This is useful for ignoring volatile fields like timestamps, UUIDs, or other fields that may change
     * between records.
     *
     * @param expectedValueMap the map representing the expected Avro record
     * @param actualValueMap the map representing the actual Avro record
     * @param excludedKeys a list of keys to exclude from comparison
     * @return true if the maps are deeply equal (excluding specified keys), false otherwise
     * @throws IllegalArgumentException if any parameter is null
     */
    public static boolean deepEquals(
            @NonNull Map<String, Object> expectedValueMap,
            @NonNull Map<String, Object> actualValueMap,
            @NonNull List<String> excludedKeys) {
        Objects.requireNonNull(expectedValueMap, "Expected value map cannot be null");
        Objects.requireNonNull(actualValueMap, "Actual value map cannot be null");
        Objects.requireNonNull(excludedKeys, "Excluded keys list cannot be null");

        LOGGER.debug(AVRO_UTILS_DATE_CONVERSION_INFO);
        LOGGER.info(AVRO_UTILS_DEEP_EQUALS_WITH_EXCLUDED_KEYS, excludedKeys);

        var expectedConverted = convertDatesToTimestamps(new HashMap<>(expectedValueMap));
        var actualConverted = convertDatesToTimestamps(new HashMap<>(actualValueMap));

        return performDeepEqualsComparison(expectedConverted, actualConverted, excludedKeys);
    }

    // ===========================================
    // ASSERTION METHODS (THROWS EXCEPTIONS)
    // ===========================================

    /**
     * Asserts that two Avro records strictly match, throwing a ComparisonException if they don't.
     *
     * <p>This method compares two JSON representations of Avro records for exact equality, including key/value orders
     * and spacing. If the records don't match, it throws a ComparisonException with a detailed error message.
     *
     * @param expectedRecord the expected Avro record in JSON format
     * @param actualRecord the actual Avro record in JSON format
     * @throws ComparisonException if the records do not strictly match
     * @throws JsonSyntaxException if either record is not valid JSON
     * @throws IllegalArgumentException if any parameter is null
     */
    public static void assertAvroRecordsStrictlyMatch(@NonNull String expectedRecord, @NonNull String actualRecord) {
        Objects.requireNonNull(expectedRecord, AVRO_UTILS_EXPECTED_RECORD_NULL);
        Objects.requireNonNull(actualRecord, AVRO_UTILS_ACTUAL_RECORD_NULL);

        if (!doesAvroRecordsStrictlyMatches(expectedRecord, actualRecord)) {
            throw new ComparisonException(
                    "Avro records do not strictly match.\nExpected: " + expectedRecord + "\nActual: " + actualRecord);
        }
    }

    /**
     * Asserts that two Avro records smart match, throwing a ComparisonException if they don't.
     *
     * <p>This method performs intelligent comparison between two Avro records, allowing for differences in key/value
     * orders and spacing. If the records don't match semantically, it throws a ComparisonException with a detailed
     * error message.
     *
     * @param expectedRecord the expected Avro record in JSON format
     * @param actualRecord the actual Avro record in JSON format
     * @throws ComparisonException if the records do not match semantically
     * @throws JsonSyntaxException if either record is not valid JSON
     * @throws IllegalArgumentException if any parameter is null
     */
    public static void assertAvroRecordsSmartMatch(@NonNull String expectedRecord, @NonNull String actualRecord) {
        Objects.requireNonNull(expectedRecord, AVRO_UTILS_EXPECTED_RECORD_NULL);
        Objects.requireNonNull(actualRecord, AVRO_UTILS_ACTUAL_RECORD_NULL);

        if (!doesAvroRecordsSmartMatches(expectedRecord, actualRecord)) {
            throw new ComparisonException("Avro records do not match.\nExpected: " + getPrettyAvroValue(expectedRecord)
                    + "\nActual: " + getPrettyAvroValue(actualRecord));
        }
    }

    /**
     * Asserts that two Avro records smart match with exclusions, throwing a ComparisonException if they don't.
     *
     * <p>This method performs intelligent comparison between two Avro records with the ability to exclude specific keys
     * from the comparison. If the records don't match (excluding specified keys), it throws a ComparisonException with
     * a detailed error message.
     *
     * @param expectedRecord the expected Avro record in JSON format
     * @param actualRecord the actual Avro record in JSON format
     * @param excludedKeys a list of JSON keys that will be excluded from comparison
     * @throws ComparisonException if the records do not match (excluding specified keys)
     * @throws JsonSyntaxException if either record is not valid JSON
     * @throws IllegalArgumentException if any parameter is null
     */
    public static void assertAvroRecordsSmartMatchWithExclusions(
            @NonNull String expectedRecord, @NonNull String actualRecord, @NonNull List<String> excludedKeys) {
        Objects.requireNonNull(expectedRecord, AVRO_UTILS_EXPECTED_RECORD_NULL);
        Objects.requireNonNull(actualRecord, AVRO_UTILS_ACTUAL_RECORD_NULL);
        Objects.requireNonNull(excludedKeys, "Excluded keys list cannot be null");

        if (!doesAvroRecordsSmartMatchesWithExclusions(expectedRecord, actualRecord, excludedKeys)) {
            throw new ComparisonException(
                    "Avro records do not match (with exclusions: " + excludedKeys + ").\nExpected: "
                            + getPrettyAvroValue(expectedRecord) + "\nActual: " + getPrettyAvroValue(actualRecord));
        }
    }

    /**
     * Asserts that two maps representing Avro records are deeply equal, throwing a ComparisonException if they're not.
     *
     * <p>This method handles nested structures, collections, and automatically converts date strings to timestamps for
     * consistent comparison. It provides comprehensive logging for debugging purposes.
     *
     * @param expectedValueMap the map representing the expected Avro record
     * @param actualValueMap the map representing the actual Avro record
     * @throws ComparisonException if the maps are not deeply equal
     * @throws IllegalArgumentException if any parameter is null
     */
    public static void assertDeepEquals(
            @NonNull Map<String, Object> expectedValueMap, @NonNull Map<String, Object> actualValueMap) {
        Objects.requireNonNull(expectedValueMap, "Expected value map cannot be null");
        Objects.requireNonNull(actualValueMap, "Actual value map cannot be null");

        if (!deepEquals(expectedValueMap, actualValueMap)) {
            throw new ComparisonException(
                    "Avro value maps do not match.\nExpected: " + expectedValueMap + "\nActual: " + actualValueMap);
        }
    }

    /**
     * Asserts that two maps are deeply equal with exclusions, throwing a ComparisonException if they're not.
     *
     * <p>This method extends the basic deep equality comparison by allowing certain keys to be excluded from the
     * comparison. If the maps don't match (excluding specified keys), it throws a ComparisonException with a detailed
     * error message.
     *
     * @param expectedValueMap the map representing the expected Avro record
     * @param actualValueMap the map representing the actual Avro record
     * @param excludedKeys a list of keys to exclude from comparison
     * @throws ComparisonException if the maps are not deeply equal (excluding specified keys)
     * @throws IllegalArgumentException if any parameter is null
     */
    public static void assertDeepEquals(
            @NonNull Map<String, Object> expectedValueMap,
            @NonNull Map<String, Object> actualValueMap,
            @NonNull List<String> excludedKeys) {
        Objects.requireNonNull(expectedValueMap, "Expected value map cannot be null");
        Objects.requireNonNull(actualValueMap, "Actual value map cannot be null");
        Objects.requireNonNull(excludedKeys, "Excluded keys list cannot be null");

        if (!deepEquals(expectedValueMap, actualValueMap, excludedKeys)) {
            throw new ComparisonException("Avro value maps do not match (with exclusions: " + excludedKeys
                    + ").\nExpected: " + expectedValueMap + "\nActual: " + actualValueMap);
        }
    }

    /**
     * Asserts that the value of a specified key matches between two Avro records, throwing a ComparisonException if it
     * doesn't.
     *
     * <p>This method supports both simple keys and nested keys using dot notation. For nested keys, it traverses the
     * object hierarchy to find the target value. It also supports array indexing using bracket notation (e.g.,
     * "users[0].name").
     *
     * @param key the key to check in the records (supports nested keys with dot notation)
     * @param expectedRecord the expected Avro record in JSON format
     * @param actualRecord the actual Avro record in JSON format
     * @throws ComparisonException if the value does not match for the key
     * @throws JsonSyntaxException if either record is not valid JSON
     * @throws IllegalArgumentException if any parameter is null
     */
    public static void assertAvroValueFromKeyMatches(
            @NonNull String key, @NonNull String expectedRecord, @NonNull String actualRecord) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(expectedRecord, AVRO_UTILS_EXPECTED_RECORD_NULL);
        Objects.requireNonNull(actualRecord, AVRO_UTILS_ACTUAL_RECORD_NULL);

        if (!doesAvroValueFromKeyMatchesRecords(key, expectedRecord, actualRecord)) {
            var expectedMap = convertJsonToMap(expectedRecord);
            var actualMap = convertJsonToMap(actualRecord);
            Object expectedValue = getNestedValue(expectedMap, key);
            Object actualValue = getNestedValue(actualMap, key);

            throw new ComparisonException("Avro value for key '" + key + "' does not match.\nExpected: " + expectedValue
                    + "\nActual: " + actualValue);
        }
    }

    /**
     * Extracts a nested value from a map using dot notation and array indexing.
     *
     * <p>This method traverses a nested map structure to extract a value at the specified path. It supports dot
     * notation for nested keys (e.g., "person.name") and array indexing using bracket notation (e.g., "users[0].name").
     *
     * @param map the map to extract the value from
     * @param key the key path (supports dot notation and array indexing)
     * @return the value at the specified path, or null if not found
     */
    private static Object getNestedValue(Map<String, Object> map, String key) {
        if (!key.contains(".") && !key.contains("[")) {
            return map.get(key);
        }

        var keys = key.split("\\.");
        Object current = map;

        try {
            for (var currentKey : keys) {
                if (currentKey.contains("[")) {
                    var arrayKey = currentKey.substring(0, currentKey.indexOf("["));
                    var indexStr = currentKey.substring(currentKey.indexOf("[") + 1, currentKey.indexOf("]"));
                    var index = Integer.parseInt(indexStr);

                    var list = (List<Object>) ((Map<String, Object>) current).get(arrayKey);
                    if (list != null && index >= 0 && index < list.size()) {
                        current = list.get(index);
                    } else {
                        return null;
                    }
                } else {
                    current = ((Map<String, Object>) current).get(currentKey);
                    if (current == null) {
                        return null;
                    }
                }
            }
            return current;
        } catch (Exception e) {
            LOGGER.error("Error extracting nested value for key: {}", key, e);
            return null;
        }
    }

    // ===========================================
    // INTERNAL COMPARISON METHODS
    // ===========================================

    /**
     * Internal method that performs the actual deep equality comparison logic.
     *
     * <p>This method handles the core comparison logic including nested objects, lists, and null value handling. It
     * provides detailed logging for debugging and troubleshooting purposes.
     *
     * @param expectedValueMap the processed expected map
     * @param actualValueMap the processed actual map
     * @param excludedKeys list of keys to exclude from comparison
     * @return true if maps are equal, false otherwise
     */
    private static boolean performDeepEqualsComparison(
            Map<String, Object> expectedValueMap, Map<String, Object> actualValueMap, List<String> excludedKeys) {

        long expectedExcludedCount = excludedKeys.stream().filter(expectedValueMap::containsKey).count();
        long actualExcludedCount   = excludedKeys.stream().filter(actualValueMap::containsKey).count();

        long effectiveExpectedSize = expectedValueMap.size() - expectedExcludedCount;
        long effectiveActualSize   = actualValueMap.size()   - actualExcludedCount;

        if (effectiveExpectedSize != effectiveActualSize) {
            LOGGER.error(AVRO_UTILS_RECORD_MISMATCH);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        AVRO_UTILS_RECORD_MISMATCH_DETAILS,
                        "Expected",
                        effectiveExpectedSize,
                        getKeySets(expectedValueMap));
                LOGGER.debug(
                        AVRO_UTILS_RECORD_MISMATCH_DETAILS,
                        "Actual",
                        effectiveActualSize,
                        getKeySets(actualValueMap));
            }
            return false;
        }

        if (expectedValueMap.equals(actualValueMap)) {
            LOGGER.info(AVRO_UTILS_VALUES_STRICTLY_MATCH);
            return true;
        }

        for (var entry : expectedValueMap.entrySet()) {
            var key = entry.getKey();
            if (excludedKeys.contains(key)) {
                LOGGER.debug("The key {} will not be matched due to it being in the excluded list", key);
                continue;
            }

            if (!actualValueMap.containsKey(key)) {
                LOGGER.error("Missing key {} in actual value", key);
                return false;
            }

            var expectedValue = entry.getValue();
            var actualValue = actualValueMap.get(key);

            if (!compareValues(expectedValue, actualValue, excludedKeys, key)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compares two values, handling different types including nested objects and lists.
     *
     * @param expectedValue the expected value
     * @param actualValue the actual value
     * @param excludedKeys list of keys to exclude from nested comparisons
     * @param key the current key being compared (for logging)
     * @return true if values are equal, false otherwise
     */
    private static boolean compareValues(
            Object expectedValue, Object actualValue, List<String> excludedKeys, String key) {
        if (expectedValue instanceof Map && actualValue instanceof Map) {
            LOGGER.debug(
                    AVRO_UTILS_NESTED_OBJECT_FOUND, expectedValue.getClass().getSimpleName());
            if (!performDeepEqualsComparison(
                    (Map<String, Object>) expectedValue, (Map<String, Object>) actualValue, excludedKeys)) {
                LOGGER.debug(AVRO_UTILS_NESTED_OBJECT_NOT_MATCH);
                return false;
            }
        } else if (expectedValue instanceof List && actualValue instanceof List) {
            LOGGER.debug(
                    AVRO_UTILS_NESTED_OBJECT_FOUND + " key : {}",
                    expectedValue.getClass().getSimpleName(),
                    key);
            if (!deepEqualsList((List<Object>) expectedValue, (List<Object>) actualValue, excludedKeys)) {
                LOGGER.error("Value mismatch for key {} values does not match for a list", key);
                return false;
            }
        } else if (expectedValue == null) {
            if (actualValue != null) {
                LOGGER.error(AVRO_UTILS_VALUES_MISMATCH_SHOULD_BE_NULL, key, actualValue);
                return false;
            }
        } else if (!expectedValue.equals(actualValue)) {
            LOGGER.error(
                    AVRO_UTILS_VALUES_MISMATCH_WITH_CLASS_NAMES,
                    key,
                    expectedValue,
                    expectedValue.getClass().getSimpleName(),
                    actualValue,
                    actualValue.getClass().getSimpleName());
            return false;
        }

        return true;
    }

    /**
     * Performs deep equality comparison on two lists, handling nested objects and excluded keys. This method supports
     * smart matching where objects in arrays can be in different orders.
     *
     * @param expectedList the expected list
     * @param actualList the actual list
     * @param excludedKeys list of keys to exclude from nested object comparisons
     * @return true if lists are equal, false otherwise
     */
    private static boolean deepEqualsList(
            List<Object> expectedList, List<Object> actualList, List<String> excludedKeys) {
        if (expectedList.size() != actualList.size()) {
            LOGGER.error("List size mismatch. Expected: {}, Actual: {}", expectedList.size(), actualList.size());
            return false;
        }

        // For primitive arrays or mixed types, fall back to ordered comparison
        if (expectedList.isEmpty() || !isObjectArray(expectedList)) {

            return deepEqualsListOrdered(expectedList, actualList, excludedKeys);
        }

        // Smart matching for object arrays - match regardless of order
        return deepEqualsListUnordered(expectedList, actualList, excludedKeys);
    }

    /**
     * Checks if the list contains only Map objects (complex objects).
     *
     * @param list the list to check
     * @return true if all elements are Map instances, false otherwise
     */
    private static boolean isObjectArray(List<Object> list) {
        return list.stream().allMatch(item -> item instanceof Map);
    }

    /**
     * Performs ordered comparison of lists (original behavior).
     *
     * @param expectedList the expected list
     * @param actualList the actual list
     * @param excludedKeys list of keys to exclude from nested object comparisons
     * @return true if lists are equal in order, false otherwise
     */
    private static boolean deepEqualsListOrdered(
            List<Object> expectedList, List<Object> actualList, List<String> excludedKeys) {
        LOGGER.debug("Performing ordered array comparison for {} expected objects", expectedList.size());
        for (int i = 0; i < expectedList.size(); i++) {
            var expected = expectedList.get(i);
            var actual = actualList.get(i);

            if (expected instanceof Map && actual instanceof Map) {
                if (!performDeepEqualsComparison(
                        (Map<String, Object>) expected, (Map<String, Object>) actual, excludedKeys)) {
                    return false;
                }
            } else if (!Objects.equals(expected, actual)) {
                LOGGER.error("List element mismatch at index {}. Expected: {}, Actual: {}", i, expected, actual);
                return false;
            }
        }

        return true;
    }

    /**
     * Performs unordered comparison of object lists. Each object in the expected list must find a matching object in
     * the actual list, but order doesn't matter.
     *
     * @param expectedList the expected list of objects
     * @param actualList the actual list of objects
     * @param excludedKeys list of keys to exclude from nested object comparisons
     * @return true if all expected objects find matches in actual list, false otherwise
     */
    private static boolean deepEqualsListUnordered(
            List<Object> expectedList, List<Object> actualList, List<String> excludedKeys) {
        LOGGER.debug("Performing smart unordered array comparison for {} expected objects", expectedList.size());

        // Keep track of which actual objects have been matched
        var usedIndices = new HashSet<Integer>();

        // Try to find a match for each expected object
        for (int expectedIndex = 0; expectedIndex < expectedList.size(); expectedIndex++) {
            var expectedObject = (Map<String, Object>) expectedList.get(expectedIndex);
            boolean foundMatch = false;

            // Try to match against each unused actual object
            for (int actualIndex = 0; actualIndex < actualList.size(); actualIndex++) {
                if (usedIndices.contains(actualIndex)) {
                    continue; // Skip already matched objects
                }

                var actualObject = (Map<String, Object>) actualList.get(actualIndex);

                if (performDeepEqualsComparison(expectedObject, actualObject, excludedKeys)) {
                    LOGGER.debug(
                            "Found match for expected object at index {} with actual object at index {}",
                            expectedIndex,
                            actualIndex);
                    usedIndices.add(actualIndex);
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                LOGGER.error(
                        "No matching object found in actual list for expected object at index {}: \n {}",
                        expectedIndex,
                        expectedObject);
                return false;
            }
        }

        LOGGER.debug("Successfully matched all {} objects in unordered array comparison", expectedList.size());
        return true;
    }

    /**
     * Checks if the value of a specified key matches between two Avro records.
     *
     * <p>This method supports both simple keys and nested keys using dot notation. For nested keys, it traverses the
     * object hierarchy to find the target value. It also supports array indexing using bracket notation (e.g.,
     * "users[0].name").
     *
     * @param key the key to check in the records (supports nested keys with dot notation)
     * @param expectedRecord the expected Avro record in JSON format
     * @param actualRecord the actual Avro record in JSON format
     * @return true if the value matches for the key, false otherwise
     * @throws JsonSyntaxException if either record is not valid JSON
     * @throws IllegalArgumentException if any parameter is null
     */
    public static boolean doesAvroValueFromKeyMatchesRecords(
            @NonNull String key, @NonNull String expectedRecord, @NonNull String actualRecord) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(expectedRecord, AVRO_UTILS_EXPECTED_RECORD_NULL);
        Objects.requireNonNull(actualRecord, AVRO_UTILS_ACTUAL_RECORD_NULL);

        var expectedValueMap = convertJsonToMap(expectedRecord);
        var actualValueMap = convertJsonToMap(actualRecord);

        if (key.contains(".")) {
            LOGGER.debug(AVRO_UTILS_EXPECTED_KEY_NESTED_INFO, key);
            return matchNestedValue(key, expectedValueMap, actualValueMap);
        }

        LOGGER.debug(AVRO_UTILS_EXPECTED_KEY_CONTAINED_IN_MAIN_OBJECT, key);
        return matchValue(key, expectedValueMap, actualValueMap);
    }

    /**
     * Checks if a specified value matches the value at a nested key in an Avro record.
     *
     * <p>This method is useful for validating that a specific nested field contains the expected value. It supports dot
     * notation for nested keys and provides detailed logging for debugging purposes.
     *
     * @param expectedValue the value expected at the specified key
     * @param nestedKey the nested key within the Avro record (supports dot notation)
     * @param actualRecord the actual Avro record in JSON format
     * @return true if the value matches at the specified nested key, false otherwise
     * @throws JsonSyntaxException if the record is not valid JSON
     * @throws IllegalArgumentException if any parameter is null
     */
    public static boolean doesAvroValueFromKeyMatchesRecord(
            @NonNull String expectedValue, @NonNull String nestedKey, @NonNull String actualRecord) {
        Objects.requireNonNull(expectedValue, "Expected value cannot be null");
        Objects.requireNonNull(nestedKey, "Nested key cannot be null");
        Objects.requireNonNull(actualRecord, AVRO_UTILS_ACTUAL_RECORD_NULL);

        var actualValueMap = convertJsonToMap(actualRecord);

        if (nestedKey.contains(".")) {
            LOGGER.debug(AVRO_UTILS_EXPECTED_KEY_NESTED_INFO, nestedKey);
            return matchNestedValue(expectedValue, nestedKey, actualValueMap);
        } else {
            LOGGER.debug(AVRO_UTILS_EXPECTED_KEY_CONTAINED_IN_MAIN_OBJECT, nestedKey);
            return actualValueMap.containsKey(nestedKey)
                    && expectedValue.equals(String.valueOf(actualValueMap.get(nestedKey)));
        }
    }

    /**
     * Matches the value of a nested key within two maps, supporting array indexing.
     *
     * <p>This method traverses nested object hierarchies using dot notation and supports array indexing with bracket
     * notation. It provides robust error handling for invalid paths and indices.
     *
     * @param nestedKey the nested key to be matched, in dot notation with optional array indices
     * @param expectedValueMap the map representing the expected Avro record
     * @param actualValueMap the map representing the actual Avro record
     * @return true if the nested values match, false otherwise
     */
    private static boolean matchNestedValue(
            String nestedKey, Map<String, Object> expectedValueMap, Map<String, Object> actualValueMap) {
        var keys = nestedKey.split("\\.");
        Object expected = expectedValueMap;
        Object actual = actualValueMap;

        try {
            for (var key : keys) {
                if (key.contains("[")) {
                    LOGGER.debug("Found an array key {}", key);
                    var arrayKey = key.substring(0, key.indexOf("["));
                    var indexStr = key.substring(key.indexOf("[") + 1, key.indexOf("]"));
                    var index = Integer.parseInt(indexStr);

                    var expectedList = (List<Object>) ((Map<String, Object>) expected).get(arrayKey);
                    var actualList = (List<Object>) ((Map<String, Object>) actual).get(arrayKey);

                    if (index >= 0 && index < expectedList.size() && index < actualList.size()) {
                        expected = expectedList.get(index);
                        actual = actualList.get(index);
                    } else {
                        LOGGER.debug("Index out of bounds, unable to match nested value from array key {}", key);
                        return false;
                    }
                } else {
                    expected = ((Map<String, Object>) expected).get(key);
                    actual = ((Map<String, Object>) actual).get(key);
                }
            }

            return Objects.equals(expected, actual);
        } catch (ClassCastException | NumberFormatException | IndexOutOfBoundsException e) {
            LOGGER.error("Error traversing nested objects", e);
            return false;
        }
    }

    /**
     * Checks if a specified value matches with the value at a nested key in a map.
     *
     * <p>This method is useful for validating specific nested fields in Avro records. It traverses the nested structure
     * using dot notation and performs string comparison of the final value.
     *
     * @param expectedValue the value expected at the specified nested key
     * @param nestedKey the nested key within the map (supports dot notation)
     * @param actualValueMap the map representing the actual Avro record
     * @return true if the value at the specified nested key matches the expected value, false otherwise
     */
    private static boolean matchNestedValue(
            String expectedValue, String nestedKey, Map<String, Object> actualValueMap) {
        var keys = nestedKey.split("\\.");
        Object actual = actualValueMap;

        try {
            for (var key : keys) {
                actual = ((Map<String, Object>) actual).get(key);
            }

            return actual != null && expectedValue.equals(String.valueOf(actual));
        } catch (ClassCastException e) {
            LOGGER.error("Error traversing nested objects", e);
            return false;
        }
    }

    /**
     * Compares values for a given key in two maps.
     *
     * <p>This method is used for comparing simple fields in Avro records. It checks if both maps contain the key and if
     * the values are equal.
     *
     * @param key the key whose values are to be compared in both maps
     * @param expectedValueMap the map representing the expected Avro record
     * @param actualValueMap the map representing the actual Avro record
     * @return true if the values for the given key are equal in both maps, false otherwise
     */
    private static boolean matchValue(
            String key, Map<String, Object> expectedValueMap, Map<String, Object> actualValueMap) {
        return expectedValueMap.containsKey(key) && Objects.equals(expectedValueMap.get(key), actualValueMap.get(key));
    }

    // ===========================================
    // HEADER EXTRACTION METHODS
    // ===========================================

    /**
     * Extracts headers from a JSON string and returns them as a Map.
     *
     * <p>This method parses a JSON string looking for a "headers" object and converts it to a Map&lt;String, String&gt;
     * for easier manipulation. All header values are converted to strings regardless of their original type.
     *
     * @param jsonString the JSON string containing headers
     * @return a Map containing the headers, or null if no headers object is found
     * @throws JsonSyntaxException if the JSON string is malformed
     * @throws IllegalArgumentException if jsonString is null
     */
    public static Map<String, String> getHeadersMap(@NonNull String jsonString) {
        Objects.requireNonNull(jsonString, "JSON string cannot be null");

        var gson = new Gson();
        var jsonObject = gson.fromJson(jsonString, JsonObject.class);

        var headersObject = jsonObject.getAsJsonObject("headers");
        if (headersObject == null) {
            LOGGER.error("No headers object found in the JSON string");
            return null;
        }

        var headersMap = new HashMap<String, String>();
        for (var key : headersObject.keySet()) {
            headersMap.put(key, headersObject.get(key).getAsString());
        }

        return headersMap;
    }

    /**
     * Converts a JSON element to an Avro-compatible object based on the provided schema.
     *
     * <p>This method handles the conversion of various JSON types to their corresponding Avro representations,
     * including complex types like unions, arrays, maps, and records. It also handles Avro logical types such as dates,
     * timestamps, and decimals.
     *
     * @param jsonElement the JSON element to convert
     * @param fieldSchema the Avro schema of the field to which this JSON element corresponds
     * @return the Avro-compatible representation of the JSON element
     * @throws ProducerException if the conversion fails or encounters an unsupported type
     * @throws IllegalArgumentException if any parameter is null
     */
    public static Object convertJsonToAvro(@NonNull JsonElement jsonElement, @NonNull Schema fieldSchema) {
        Objects.requireNonNull(jsonElement, "JSON element cannot be null");
        Objects.requireNonNull(fieldSchema, "Field schema cannot be null");

        LOGGER.debug("Converting a JSON element {} to Avro", jsonElement);

        return switch (fieldSchema.getType()) {
            case UNION -> handleUnionType(jsonElement, fieldSchema);
            case MAP -> convertJsonMapToAvroMap(jsonElement.getAsJsonObject(), fieldSchema.getValueType());
            case ARRAY -> convertJsonArrayToArray(jsonElement.getAsJsonArray(), fieldSchema);
            case BYTES -> handleBytesType(jsonElement, fieldSchema);
            default -> convertJsonPrimitiveToAvro(jsonElement, fieldSchema);
        };
    }

    /**
     * Handles conversion of UNION schema types.
     *
     * <p>For UNION types, this method picks the first non-null schema type and converts the JSON element using that
     * schema.
     *
     * @param jsonElement the JSON element to convert
     * @param fieldSchema the UNION schema
     * @return the converted Avro object, or null if all union types are null
     */
    private static Object handleUnionType(JsonElement jsonElement, Schema fieldSchema) {
        LOGGER.debug("Found a UNION Type in the field schema");

        for (var schema : fieldSchema.getTypes()) {
            if (schema.getType() != Schema.Type.NULL) {
                LOGGER.debug("Found a non-null union type, converting it into Avro");
                return convertJsonToAvro(jsonElement, schema);
            }
        }

        return null; // All types in UNION are NULL
    }

    /**
     * Handles conversion of BYTES schema types, including decimal logical types.
     *
     * @param jsonElement the JSON element to convert
     * @param fieldSchema the BYTES schema
     * @return the converted byte representation
     */
    private static Object handleBytesType(JsonElement jsonElement, Schema fieldSchema) {
        LOGGER.debug("Converting to BYTES");

        if (fieldSchema.getLogicalType() instanceof LogicalTypes.Decimal decimalLogicalType) {
            LOGGER.debug("Converting a logical type decimal to BYTE");
            return ByteBuffer.wrap(new BigDecimal(jsonElement.getAsString())
                    .setScale(decimalLogicalType.getScale(), RoundingMode.HALF_UP)
                    .round(new MathContext(decimalLogicalType.getPrecision()))
                    .unscaledValue()
                    .toByteArray());
        } else {
            return jsonElement.getAsByte();
        }
    }

    /**
     * Converts JSON primitive types to their corresponding Avro representations.
     *
     * <p>This method handles the conversion of basic JSON types (string, number, boolean) to Avro types, including
     * special handling for logical types like dates and timestamps.
     *
     * @param jsonElement the JSON element to convert
     * @param schemaType the target Avro schema type
     * @return the converted Avro object
     * @throws ProducerException if the conversion fails
     */
    private static Object convertJsonPrimitiveToAvro(JsonElement jsonElement, Schema schemaType) {
        return switch (schemaType.getType()) {
            case RECORD -> convertJsonObjectToRecord(jsonElement.getAsJsonObject(), schemaType);
            case MAP -> convertJsonMapToAvroMap(jsonElement.getAsJsonObject(), schemaType);
            case STRING -> jsonElement.getAsString();
            case BOOLEAN -> jsonElement.getAsBoolean();
            case INT -> handleIntType(jsonElement, schemaType);
            case LONG -> handleLongType(jsonElement, schemaType);
            case FLOAT -> jsonElement.getAsFloat();
            case DOUBLE -> jsonElement.getAsDouble();
            default -> throw new ProducerException("Unsupported complex type: " + schemaType);
        };
    }

    /**
     * Handles conversion of INT schema types, including logical types like date and time-millis.
     *
     * @param jsonElement the JSON element to convert
     * @param schemaType the INT schema
     * @return the converted integer value
     */
    private static Object handleIntType(JsonElement jsonElement, Schema schemaType) {
        var logicalType = schemaType.getLogicalType();

        if (logicalType instanceof LogicalTypes.Date) {
            LOGGER.debug("Converting a logical type date string to INT");
            try {
                return convertDateStringToDateInt(jsonElement.getAsString());
            } catch (Exception e) {
                LOGGER.error("Failed to convert date string to date int", e);
                throw new RuntimeException(e);
            }
        } else if (logicalType instanceof LogicalTypes.TimeMillis) {
            LOGGER.debug("Converting a logical type time-millis string to INT");
            try {
                return convertTimeStringToTimeMillis(jsonElement.getAsString());
            } catch (Exception e) {
                LOGGER.error("Failed to convert time string to time-millis", e);
                throw new RuntimeException(e);
            }
        } else {
            return jsonElement.getAsInt();
        }
    }

    /**
     * Handles conversion of LONG schema types, including logical types like timestamp-millis and timestamp-micros.
     *
     * @param jsonElement the JSON element to convert
     * @param schemaType the LONG schema
     * @return the converted long value
     */
    private static Object handleLongType(JsonElement jsonElement, Schema schemaType) {
        var logicalType = schemaType.getLogicalType();

        if (logicalType instanceof LogicalTypes.TimestampMillis) {
            LOGGER.debug("Converting a logical type timestamp-millis to LONG");
            try {
                return convertDateStringToTimestamp(jsonElement.getAsString());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } else if (logicalType instanceof LogicalTypes.TimestampMicros) {
            LOGGER.debug("Converting a logical type timestamp-micros to LONG");
            try {
                return convertDateStringToTimestampMicros(jsonElement.getAsString());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            return jsonElement.getAsLong();
        }
    }

    /**
     * Converts a JsonObject to an Avro-compatible map.
     *
     * <p>This method is essential for handling map data types in Avro schemas. It recursively converts each value in
     * the map according to the value schema.
     *
     * @param jsonObject the JsonObject to be converted
     * @param valueSchema the schema of the values in the Avro map
     * @return a map in Avro-compatible format
     */
    private static Map<String, Object> convertJsonMapToAvroMap(JsonObject jsonObject, Schema valueSchema) {
        LOGGER.debug("Converting a JSON map to Avro MAP");
        LOGGER.debug("Value schema {}", valueSchema);

        var avroMap = new HashMap<String, Object>();
        for (var entry : jsonObject.entrySet()) {
            var key = entry.getKey();
            var element = entry.getValue();
            var avroValue = convertJsonToAvro(element, valueSchema);
            avroMap.put(key, avroValue);
        }

        return avroMap;
    }

    /**
     * Converts a JsonObject to an Avro GenericRecord.
     *
     * <p>This method is crucial for handling complex types represented as JSON objects in Avro records. It maps each
     * field in the JSON object to the corresponding field in the Avro record schema.
     *
     * @param jsonObject the JsonObject to be converted
     * @param recordSchema the schema of the Avro record
     * @return an Avro GenericRecord corresponding to the JsonObject
     */
    private static GenericRecord convertJsonObjectToRecord(JsonObject jsonObject, Schema recordSchema) {
        LOGGER.debug("Converting a JSON object to Avro RECORD");

        var record = new GenericData.Record(recordSchema);
        for (var entry : jsonObject.entrySet()) {
            var key = entry.getKey();
            var element = entry.getValue();
            var field = recordSchema.getField(key);

            if (field != null) {
                record.put(key, convertJsonToAvro(element, field.schema()));
            }
        }

        return record;
    }

    /**
     * Converts a JsonArray to an Avro-compatible array.
     *
     * <p>This method handles conversion of JSON arrays to Avro arrays, considering the schema of array elements. It can
     * handle arrays of records, maps, and primitive types.
     *
     * @param jsonArray the JsonArray to be converted
     * @param elementSchema the schema of the elements in the Avro array
     * @return an Avro-compatible array
     */
    private static GenericData.Array<Object> convertJsonArrayToArray(JsonArray jsonArray, Schema elementSchema) {
        LOGGER.debug("Converting a JSON array to Avro ARRAY");

        var avroArray = new GenericData.Array<Object>(jsonArray.size(), elementSchema);
        for (var element : jsonArray) {
            if (element.isJsonObject()) {
                LOGGER.debug(
                        "Found a JSON object inside the array! (elementSchema type {} & elementSchema.getElementType {})",
                        elementSchema.getType(),
                        elementSchema.getElementType().getType());

                var elementType = elementSchema.getElementType().getType();
                if (elementType == Schema.Type.RECORD) {
                    LOGGER.debug("Found a RECORD in an ARRAY!");
                    avroArray.add(convertJsonObjectToRecord(element.getAsJsonObject(), elementSchema.getElementType()));
                } else if (elementType == Schema.Type.MAP) {
                    LOGGER.debug("Found a MAP in an ARRAY");
                    avroArray.add(convertJsonMapToAvroMap(element.getAsJsonObject(), elementSchema.getElementType()));
                }
            } else {
                avroArray.add(convertJsonToAvro(element, elementSchema.getElementType()));
            }
        }

        return avroArray;
    }

    // ===========================================
    // DATE AND TIME CONVERSION METHODS
    // ===========================================

    /**
     * Recursively converts date strings to Unix timestamps throughout a nested map structure.
     *
     * <p>This method traverses a map representation of JSON input, identifies fields containing date strings, and
     * converts them to Unix timestamps. It handles nested maps and lists recursively.
     *
     * @param jsonMap the map representation of the JSON input
     * @return a map with date fields converted to Unix timestamps
     */
    public static Map<String, Object> convertDatesToTimestamps(Map<String, Object> jsonMap) {
        Objects.requireNonNull(jsonMap, "JSON map cannot be null");

        for (var entry : jsonMap.entrySet()) {
            var value = entry.getValue();

            if (value instanceof String dateString) {
                if (isDateString(dateString)) {
                    try {
                        var timestamp = convertDateStringToTimestamp(dateString);
                        jsonMap.put(entry.getKey(), timestamp);
                    } catch (ParseException e) {
                        LOGGER.error("An error happened while converting Date to Timestamps", e);
                    }
                }
            } else if (value instanceof Map<?, ?> nestedMap) {
                // convertDatesToTimestamps((Map<String, Object>) nestedMap);
            } else if (value instanceof List<?> list) {
                for (var item : list) {
                    if (item instanceof Map<?, ?> mapItem) {

                        // convertDatesToTimestamps(<(Map<String, Object>) mapItem);
                    }
                }
            }
        }

        return jsonMap;
    }

    /**
     * Determines if a string matches one of the supported date formats.
     *
     * <p>This method tests a string against multiple date format patterns to determine if it represents a valid
     * date/timestamp. It uses strict parsing to avoid false positives.
     *
     * @param dateString the string to check
     * @return true if the string matches a supported date format, false otherwise
     */
    public static boolean isDateString(String dateString) {
        if (dateString == null) {
            return false;
        }

        for (var pattern : SUPPORTED_DATE_PATTERNS) {
            var dateFormat = new SimpleDateFormat(pattern);
            dateFormat.setLenient(false);

            try {
                dateFormat.parse(dateString);
                return true;
            } catch (ParseException e) {
                // Continue trying other patterns
            }
        }

        return false;
    }

    /**
     * Converts a date string to an integer representing days since epoch (1970-01-01).
     *
     * <p>This method is used for Avro's date logical type, which represents dates as the number of days since the Unix
     * epoch.
     *
     * @param dateString the date string to convert, in format "yyyy-MM-dd"
     * @return the number of days since epoch (January 1, 1970)
     * @throws IllegalArgumentException if the date string cannot be parsed
     */
    public static int convertDateStringToDateInt(String dateString) {
        Objects.requireNonNull(dateString, "Date string cannot be null");

        try {
            var date = LocalDate.parse(dateString);
            return (int) date.toEpochDay();
        } catch (DateTimeParseException e) {
            LOGGER.error("The provided date was in an incorrect format: {}", dateString);
            throw new IllegalArgumentException("Invalid date format. Expected format is yyyy-MM-dd", e);
        }
    }

    /**
     * Converts a time string to an integer representing milliseconds since midnight.
     *
     * <p>This method is used for Avro's time-millis logical type, which represents times as the number of milliseconds
     * since midnight.
     *
     * @param timeString the time string to convert, in format "HH:mm:ss"
     * @return the number of milliseconds since midnight
     * @throws IllegalArgumentException if the time string cannot be parsed
     */
    public static int convertTimeStringToTimeMillis(String timeString) {
        Objects.requireNonNull(timeString, "Time string cannot be null");

        try {
            var time = LocalTime.parse(timeString);
            return (int) (time.toNanoOfDay() / 1_000_000);
        } catch (DateTimeParseException e) {
            LOGGER.error("The provided time was in an incorrect format: {}", timeString);
            throw new IllegalArgumentException("Invalid time format. Expected format is HH:mm:ss", e);
        }
    }

    /**
     * Converts a date string to a Unix timestamp in milliseconds.
     *
     * <p>This method handles date strings in ISO format with optional milliseconds and converts them to Unix
     * timestamps. It supports multiple input formats and provides robust error handling.
     *
     * @param dateString the date string to convert (ISO format with Z suffix)
     * @return the Unix timestamp in milliseconds
     * @throws ParseException if the date string cannot be parsed
     */
    public static long convertDateStringToTimestamp(String dateString) throws ParseException {
        Objects.requireNonNull(dateString, "Date string cannot be null");

        try {
            var instant = Instant.from(TIMESTAMP_FORMATTER_WITH_MS.parse(dateString));
            return instant.toEpochMilli();
        } catch (DateTimeParseException e) {
            LOGGER.error("The provided date was in an incorrect format: {}", e);
            throw new ParseException(dateString, 0);
        }
    }

    /**
     * Converts a date string to a Unix timestamp in microseconds.
     *
     * <p>This method handles date strings in ISO format with optional microseconds and converts them to Unix timestamps
     * in microsecond precision. This is used for Avro's timestamp-micros logical type.
     *
     * @param dateString the date string to convert (ISO format with Z suffix)
     * @return the Unix timestamp in microseconds
     * @throws ParseException if the date string cannot be parsed
     */
    public static long convertDateStringToTimestampMicros(String dateString) throws ParseException {
        Objects.requireNonNull(dateString, "Date string cannot be null");

        try {
            var instant = Instant.from(TIMESTAMP_FORMATTER_WITH_MICROS.parse(dateString));
            return instant.getEpochSecond() * 1_000_000 + instant.getNano() / 1_000;
        } catch (DateTimeParseException e) {
            LOGGER.error("The provided date was in an incorrect format: {}", e);
            throw new ParseException(dateString, 0);
        }
    }

    // ===========================================
    // UTILITY METHODS
    // ===========================================

    /**
     * Constructs a comma-separated string of keys from a map.
     *
     * <p>This method is helpful for logging and debugging purposes, especially to list all keys in a map in a readable
     * format.
     *
     * @param map the map whose keys are to be listed
     * @return a string containing all keys in the map, separated by commas
     */
    private static String getKeySets(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        final int KEYSET_TRAILING_LENGTH = ", ".length(); // Length of ", "
        var sb = new StringBuilder();
        map.keySet().forEach(key -> sb.append(key).append(", "));

        if (!sb.isEmpty()) {
            sb.setLength(sb.length() - KEYSET_TRAILING_LENGTH); // Remove trailing ", "
        }

        return sb.toString();
    }
}
