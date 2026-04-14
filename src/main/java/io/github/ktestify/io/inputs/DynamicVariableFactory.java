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
package io.github.ktestify.io.inputs;

import io.github.ktestify.io.inputs.types.DateVariable;
import io.github.ktestify.io.inputs.types.EnvironmentVariable;
import io.github.ktestify.io.inputs.types.RandomVariable;
import io.github.ktestify.io.inputs.types.TimestampVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UtilityClass
public class DynamicVariableFactory {
    private static final Map<String, DynamicVariable> variables = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicVariableFactory.class);

    static {
        registerVariable(new DateVariable());
        registerVariable(new TimestampVariable());
        registerVariable(new RandomVariable());
        registerVariable(new EnvironmentVariable());
    }

    public static void registerVariable(DynamicVariable variable) {
        LOGGER.debug("Registering variable {}.", variable.getName());
        variables.put(variable.getName(), variable);
    }

    public static DynamicVariable getVariable(String name) {
        return variables.get(name);
    }

    public static boolean isRegistered(String name) {
        return variables.containsKey(name);
    }

    public static Set<String> getRegisteredVariableNames() {
        return variables.keySet();
    }

    public static void clearRegisteredVariables() {
        variables.clear();
    }
}
