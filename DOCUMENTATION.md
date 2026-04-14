
# KTestify - Kafka Testing Framework Documentation

**Version:** 0.2.51-SNAPSHOT  
**Last Updated:** January 11, 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Components](#core-components)
4. [Configuration](#configuration)
5. [Producers](#producers)
6. [Consumers](#consumers)
7. [Dynamic Variables](#dynamic-variables)
8. [Utilities](#utilities)
9. [Exception Handling](#exception-handling)
10. [Usage Examples](#usage-examples)

---

## Overview

KTestify is a comprehensive Kafka testing framework designed to simplify testing of Kafka-based applications. It provides robust support for:

- **Avro and Raw message formats**
- **Producer and Consumer abstractions**
- **Dynamic variable injection**
- **Message matching and comparison**
- **Schema registry integration**
- **Flexible configuration management**

The framework is built on Java 17 and leverages Lombok for code reduction, SLF4J for logging, and Apache Kafka clients for Kafka interactions.

---

## Architecture

### Package Structure

```
io.github.ktestify
├── Core.java                          # Main entry point (placeholder)
├── constants/                         # Configuration and logging constants
│   ├── ConfigConstants.java
│   └── LogMessagesConstants.java
├── exceptions/                        # Custom exceptions
│   ├── ConsumerException.java
│   └── ProducerException.java
├── io/                                # I/O operations
│   ├── core/                          # Abstract producer/consumer
│   │   ├── AbstractConsumer.java
│   │   └── AbstractProducer.java
│   ├── inputs/                        # Dynamic variable processing
│   │   ├── DynamicVariable.java
│   │   ├── DynamicVariableFactory.java
│   │   ├── DynamicVariableProcessor.java
│   │   └── types/
│   │       ├── DateVariable.java
│   │       ├── EnvironmentVariable.java
│   │       ├── RandomVariable.java
│   │       └── TimestampVariable.java
│   └── kafka/                         # Kafka-specific implementations
│       ├── AbstractKafkaConsumer.java
│       ├── AbstractKafkaProducer.java
│       ├── ProducerContext.java
│       └── impl/
│           ├── AvroKafkaProducer.java
│           └── RawKafkaProducer.java
├── models/                            # Domain models
│   ├── MatchedRecord.java
│   └── Topic.java
└── utils/                             # Utility classes
    ├── FileUtils.java
    ├── StringDiffUtils.java
    └── serdes/
        └── AvroUtils.java
```

---

## Core Components

### 1. Core Class

**Location:** `ktestify.io.github.Core`

Currently a placeholder class that serves as the main entry point for the framework.

```java
public class Core {
}
```

### 2. Topic Model

**Location:** `models.ktestify.io.github.Topic`

Represents a Kafka topic with namespace support and type differentiation.

**Features:**
- Topic name and alias
- Namespace support
- Type classification (INPUT/OUTPUT)
- Namespaced topic generation

**Usage:**
```java
Topic topic = new Topic();
topic.setTopicName("orders");
topic.setTopicType(Topic.Type.INPUT);
topic.setTopicNamespace(new Topic.TopicNamespace("prod", "production"));

String fullTopic = topic.getNamespacedTopic(); // "prod.orders"
```

### 3. MatchedRecord Model

**Location:** `models.ktestify.io.github.MatchedRecord`

Immutable record representing a matched Kafka message for tracking purposes.

**Fields:**
- `topic`: Topic name
- `partition`: Partition number
- `offset`: Message offset
- `key`: Record key
- `timestamp`: Record timestamp
- `processedTime`: Processing timestamp

**Usage:**
```java
ConsumerRecord<K, V> record = // ... from Kafka
MatchedRecord matched = MatchedRecord.fromConsumerRecord(record);
```

---

## Configuration

### ConfigConstants

**Location:** `constants.ktestify.io.github.ConfigConstants`

Centralized configuration constants for the framework.

#### Key Constants

**Match Methods:**
```java
MATCH_METHOD = "matchMethod"
METHOD_MATCH_FILE = "methodMatchFile"
METHOD_MATCH_KEY_FILE = "methodMatchKeyValue"
METHOD_MATCH_KEY_VALUE = "methodMatchKey"
METHOD_MATCH_XML = "methodMatchXML"
METHOD_MATCH_XPATH = "methodMatchXPath"
METHOD_FIELDS_TO_MATCH = "methodFieldsToMatch"
METHOD_RECORD_KEY_MATCH = "methodRecordKeyMatch"
```

**Topic Configuration:**
```java
TOPIC_NAME = "topicName"
TOPIC_TYPE_AVRO = "avro"
TOPIC_TYPE_RAW = "raw"
SCHEMA_REGISTRY_URL = "schema-registry-url"
SCHEMA_VERSION = "schema-version"
```

**Timing:**
```java
DELTA_TIME = 20              // Default delta time in seconds
TO_MILLISECONDS = 1000
DEFAULT_READ_TIMEOUT = 10L   // Default read timeout
POLL_MILLIS = 100           // Polling interval
BUFFER_TIME = 5000          // Buffer time
```

**DataTable Variables:**
```java
DATA_TABLE_TOPIC_NAME = "topicName"
DATA_TABLE_TOPIC_ALIAS = "topicAlias"
DATA_TABLE_FILE = "file"
DATA_TABLE_SCHEMA_NAME = "schemaName"
DATA_TABLE_RECORD_KEY = "recordKey"
DATA_TABLE_READ_TIMEOUT = "consumerReadTimeout"
DATA_TABLE_EXPECTED_RECORD_KEY = "expectedRecordKey"
DATA_TABLE_EXPECTED_RECORDS_COUNT = "expectedRecordsCount"
```

### LogMessagesConstants

**Location:** `constants.ktestify.io.github.LogMessagesConstants`

Centralized logging messages for consistency and internationalization support.

#### Categories

**Producer Messages:**
```java
MESSAGE_RAW_PRODUCER_CREATED_TOPIC
MESSAGE_AVRO_PRODUCER_CREATED
MESSAGE_MESSAGE_SENT_IN_OBJECT_NAME
```

**Consumer Messages:**
```java
MESSAGE_CONSUMER_SUBSCRIBED_TO_TOPIC
MESSAGE_CONSUMER_DELTA_TIME_FROM_DATATABLE
MESSAGE_CONSUMER_RECORD_MATCHES_EXPECTED_KEY
MESSAGE_CONSUMER_DOES_MATCH
```

**Error Messages:**
```java
ERROR_EMPTY_FILE
ERROR_CONSUMER_THREAD_EXCEPTION
ERROR_MISSING_TOPIC_IN_DATA_TABLE
ERROR_MISSING_MATCH_METHOD
```

---

## Producers

### AbstractProducer

**Location:** `core.io.ktestify.io.github.AbstractProducer`

Base class for all producer implementations.

**Key Features:**
- Properties-based configuration
- Abstract `produce()` method for implementation-specific logic

### AbstractKafkaProducer

**Location:** `kafka.io.ktestify.io.github.AbstractKafkaProducer`

Kafka-specific producer abstraction providing common functionality.

**Key Features:**
- Context-based initialization
- Header support
- Payload resolution (inline or file-based)
- Schema resolution
- Record building with headers

**Constructor Options:**
```java
// Simple constructor
AbstractKafkaProducer(Topic topic, Map<String, String> properties, 
                      Producer<K, V> producer, File file)

// Full constructor with all options
AbstractKafkaProducer(Topic topic, Map<String, String> properties,
                      Producer<K, V> producer, File file,
                      Map<String, String> headers, String payload,
                      File schemaFile, String schemaContent)

// Context-based constructor
AbstractKafkaProducer(ProducerContext<K, V> context)
```

**Protected Methods:**
```java
String resolvePayload()           // Resolves payload from inline or file
String resolveSchema()            // Resolves schema from content or file
ProducerRecord<K, V> buildRecord(K key, V value) // Builds record with headers
```

### ProducerContext

**Location:** `kafka.io.ktestify.io.github.ProducerContext`

Immutable context object encapsulating all producer configuration and dependencies.

**Builder Pattern:**
```java
ProducerContext<String, String> context = ProducerContext.<String, String>builder()
    .topic(topic)
    .properties(props)
    .producer(kafkaProducer)
    .payloadFile(new File("payload.json"))
    .recordKey("myKey")
    .headers(Map.of("header1", "value1"))
    .build();
```

**Validation:**
- Topic must be provided and valid
- Topic type must not be OUTPUT
- Properties must be provided
- Producer instance must be provided
- Files must exist and be non-empty

### RawKafkaProducer

**Location:** `impl.kafka.io.ktestify.io.github.RawKafkaProducer`

Producer for sending raw (String) messages to Kafka.

**Usage:**
```java
RawKafkaProducer producer = new RawKafkaProducer(
    topic, 
    properties, 
    kafkaProducer, 
    payloadFile,
    headers,
    null  // or inline payload string
);

producer.produce();
```

**Features:**
- String key and value
- Synchronous sending with `.get()`
- Exception handling for interruption and execution errors
- Metadata logging (topic, partition, offset)

### AvroKafkaProducer

**Location:** `impl.kafka.io.ktestify.io.github.AvroKafkaProducer`

Producer for sending Avro-serialized messages to Kafka.

**Usage:**
```java
AvroKafkaProducer producer = new AvroKafkaProducer(
    topic,
    properties,
    kafkaProducer,
    payloadFile,
    schemaFile
);

producer.produce();
```

**Features:**
- Automatic JSON to Avro conversion
- Schema parsing and validation
- GenericRecord creation
- Synchronous sending
- Comprehensive error handling

---

## Consumers

### AbstractConsumer

**Location:** `core.io.ktestify.io.github.AbstractConsumer`

Base class for all consumer implementations implementing `Callable<Boolean>`.

**Key Features:**
- Properties-based configuration
- Callable pattern for async execution
- Returns Boolean indicating success/failure

### AbstractKafkaConsumer

**Location:** `kafka.io.ktestify.io.github.AbstractKafkaConsumer`

Advanced Kafka consumer with intelligent record matching and offset management.

**Key Features:**
- Delta time-based offset seeking
- Record matching with custom logic
- Expected record key filtering
- Topic type validation (only OUTPUT topics)
- Automatic subscription and unsubscription
- Matched record tracking (future feature)

**Abstract Method:**
```java
public abstract boolean doesMatch(ConsumerRecord<K, V> outputRecord) 
    throws ConsumerException;
```

**Workflow:**
1. Subscribe to topic
2. Calculate delta time for offset
3. Seek to appropriate offset
4. Poll for records
5. Process and match records
6. Return result or throw exception on timeout

**Configuration:**
```java
Map<String, String> properties = new HashMap<>();
properties.put(DATA_TABLE_READ_TIMEOUT, "10000");  // 10 seconds
properties.put(CONSUMER_DELTA_TIME, "30");         // 30 seconds back
properties.put(EXPECTED_RECORD_KEY, "order-123");  // Optional key filter
properties.put(MATCH_METHOD, "methodMatchFile");   // Match method
```

**Delta Time Logic:**
- Reads from `CONSUMER_DELTA_TIME` property (in seconds)
- Converts to milliseconds
- Seeks to offset at (current time - delta)
- Falls back to default `DELTA_TIME` (20s) if not specified

---

## Dynamic Variables

Dynamic variables allow runtime value injection into payloads using a placeholder syntax: `{{variableName:format}}`

### DynamicVariable Interface

**Location:** `inputs.io.ktestify.io.github.DynamicVariable`

```java
public interface DynamicVariable {
    String getName();                    // Variable identifier
    String process(String value);        // Process with format
    default String process() {           // Process without format
        return process("");
    }
}
```

### DynamicVariableFactory

**Location:** `inputs.io.ktestify.io.github.DynamicVariableFactory`

Factory for registering and retrieving dynamic variables.

**Pre-registered Variables:**
- `date` - DateVariable
- `timestamp` - TimestampVariable
- `random` - RandomVariable
- `env` - EnvironmentVariable

**Methods:**
```java
registerVariable(DynamicVariable variable)  // Register custom variable
getVariable(String name)                    // Get variable by name
isRegistered(String name)                   // Check if registered
getRegisteredVariableNames()                // Get all names
clearRegisteredVariables()                  // Clear all
```

### DynamicVariableProcessor

**Location:** `inputs.io.ktestify.io.github.DynamicVariableProcessor`

Processes strings containing dynamic variable placeholders.

**Pattern:** `{{variableName:format}}`

**Usage:**
```java
DynamicVariableProcessor processor = new DynamicVariableProcessor();
String input = "Order created at {{timestamp:yyyy-MM-dd HH:mm:ss}}";
String output = processor.process(input);
// Output: "Order created at 2026-01-11 14:30:45"
```

**Static Method:**
```java
boolean containsDynamic = DynamicVariableProcessor.doesContainDynamicVariable(input);
```

### Built-in Variables

#### DateVariable

**Name:** `date`  
**Format:** Java DateTimeFormatter pattern (optional)  
**Default:** ISO_DATE format

**Examples:**
```
{{date}}                    → 2026-01-11
{{date:MM/dd/yyyy}}        → 01/11/2026
{{date:dd-MMM-yyyy}}       → 11-Jan-2026
```

#### TimestampVariable

**Name:** `timestamp`  
**Format:** Java DateTimeFormatter pattern (required for process(String))  
**Default:** ISO_LOCAL_DATE_TIME

**Examples:**
```
{{timestamp}}                           → 2026-01-11T14:30:45.123
{{timestamp:yyyy-MM-dd HH:mm:ss}}      → 2026-01-11 14:30:45
{{timestamp:HH:mm:ss}}                 → 14:30:45
```

#### RandomVariable

**Name:** `random`  
**Format:** `type:length` or just `length`  
**Types:** `uuid`, `str` (string), `num` (number)  
**Default:** UUID

**Examples:**
```
{{random}}              → 550e8400-e29b-41d4-a716-446655440000
{{random:uuid}}         → 550e8400-e29b-41d4-a716-446655440000
{{random:str:10}}       → aB3xY7zK9m
{{random:num:8}}        → 12345678
{{random:16}}           → aBcDeFgH12345678 (16-char string)
```

#### EnvironmentVariable

**Name:** `env`  
**Format:** Environment variable name (required)

**Examples:**
```
{{env:HOME}}            → /home/user
{{env:PATH}}            → /usr/local/bin:/usr/bin
{{env:KAFKA_BROKER}}    → localhost:9092
```

**Error Handling:** Throws IllegalArgumentException if variable name is null/empty

---

## Utilities

### FileUtils

**Location:** `utils.ktestify.io.github.FileUtils`

Utility class for file operations with dynamic variable processing support.

**Key Methods:**

```java
// Get File object from path
File getFile(String fullPath)

// Read file content with dynamic variable processing
String getFileContent(File file)
String getFileContent(InputStream inputStream)

// Get InputStream from File
InputStream getInputStream(File file)

// Write content to file
void writeFileContent(File file, String content)
```

**Dynamic Variable Integration:**
All `getFileContent()` methods automatically process dynamic variables in file content.

**Example:**
```java
// payload.json contains: {"orderId": "{{random:uuid}}", "date": "{{date}}"}
File file = FileUtils.getFile("/path/to/payload.json");
String content = FileUtils.getFileContent(file);
// content: {"orderId": "550e8400-e29b-41d4-a716-446655440000", "date": "2026-01-11"}
```

### StringDiffUtils

**Location:** `utils.ktestify.io.github.StringDiffUtils`

Utility for generating colored diff output between two strings.

**Features:**
- Character-by-character comparison
- Colored output (red for differences, green for additions)
- Visible whitespace characters
- Line break indicators

**Usage:**
```java
String expected = "Hello World";
String actual = "Hello Universe";

StringBuilder diff = StringDiffUtils.getPrettyStringDiff(
    expected, 
    actual, 
    StringDiffUtils.Type.EXPECTED
);
```

**Color Codes:**
- Red background: Mismatched characters
- Green color: Additional characters in actual
- White: Matching characters
- ␣: Space character
- ¶: Line break

### AvroUtils

**Location:** `serdes.utils.ktestify.io.github.AvroUtils`

Comprehensive utility class for Avro operations with 1000+ lines of functionality.

#### Key Features

**1. JSON to Avro Conversion**
```java
// Convert JSON to GenericRecord
GenericRecord convertJsonToGenericRecord(String json, Schema schema)

// Convert JSON element to Avro object
Object convertJsonToAvro(JsonElement jsonElement, Schema fieldSchema)
```

**2. Avro Record Comparison**
```java
// Strict comparison
boolean doesAvroRecordsStrictlyMatches(String expected, String actual)

// Smart comparison (ignores key order, formatting)
boolean doesAvroRecordsSmartMatches(String expected, String actual)

// Smart comparison with exclusions
boolean doesAvroRecordsSmartMatchesWithExclusions(
    String expected, 
    String actual, 
    List<String> excludedKeys
)
```

**3. Key-based Value Matching**
```java
// Check specific key value across two records
boolean doesAvroValueFromKeyMatchesRecords(
    String key, 
    String expectedRecord, 
    String actualRecord
)

// Check nested key value against expected value
boolean doesAvroValueFromKeyMatchesRecord(
    String expectedValue, 
    String nestedKey, 
    String actualRecord
)
```

**4. Map Operations**
```java
// Deep equality comparison
boolean deepEquals(Map<String, Object> expected, Map<String, Object> actual)

// Deep equality with exclusions
boolean deepEquals(
    Map<String, Object> expected, 
    Map<String, Object> actual, 
    List<String> excludedKeys
)
```

**5. Date/Time Conversion**
```java
// Convert dates to timestamps in maps
Map<String, Object> convertDatesToTimestamps(Map<String, Object> jsonMap)

// Check if string is a date
boolean isDateString(String dateString)

// Convert date string to integer (days since epoch)
int convertDateStringToDateInt(String dateString)

// Convert time string to milliseconds since midnight
int convertTimeStringToTimeMillis(String timeString)

// Convert date string to Unix timestamp (milliseconds)
long convertDateStringToTimestamp(String dateString)

// Convert date string to Unix timestamp (microseconds)
long convertDateStringToTimestampMicros(String dateString)
```

**6. Formatting and Conversion**
```java
// Pretty print JSON
String getPrettyAvroValue(String uglyAvroValue)

// Map to JSON string
String convertMapToJsonString(Map<String, Object> map)

// JSON string to Map
Map<String, Object> convertJsonToMap(String json)

// File content to JsonElement
JsonElement getJsonElementFromFile(String fileContent)

// Extract headers from JSON
Map<String, String> getHeadersMap(String jsonString)
```

#### Supported Avro Types

**Primitive Types:**
- STRING
- BOOLEAN
- INT
- LONG
- FLOAT
- DOUBLE
- BYTES

**Complex Types:**
- RECORD (nested objects)
- MAP
- ARRAY
- UNION

**Logical Types:**
- `date` - Days since epoch (INT)
- `time-millis` - Milliseconds since midnight (INT)
- `timestamp-millis` - Unix timestamp in milliseconds (LONG)
- `timestamp-micros` - Unix timestamp in microseconds (LONG)
- `decimal` - Arbitrary precision decimal (BYTES)

#### Date Format Support

**Supported Input Patterns:**
```
yyyy-MM-dd'T'HH:mm:ss
yyyy-MM-dd'T'HH:mm:ss'Z'
yyyy-MM-dd'T'HH:mm:ss:SSS
yyyy-MM-dd'T'HH:mm:ss:SSS'Z'
yyyy-MM-dd'T'HH:mm:ss[.SSS]'Z'      (with optional milliseconds)
yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]'Z'   (with optional microseconds)
```

#### Smart Array Comparison

AvroUtils provides intelligent array comparison with two modes:

**Ordered Comparison:**
- Used for primitive arrays
- Elements must match in exact order

**Unordered Comparison:**
- Used for object arrays
- Matches objects regardless of order
- Each expected object must find a match in actual array
- Prevents duplicate matching

**Example:**
```java
String expected = """
{
  "items": [
    {"id": 1, "name": "Apple"},
    {"id": 2, "name": "Banana"}
  ]
}
""";

String actual = """
{
  "items": [
    {"id": 2, "name": "Banana"},
    {"id": 1, "name": "Apple"}
  ]
}
""";

// Returns true - order doesn't matter for objects
boolean matches = AvroUtils.doesAvroRecordsSmartMatches(expected, actual);
```

---

## Exception Handling

### ConsumerException

**Location:** `exceptions.ktestify.io.github.ConsumerException`

Runtime exception for consumer-related errors.

**Common Scenarios:**
- Timeout while waiting for records
- Cannot consume from INPUT topic
- Record matching failures

**Usage:**
```java
throw new ConsumerException("Consumer died without finding any record");
```

### ProducerException

**Location:** `exceptions.ktestify.io.github.ProducerException`

Runtime exception for producer-related errors.

**Common Scenarios:**
- Invalid topic configuration
- Missing required parameters
- File validation failures
- Schema parsing errors

**Usage:**
```java
throw new ProducerException("No topic name was specified!");
```

---

## Usage Examples

### Example 1: Simple Raw Producer

```java
// Create topic
Topic topic = new Topic();
topic.setTopicName("orders");
topic.setTopicType(Topic.Type.INPUT);

// Create producer properties
Map<String, String> properties = new HashMap<>();
properties.put("bootstrap.servers", "localhost:9092");

// Create Kafka producer
Properties kafkaProps = new Properties();
kafkaProps.put("bootstrap.servers", "localhost:9092");
kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
Producer<String, String> kafkaProducer = new KafkaProducer<>(kafkaProps);

// Create payload file
File payloadFile = new File("src/test/resources/order.json");

// Create and execute producer
RawKafkaProducer producer = new RawKafkaProducer(
    topic, 
    properties, 
    kafkaProducer, 
    payloadFile
);

producer.produce();
```

### Example 2: Avro Producer with Headers

```java
// Create topic
Topic topic = new Topic();
topic.setTopicName("customer-events");
topic.setTopicType(Topic.Type.INPUT);

// Prepare headers
Map<String, String> headers = Map.of(
    "source", "test-framework",
    "correlation-id", "{{random:uuid}}"
);

// Create producer context
ProducerContext<String, GenericRecord> context = ProducerContext
    .<String, GenericRecord>builder()
    .topic(topic)
    .properties(properties)
    .producer(avroKafkaProducer)
    .payloadFile(new File("customer.json"))
    .schemaFile(new File("customer.avsc"))
    .headers(headers)
    .build();

// Create and execute producer
AvroKafkaProducer producer = new AvroKafkaProducer(context);
producer.produce();
```

### Example 3: Consumer with Matching

```java
// Create custom consumer
public class OrderConsumer extends AbstractKafkaConsumer<String, String> {
    
    private final String expectedStatus;
    
    public OrderConsumer(Topic topic, 
                        Consumer<String, String> consumer,
                        Map<String, String> properties,
                        String expectedStatus) {
        super(topic, consumer, properties);
        this.expectedStatus = expectedStatus;
    }
    
    @Override
    public boolean doesMatch(ConsumerRecord<String, String> record) {
        String payload = record.value();
        Map<String, Object> orderMap = AvroUtils.convertJsonToMap(payload);
        String status = (String) orderMap.get("status");
        return expectedStatus.equals(status);
    }
}

// Use the consumer
Topic topic = new Topic();
topic.setTopicName("order-status");
topic.setTopicType(Topic.Type.OUTPUT);

Map<String, String> properties = new HashMap<>();
properties.put(DATA_TABLE_READ_TIMEOUT, "30000");
properties.put(CONSUMER_DELTA_TIME, "60");

Consumer<String, String> kafkaConsumer = new KafkaConsumer<>(kafkaProps);

OrderConsumer consumer = new OrderConsumer(
    topic, 
    kafkaConsumer, 
    properties, 
    "COMPLETED"
);

Boolean found = consumer.call();  // Blocks until match or timeout
```

### Example 4: Dynamic Variables in Payload

**payload-template.json:**
```json
{
  "orderId": "{{random:uuid}}",
  "customerId": "CUST-{{random:num:8}}",
  "orderDate": "{{date:yyyy-MM-dd}}",
  "timestamp": "{{timestamp:yyyy-MM-dd'T'HH:mm:ss'Z'}}",
  "environment": "{{env:ENV_NAME}}",
  "correlationId": "{{random:str:16}}"
}
```

**Java Code:**
```java
File templateFile = new File("payload-template.json");
String processedContent = FileUtils.getFileContent(templateFile);

// Resulting JSON (example):
// {
//   "orderId": "550e8400-e29b-41d4-a716-446655440000",
//   "customerId": "CUST-12345678",
//   "orderDate": "2026-01-11",
//   "timestamp": "2026-01-11T14:30:45Z",
//   "environment": "production",
//   "correlationId": "aB3xY7zK9mP5qR2s"
// }
```

### Example 5: Avro Record Comparison with Exclusions

```java
String expectedRecord = """
{
  "orderId": "12345",
  "customerId": "CUST-001",
  "amount": 99.99,
  "createdAt": "2026-01-11T10:00:00Z",
  "processedAt": "2026-01-11T10:05:00Z"
}
""";

String actualRecord = """
{
  "orderId": "12345",
  "customerId": "CUST-001",
  "amount": 99.99,
  "createdAt": "2026-01-11T10:00:00Z",
  "processedAt": "2026-01-11T10:06:30Z"
}
""";

// Exclude timestamp fields that may vary
List<String> excludedKeys = List.of("processedAt", "createdAt");

boolean matches = AvroUtils.doesAvroRecordsSmartMatchesWithExclusions(
    expectedRecord,
    actualRecord,
    excludedKeys
);

// Returns true - differences only in excluded fields
```

### Example 6: Nested Key Matching

```java
String record = """
{
  "order": {
    "header": {
      "orderId": "12345",
      "status": "COMPLETED"
    },
    "items": [
      {
        "productId": "PROD-001",
        "quantity": 2
      }
    ]
  }
}
""";

// Check nested value
boolean statusMatch = AvroUtils.doesAvroValueFromKeyMatchesRecord(
    "COMPLETED",
    "order.header.status",
    record
);

// Check array element
boolean productMatch = AvroUtils.doesAvroValueFromKeyMatchesRecord(
    "PROD-001",
    "order.items[0].productId",
    record
);
```

### Example 7: Custom Dynamic Variable

```java
// Create custom variable
public class CounterVariable implements DynamicVariable {
    private static final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public String getName() {
        return "counter";
    }
    
    @Override
    public String process(String format) {
        int value = counter.incrementAndGet();
        if (format != null && !format.isEmpty()) {
            // Format could be padding, e.g., "5" for 5 digits
            int padding = Integer.parseInt(format);
            return String.format("%0" + padding + "d", value);
        }
        return String.valueOf(value);
    }
}

// Register custom variable
DynamicVariableFactory.registerVariable(new CounterVariable());

// Use in payload
String template = "Order-{{counter:5}}";  // "Order-00001"
DynamicVariableProcessor processor = new DynamicVariableProcessor();
String result = processor.process(template);
```

---

## Best Practices

### 1. Producer Best Practices

✅ **DO:**
- Use `ProducerContext.builder()` for complex configurations
- Validate topic types before producing
- Use headers for metadata and tracing
- Leverage dynamic variables for test data generation
- Close producers after use

❌ **DON'T:**
- Produce to OUTPUT topics
- Hardcode timestamps or IDs in test payloads
- Ignore producer exceptions
- Reuse producers across different test scenarios

### 2. Consumer Best Practices

✅ **DO:**
- Set appropriate read timeouts based on expected latency
- Use delta time to avoid processing old messages
- Implement specific matching logic in `doesMatch()`
- Filter by expected record key when needed
- Log detailed information in custom consumers

❌ **DON'T:**
- Consume from INPUT topics
- Use very short timeouts (< 5 seconds)
- Ignore ConsumerException
- Process all records without filtering

### 3. Testing Best Practices

✅ **DO:**
- Use dynamic variables for unique test data
- Exclude volatile fields from comparisons
- Use smart matching instead of strict matching
- Test with realistic data volumes
- Clean up topics between tests

❌ **DON'T:**
- Rely on strict JSON formatting matching
- Compare timestamps without conversion
- Ignore schema evolution
- Test with production data

### 4. Configuration Best Practices

✅ **DO:**
- Centralize configuration in properties files
- Use meaningful topic aliases
- Document custom match methods
- Version schemas properly
- Use appropriate serializers/deserializers

❌ **DON'T:**
- Hardcode broker URLs
- Mix INPUT and OUTPUT topic usage
- Skip schema validation
- Use default configurations in production

---

## Troubleshooting

### Common Issues

**1. ConsumerException: Consumer died without finding any record**

**Cause:** Timeout exceeded before finding a matching record

**Solutions:**
- Increase `DATA_TABLE_READ_TIMEOUT`
- Adjust `CONSUMER_DELTA_TIME` to look further back
- Verify topic name and type
- Check if records are being produced
- Review matching logic in `doesMatch()`

**2. ProducerException: Topic type is OUTPUT. Cannot produce to an output topic**

**Cause:** Attempting to produce to a topic marked as OUTPUT

**Solution:**
- Change topic type to INPUT
- Verify topic configuration

**3. ProducerException: No payload content was provided**

**Cause:** Both `payloadFile` and `payload` are null/empty

**Solutions:**
- Provide a valid payload file
- Provide inline payload string
- Verify file path and existence

**4. IllegalArgumentException: Invalid date format**

**Cause:** Date string doesn't match expected format

**Solutions:**
- Use supported date patterns
- Check for timezone indicators
- Validate input date strings

**5. Dynamic variables not being replaced**

**Cause:** Incorrect placeholder syntax or unregistered variable

**Solutions:**
- Use correct syntax: `{{variableName:format}}`
- Verify variable is registered
- Check variable name spelling
- Review format specification

---

## Future Enhancements

Based on TODO comments in the codebase:

1. **MatchedRecord Factory** - Track matched records across executions to prevent duplicate processing
2. **Snapshot Manager Integration** - Update test files automatically when tests fail
3. **Schema Registry Integration** - Fetch schemas from Confluent Schema Registry
4. **Enhanced Matching Methods** - More built-in matching strategies
5. **Metrics and Monitoring** - Consumer/producer performance metrics
6. **Parallel Processing** - Concurrent consumer matching
7. **Custom Serializers** - Pluggable serialization framework

---

## Dependencies

Key dependencies from `pom.xml`:

```xml
<!-- Kafka -->
<kafka.version>3.9.0</kafka.version>
<confluent.version>7.9.0</confluent.version>

<!-- Testing -->
<junit.jupiter.version>5.12.2</junit.jupiter.version>
<cucumber.version>7.8.1</cucumber.version>
<mockito.version>5.17.0</mockito.version>

<!-- Utilities -->
<lombok.version>1.18.38</lombok.version>
<gson.version>2.13.0</gson.version>
<slf4j.version>2.0.9</slf4j.version>

<!-- Java -->
<java.version>17</java.version>
```

---

## Contributing

When contributing to KTestify:

1. Follow existing code patterns and naming conventions
2. Add JavaDoc comments for public methods
3. Include unit tests for new features
4. Update this documentation for significant changes
5. Use Lombok annotations to reduce boilerplate
6. Follow SLF4J logging best practices
7. Maintain backward compatibility where possible

---

## License

*[License information to be added]*

---

## Support

For issues, questions, or contributions, please contact the development team or create an issue in the project repository.

---

**End of Documentation**

