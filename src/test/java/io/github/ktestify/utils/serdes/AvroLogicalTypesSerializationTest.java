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

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Regression tests for Avro logical types going through {@link AvroDeserializer} and {@link AvroUtils}. */
@DisplayName("Avro logical types to JSON")
class AvroLogicalTypesSerializationTest {

    private static final Schema SCHEMA = new Schema.Parser().parse("""
                    {
                      "type": "record", "name": "Logical", "fields": [
                        {"name": "day", "type": {"type": "int", "logicalType": "date"}},
                        {"name": "at", "type": {"type": "long", "logicalType": "timestamp-micros"}},
                        {"name": "local", "type": {"type": "long", "logicalType": "local-timestamp-millis"}},
                        {"name": "time", "type": {"type": "int", "logicalType": "time-millis"}},
                        {"name": "payload", "type": "bytes"}
                      ]
                    }
                    """);

    @Test
    @DisplayName("java.time values are serialized as ISO-8601 strings")
    void javaTimeValuesSerializeAsIsoStrings() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("day", LocalDate.of(2026, 10, 2));
        map.put("at", Instant.parse("2026-10-02T10:15:30Z"));
        map.put("local", LocalDateTime.of(2026, 10, 2, 10, 15, 30));
        map.put("time", LocalTime.of(10, 15, 30));

        String json = AvroUtils.convertMapToJsonString(map);
        Map<String, Object> roundTrip = AvroUtils.convertJsonToMap(json);

        assertEquals("2026-10-02", roundTrip.get("day"));
        assertEquals("2026-10-02T10:15:30Z", roundTrip.get("at"));
        assertEquals("2026-10-02T10:15:30", roundTrip.get("local"));
        assertEquals("10:15:30", roundTrip.get("time"));
    }

    @Test
    @DisplayName("a GenericRecord with logical types converts to JSON without reflection errors")
    void genericRecordWithLogicalTypesConverts() {
        GenericRecord record = new GenericData.Record(SCHEMA);
        record.put("day", 20_000);
        record.put("at", 1_000_000L);
        record.put("local", 1_000L);
        record.put("time", 1_000);
        record.put("payload", ByteBuffer.wrap(new byte[] {1, 2, 3}));

        String json =
                assertDoesNotThrow(() -> AvroUtils.convertMapToJsonString(AvroDeserializer.recordDeserializer(record)));

        assertTrue(json.contains("\"day\": \"2024-10-04\""), json);
    }

    @Test
    @DisplayName("bytes honour the ByteBuffer position and limit")
    void bytesHonourBufferWindow() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {9, 1, 2, 9}, 1, 2).slice();
        Schema bytesSchema = Schema.create(Schema.Type.BYTES);

        byte[] result = (byte[]) AvroDeserializer.objectDeserializer(buffer, bytesSchema);

        assertArrayEquals(new byte[] {1, 2}, result);
        assertEquals(0, buffer.position(), "source buffer must not be consumed");
    }
}
