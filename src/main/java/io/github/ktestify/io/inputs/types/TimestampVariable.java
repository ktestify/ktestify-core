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
package io.github.ktestify.io.inputs.types;

import io.github.ktestify.io.inputs.DynamicVariable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimestampVariable implements DynamicVariable {
    @Override
    public String getName() {
        return "timestamp";
    }

    @Override
    public String process(String format) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
    }

    @Override
    public String process() {
        // Use ISO datetime format as default
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
