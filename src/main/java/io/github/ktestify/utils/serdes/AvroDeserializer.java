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

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.avro.Conversions.DecimalConversion;
import org.apache.avro.Conversions.UUIDConversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.data.TimeConversions.DateConversion;
import org.apache.avro.data.TimeConversions.LocalTimestampMicrosConversion;
import org.apache.avro.data.TimeConversions.LocalTimestampMillisConversion;
import org.apache.avro.data.TimeConversions.TimeMicrosConversion;
import org.apache.avro.data.TimeConversions.TimeMillisConversion;
import org.apache.avro.data.TimeConversions.TimestampMicrosConversion;
import org.apache.avro.data.TimeConversions.TimestampMillisConversion;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericFixed;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deserializes Avro {@link GenericRecord} instances into plain Java {@link Map} representations, handling all Avro
 * logical types (date, time, timestamp, decimal, UUID, …).
 *
 * @since 0.3.0
 */
public final class AvroDeserializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvroDeserializer.class);
    public static final String ERROR_UNEXPECTED_VALUE_TYPE = "Unexpected value type";

    private static final String DATE = "date";
    private static final String DECIMAL = "decimal";
    private static final String UUID_TYPE = "uuid";
    private static final String TIME_MILLIS = "time-millis";
    private static final String TIME_MICROS = "time-micros";
    private static final String TIMESTAMP_MILLIS = "timestamp-millis";
    private static final String TIMESTAMP_MICROS = "timestamp-micros";
    private static final String LOCAL_TIMESTAMP_MILLIS = "local-timestamp-millis";
    private static final String LOCAL_TIMESTAMP_MICROS = "local-timestamp-micros";

    private static final DecimalConversion DECIMAL_CONVERSION = new DecimalConversion();
    private static final UUIDConversion UUID_CONVERSION = new UUIDConversion();
    private static final DateConversion DATE_CONVERSION = new DateConversion();
    private static final TimeMicrosConversion TIME_MICROS_CONVERSION = new TimeMicrosConversion();
    private static final TimeMillisConversion TIME_MILLIS_CONVERSION = new TimeMillisConversion();
    private static final TimestampMicrosConversion TIMESTAMP_MICROS_CONVERSION = new TimestampMicrosConversion();
    private static final TimestampMillisConversion TIMESTAMP_MILLIS_CONVERSION = new TimestampMillisConversion();
    private static final LocalTimestampMicrosConversion LOCAL_TIMESTAMP_MICROS_CONVERSION =
            new LocalTimestampMicrosConversion();
    private static final LocalTimestampMillisConversion LOCAL_TIMESTAMP_MILLIS_CONVERSION =
            new LocalTimestampMillisConversion();

    private AvroDeserializer() {}

    // =========================================================================
    // Public API
    // =========================================================================

    /** Deserializes a {@link GenericRecord} into an ordered {@link Map}, preserving schema field order. */
    public static Map<String, Object> recordDeserializer(GenericRecord record) {
        if (record.getSchema() == null || record.getSchema().getFields() == null) {
            LOGGER.error("Record schema or fields are null — returning empty map.");
            return new HashMap<>();
        }
        return record.getSchema().getFields().stream()
                .collect(
                        LinkedHashMap::new,
                        (m, field) -> m.put(field.name(), objectDeserializer(record.get(field.name()), field.schema())),
                        HashMap::putAll);
    }

    /** Deserializes a single Avro value according to its schema, handling logical types. */
    public static Object objectDeserializer(Object value, Schema schema) {
        LogicalType logicalType = schema.getLogicalType();
        Type primitiveType = schema.getType();
        if (logicalType != null) {
            return switch (logicalType.getName()) {
                case DATE -> dateDeserializer(value, schema, primitiveType, logicalType);
                case DECIMAL -> decimalDeserializer(value, schema, primitiveType, logicalType);
                case TIME_MICROS -> timeMicrosDeserializer(value, schema, primitiveType, logicalType);
                case TIME_MILLIS -> timeMillisDeserializer(value, schema, primitiveType, logicalType);
                case TIMESTAMP_MICROS -> timestampMicrosDeserializer(value, schema, primitiveType, logicalType);
                case TIMESTAMP_MILLIS ->
                    timestampMillisDeserializer(value, schema, primitiveType, logicalType)
                            .toEpochMilli();
                case LOCAL_TIMESTAMP_MICROS ->
                    localTimestampMicrosDeserializer(value, schema, primitiveType, logicalType);
                case LOCAL_TIMESTAMP_MILLIS ->
                    localTimestampMillisDeserializer(value, schema, primitiveType, logicalType);
                case UUID_TYPE -> uuidDeserializer(value, schema, primitiveType, logicalType);
                default -> throw new IllegalStateException(ERROR_UNEXPECTED_VALUE_TYPE + logicalType);
            };
        }
        return primitiveDeserializer(value, schema);
    }

    // =========================================================================
    // Private — primitive / compound
    // =========================================================================

    @SuppressWarnings("unchecked")
    private static Object primitiveDeserializer(Object value, Schema schema) {
        return switch (schema.getType()) {
            case UNION -> unionDeserializer(value, schema);
            case MAP -> mapDeserializer((Map<String, ?>) value, schema);
            case RECORD -> recordDeserializer((GenericRecord) value);
            case ENUM -> value.toString();
            case ARRAY -> arrayDeserializer((Collection<?>) value, schema);
            case FIXED -> ((GenericFixed) value).bytes();
            case STRING -> ((CharSequence) value).toString();
            case BYTES -> ((ByteBuffer) value).array();
            case INT, LONG, FLOAT, DOUBLE, BOOLEAN, NULL -> value;
            default -> throw new IllegalStateException(ERROR_UNEXPECTED_VALUE_TYPE + schema.getType());
        };
    }

    private static Object unionDeserializer(Object value, Schema schema) {
        return objectDeserializer(
                value,
                schema.getTypes().stream()
                        .filter(type -> {
                            try {
                                return new GenericData().validate(type, value);
                            } catch (Exception e) {
                                LOGGER.error("Error validating union type: {}", e.getMessage());
                                return false;
                            }
                        })
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No valid union type for value: " + value)));
    }

    private static Map<String, ?> mapDeserializer(Map<String, ?> value, Schema schema) {
        return value.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, e -> objectDeserializer(e.getValue(), schema.getValueType())));
    }

    private static Collection<?> arrayDeserializer(Collection<?> value, Schema schema) {
        return value.stream()
                .map(e -> objectDeserializer(e, schema.getElementType()))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Private — logical types
    // =========================================================================

    private static Instant timestampMicrosDeserializer(Object v, Schema s, Type t, LogicalType l) {
        if (Objects.requireNonNull(t) == Type.LONG) return TIMESTAMP_MICROS_CONVERSION.fromLong((Long) v, s, l);
        throw new IllegalStateException(ERROR_UNEXPECTED_VALUE_TYPE + t);
    }

    private static Instant timestampMillisDeserializer(Object v, Schema s, Type t, LogicalType l) {
        if (t == Type.LONG) return TIMESTAMP_MILLIS_CONVERSION.fromLong((Long) v, s, l);
        throw new IllegalStateException(ERROR_UNEXPECTED_VALUE_TYPE + t);
    }

    private static LocalDateTime localTimestampMicrosDeserializer(Object v, Schema s, Type t, LogicalType l) {
        if (Objects.requireNonNull(t) == Type.LONG) return LOCAL_TIMESTAMP_MICROS_CONVERSION.fromLong((Long) v, s, l);
        throw new IllegalStateException(ERROR_UNEXPECTED_VALUE_TYPE + t);
    }

    private static LocalDateTime localTimestampMillisDeserializer(Object v, Schema s, Type t, LogicalType l) {
        if (t == Type.LONG) return LOCAL_TIMESTAMP_MILLIS_CONVERSION.fromLong((Long) v, s, l);
        throw new IllegalStateException(ERROR_UNEXPECTED_VALUE_TYPE + t);
    }

    private static LocalTime timeMicrosDeserializer(Object v, Schema s, Type t, LogicalType l) {
        if (t == Type.LONG) return TIME_MICROS_CONVERSION.fromLong((Long) v, s, l);
        throw new IllegalStateException(ERROR_UNEXPECTED_VALUE_TYPE + t);
    }

    private static LocalTime timeMillisDeserializer(Object v, Schema s, Type t, LogicalType l) {
        if (t == Type.INT) return TIME_MILLIS_CONVERSION.fromInt((Integer) v, s, l);
        throw new IllegalStateException(ERROR_UNEXPECTED_VALUE_TYPE + t);
    }

    public static LocalDate dateDeserializer(Object v, Schema s, Type t, LogicalType l) {
        if (t == Type.INT) return DATE_CONVERSION.fromInt((Integer) v, s, l);
        throw new IllegalStateException(ERROR_UNEXPECTED_VALUE_TYPE + t);
    }

    private static UUID uuidDeserializer(Object v, Schema s, Type t, LogicalType l) {
        if (t == Type.STRING) return UUID_CONVERSION.fromCharSequence((CharSequence) v, s, l);
        throw new IllegalStateException(ERROR_UNEXPECTED_VALUE_TYPE + t);
    }

    private static BigDecimal decimalDeserializer(Object v, Schema s, Type t, LogicalType l) {
        return switch (t) {
            case BYTES -> DECIMAL_CONVERSION.fromBytes((ByteBuffer) v, s, l);
            case FIXED -> DECIMAL_CONVERSION.fromFixed((GenericFixed) v, s, l);
            default -> throw new IllegalStateException(ERROR_UNEXPECTED_VALUE_TYPE + t);
        };
    }
}
