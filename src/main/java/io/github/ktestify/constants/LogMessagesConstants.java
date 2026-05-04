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
package io.github.ktestify.constants;

public final class LogMessagesConstants {

    public static final String MESSAGE_CONSUMER_SUBSCRIBED_TO_TOPIC = "Subscribed to topic {}.";
    public static final String MESSAGE_CONSUMER_DELTA_TIME_FROM_DATATABLE =
            "Retrieved a consumer delta time from the DataTable : {} seconds.";
    public static final String MESSAGE_CONSUMER_DELTA_TIME_IN_TIMESTAMP =
            "Converted consumer delta time to timestamp, {} ms.";
    public static final String MESSAGE_CONSUMER_RECORD_ALREADY_MATCHED =
            "{} has already been matched, skipping to the next record.";
    public static final String MESSAGE_CONSUMER_RECORD_NOT_MATCHED_YET =
            "{} has not been matched yet, using this record for the matching process.";
    public static final String MESSAGE_CONSUMER_OFFSET_FOR_TIMESTAMP = "Offset for timestamp {}.";
    public static final String MESSAGE_CONSUMER_OFFSETS_NULL_SEEK_TO_LATEST =
            "Offset for timestamp on partition {} was null, seeking to the latest ...";
    public static final String MESSAGE_CONSUMER_SEEKING_TO_OFFSET = "Seeking to offset for time with timestamp {}.";
    public static final String MESSAGE_CONSUMER_RETRIEVED_OFFSETS = "Retrieved offsets for time with timestamp {}.";
    public static final String MESSAGE_CONSUMER_GOT_RECORD_WITH_KEY_AND_VALUE = "Got a record with key {} \n{}";
    public static final String MESSAGE_CONSUMER_NO_DELTA_TIME_FOUND =
            "No consumer delta time has been found in DataTable, defaulting to {} ms..";
    public static final String MESSAGE_CONSUMER_RECORDS_IN_TOPIC_NOT_EMPTY = "Records in topic {} are not empty.";
    public static final String MESSAGE_CONSUMER_RECORD_DO_NOT_MATCHES_EXPECTED_KEY =
            "Found a record with key {} but it does not match the expected key {}.";
    public static final String MESSAGE_CONSUMER_RECORD_MATCHES_EXPECTED_KEY =
            "Found a record that matches the expected key {}.";

    public static final String ERROR_PARSING_CONSUMER_DELTA_TIME =
            "Failed to parse consumer delta time, defaulting to {} ms";

    public static final String AVRO_UTILS_DATE_CONVERSION_INFO = "Converting dates to timestamps";
    public static final String AVRO_UTILS_VALUES_STRICTLY_MATCH = "Values strictly match";
    public static final String AVRO_UTILS_RECORD_MISMATCH =
            "Record mismatch, either one of the record is null or their size does not match";
    public static final String AVRO_UTILS_RECORD_MISMATCH_DETAILS = "{} value map details : size {}, keySet {}";
    public static final String AVRO_UTILS_RECORD_MISSING_KEY = "Missing key {} in actual value map, expected key {}";
    public static final String AVRO_UTILS_NESTED_OBJECT_FOUND = "Found a nested object ! (nestedObjectType : {})";
    public static final String AVRO_UTILS_NESTED_OBJECT_NOT_MATCH = "Nested object does not match";
    public static final String AVRO_UTILS_VALUES_MISMATCH_SHOULD_BE_NULL =
            "Value mismatch for key {} values should be null but found {} in actual value";
    public static final String AVRO_UTILS_VALUES_MISMATCH_WITH_CLASS_NAMES =
            "Value mismatch for key {} values does not match expected {} (class {}), but got {} (class {})";
    public static final String AVRO_UTILS_DEEP_EQUALS_WITH_EXCLUDED_KEYS =
            "Deep equals with excluded keys : KeySet[{}]";
    public static final String AVRO_UTILS_EXPECTED_KEY_NESTED_INFO = "Expected key is in a nested object key {}";
    public static final String AVRO_UTILS_EXPECTED_KEY_CONTAINED_IN_MAIN_OBJECT =
            "Expected key {} is contained in the main object";
    public static final String AVRO_UTILS_EXPECTED_RECORD_NULL = "Expected records cannot be null.";
    public static final String AVRO_UTILS_ACTUAL_RECORD_NULL = "Actual records cannot be null.";

    private LogMessagesConstants() {}
}
