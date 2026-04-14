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
package io.github.ktestify.models;

import java.time.Instant;
import java.util.Objects;
import lombok.Value;
import lombok.With;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Value
public class MatchedRecord {
    String topic;
    int partition;
    long offset;
    String key;
    Instant timestamp;

    @With
    Instant processedTime;

    public static <K, V> MatchedRecord fromConsumerRecord(ConsumerRecord<K, V> record) {
        return new MatchedRecord(
                record.topic(),
                record.partition(),
                record.offset(),
                record.key().toString(), // Key may be serialized in different formats, convert to String as a common
                // representation
                Instant.ofEpochMilli(record.timestamp()),
                Instant.now());
    }

    public MatchedRecord(
            String topic, int partition, long offset, String key, Instant timestamp, Instant processedTime) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.key = key;
        this.timestamp = timestamp;
        this.processedTime = processedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MatchedRecord that = (MatchedRecord) o;
        return partition == that.partition
                && offset == that.offset
                && Objects.equals(topic, that.topic)
                && Objects.equals(key, that.key)
                && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, partition, offset, key, timestamp);
    }

    @Override
    public String toString() {
        return String.format(
                "MatchedRecord(topic=%s, partition=%d, offset=%d, key=%s, timestamp=%s)",
                topic, partition, offset, key, timestamp);
    }
}
