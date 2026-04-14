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

/** Constants defining the configuration file API */
public final class ConfigConstants {

    private ConfigConstants() {}

    /*
     * Match methods
     */
    public static final String MATCH_METHOD = "matchMethod";
    public static final String METHOD_MATCH_FILE = "methodMatchFile";
    public static final String METHOD_MATCH_KEY_FILE = "methodMatchKeyValue";
    public static final String METHOD_MATCH_KEY_VALUE = "methodMatchKey";
    public static final String MATCH_KEY_VALUE = "matchKeyValue";
    public static final String MATCH_KEYS_VALUES = "matchKeysValues";

    public static final String METHOD_MATCH_XML = "methodMatchXML";
    public static final String METHOD_MATCH_XPATH = "methodMatchXPath";
    public static final String METHOD_FIELDS_TO_MATCH = "methodFieldsToMatch";
    public static final String METHOD_RECORD_KEY_MATCH = "methodRecordKeyMatch";

    public static final String MATCH_FILE_PATH = "matchFile";
    public static final String MATCH_FILES_PATHS = "matchFiles";

    public static final String EXCLUDED_XML_ELEMENTS = "excludedXMLElements";
    public static final String XPATHS_TO_MATCH = "xpathsExpressions";
    public static final String FIELD_TO_MATCH_VALUE = "fieldToMatchValue";
    public static final String FIELD_TO_MATCH_KEY = "fieldToMatchKey";
    public static final String FIELD_TO_MATCH_LINE = "fieldToMatchLine";
    public static final String FIELD_TO_MATCH_FROM = "fieldToMatchFrom";
    public static final String FIELD_TO_MATCH_TO = "fieldsToMatchTo";
    public static final String EXCLUDE_FIELDS_TO_MATCH = "excludeFieldsToMatch";

    public static final String TOPIC_NAME = "topicName";

    public static final String TOPIC_TYPE_AVRO = "avro";
    public static final String TOPIC_TYPE_RAW = "raw";
    public static final String QUEUE_NAME = "queueName";
    public static final String FILES = "files";
    public static final String SCHEMA = "schema";
    public static final String KEY = "key";
    public static final String PAYLOAD_PATH = "payloadPath";
    public static final String PAYLOAD = "payload";
    public static final String HEADERS = "headers";
    public static final String HEADER_KEY = "key";
    public static final String HEADER_VALUE = "value";
    public static final String CONSUMER_DELTA_TIME = "consumerDeltaTime";

    public static final String SCHEMA_REGISTRY_URL = "schema-registry-url";
    public static final String SCHEMA_VERSION = "schema-version";

    // IBM MQ config
    public static final String DATA_TABLE_HEADER_FILE = "headerFile";

    public static final String IBMQ_BROKER_URL = "IBMQ_BROKER_URL";
    public static final String IBMQ_BROKER_PORT = "IBMQ_BROKER_PORT";
    public static final String IMBQ_BROKER_CHANNEL = "IBMQ_BROKER_CHANNEL";
    public static final String IBMQ_USERNAME = "IBMQ_USERNAME";
    public static final String IBMQ_PASSWORD = "IBMQ_PASSWORD";

    // Basic auth Config
    public static final String BASIC_AUTH_CREDENTIAL_SOURCE = "basic.auth.credentials.source";
    public static final String BASIC_AUTH_CREDENTIAL_USER_INFO = "basic.auth.user.info";
    public static final Integer SCHEMA_REGISTRY_DEFAULT_CACHE_CAPACITY = 50;
    public static final int DELTA_TIME = 20; // Default delta time is 20s
    public static final int TO_MILLISECONDS = 1000;
    public static final Long DEFAULT_READ_TIMEOUT = 10L;
    public static final int POLL_MILLIS = 100;
    public static final int BUFFER_TIME = 5000;
    public static final String ERROR_UNEXPECTED_VALUE_TYPE = "Unexpected value type";

    /*
    DataTable variable constants
    */

    // Background object declarations

    // Topic related
    public static final String DATA_TABLE_TOPIC_NAME = "topicName";
    public static final String DATA_TABLE_TOPIC_ALIAS = "topicAlias";
    public static final String DATA_TABLE_FILE = "file";
    public static final String DATA_TABLE_FILES = "files";
    public static final String DATA_TABLE_NAMESPACE = "namespace";
    public static final String DATA_TABLE_NAMESPACE_ALIAS = "namespaceAlias";
    public static final String DATA_TABLE_RECORD_KEY = "recordKey";
    public static final String DATA_TABLE_RECORDS_KEYS_VALUES = "recordsKeysValues";

    public static final String DATA_TABLE_RECORD_KEY_VALUE = "recordKeyValue";
    public static final String DATA_TABLE_READ_TIMEOUT = "consumerReadTimeout";
    public static final String DATA_TABLE_TOPIC_TYPE = "topicType";

    public static final String DATA_TABLE_EXPECTED_RECORD_KEY = "expectedRecordKey";
    public static final String EXPECTED_RECORD_KEY = "expectedRecordKey";

    public static final String DATA_TABLE_EXPECTED_RECORDS_COUNT = "expectedRecordsCount";
    public static final String EXPECTED_RECORDS_COUNT = "expectedRecordsCount";

    public static final String DATA_TABLE_CONSUMER_DELTA_TIME = "consumerDeltaTime";

    // schema
    public static final String DATA_TABLE_SCHEMA_NAME = "schemaName";
    public static final String DATA_TABLE_SCHEMA_VERSION = "schemaVersion";
    public static final String DATA_TABLE_SCHEMA_ALIAS = "schemaAlias";

    // Queue related
    public static final String DATA_TABLE_QUEUE_NAME = "queueName";
    public static final String DATA_TABLE_QUEUE_MANAGER = "queueManager";
    public static final String DATA_TABLE_QUEUE_CHANNEL = "channel";
    public static final String DATA_TABLE_QUEUE_ALIAS = "queueAlias";

    // Field matcher

    public static final String DATA_TABLE_FIELD_TO_MATCH_LINE = "line";
    public static final String DATA_TABLE_FIELD_TO_MATCH_FROM = "from";
    public static final String DATA_TABLE_FIELD_TO_MATCH_TO = "to";

    public static final String DATA_TABLE_FIELD_TO_MATCH_KEY = "key";
    public static final String DATA_TABLE_FIELD_TO_MATCH_VALUE = "value";

    public static final String DATA_TABLE_FIELD_TO_MATCH_EXCLUDE_ELEMENTS = "excludedElements";

    public static final String DATA_TABLE_FIELD_TO_MATCH_XPATH = "xpathExpressions";
    public static final String DATA_TABLE_FIELD_TO_MATCH_EXCLUDED_KEYS = "excludedKeys";

    // CFT
    public static final String DATA_TABLE_CFT_HOST = "host";
    public static final String DATA_TABLE_CFT_PORT = "port";
    public static final String DATA_TABLE_CFT_USERNAME = "username";
    public static final String DATA_TABLE_CFT_PASSWORD = "password";
    public static final String DATA_TABLE_CFT_BASE_PATH = "basePath";
    public static final String DATA_TABLE_CFT_TYPE = "type";
    public static final String DATA_TABLE_CFT_ALIAS = "cftAlias";
    public static final String DATA_TABLE_CFT_FILE = "sourceFile";
    public static final String DATA_TABLE_CFT_DESTINATION_FILE = "destinationFile";

    // Script executions
    public static final String DATA_TABLE_SCRIPT_PATH = "scriptPath";
    public static final String DATA_TABLE_SCRIPT_ARGS = "scriptArgs";

    public static final String DEFAULT_SSH_PORT = "22";
}
