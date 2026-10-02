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
package io.github.ktestify.io.inputs;

import io.github.ktestify.io.inputs.types.DateVariable;
import io.github.ktestify.io.inputs.types.EnvironmentVariable;
import io.github.ktestify.io.inputs.types.RandomVariable;
import io.github.ktestify.io.inputs.types.TimestampVariable;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of {@link DynamicVariable} implementations, keyed by variable name.
 *
 * <p>Names are case-insensitive: {@code {{env:HOME}}}, {@code {{ENV:HOME}}} and {@code {{Env:HOME}}} all resolve to the
 * same {@link EnvironmentVariable}. The built-in variables ({@code date}, {@code timestamp}, {@code random},
 * {@code env}) are registered at class initialization and can be restored with {@link #resetToDefaults()}.
 *
 * @since 0.1.0
 */
@UtilityClass
public class DynamicVariableFactory {
    private static final Map<String, DynamicVariable> variables = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicVariableFactory.class);

    static {
        resetToDefaults();
    }

    /**
     * Registers a variable under its {@link DynamicVariable#getName() name}, replacing any variable already registered
     * under the same name (case-insensitive).
     *
     * @param variable the variable to register
     */
    public static void registerVariable(DynamicVariable variable) {
        LOGGER.debug("Registering variable {}.", variable.getName());
        variables.put(normalize(variable.getName()), variable);
    }

    /**
     * Returns the variable registered under {@code name}.
     *
     * @param name the variable name, case-insensitive
     * @return the variable, or {@code null} if none is registered
     */
    public static DynamicVariable getVariable(String name) {
        return name == null ? null : variables.get(normalize(name));
    }

    /**
     * Returns whether a variable is registered under {@code name}.
     *
     * @param name the variable name, case-insensitive
     * @return {@code true} if a variable is registered
     */
    public static boolean isRegistered(String name) {
        return name != null && variables.containsKey(normalize(name));
    }

    /**
     * Returns an immutable snapshot of the registered (lower-case) variable names.
     *
     * @return the registered names
     */
    public static Set<String> getRegisteredVariableNames() {
        return Set.copyOf(variables.keySet());
    }

    /** Removes every registered variable, including the built-ins. Use {@link #resetToDefaults()} to restore them. */
    public static void clearRegisteredVariables() {
        variables.clear();
    }

    /**
     * Clears the registry and registers the built-in variables again.
     *
     * @since 1.1.4
     */
    public static void resetToDefaults() {
        variables.clear();
        registerVariable(new DateVariable());
        registerVariable(new TimestampVariable());
        registerVariable(new RandomVariable());
        registerVariable(new EnvironmentVariable());
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
