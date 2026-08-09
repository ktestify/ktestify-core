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
package io.github.ktestify.plugin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link PluginVersionResolver}.
 *
 * <p>Verifies fallback behaviour when the {@code plugin-version.properties} resource is missing or unfiltered, which is
 * the expected state in IDE runs and unit test contexts.
 */
@DisplayName("PluginVersionResolver")
class PluginVersionResolverTest {

    @Test
    @DisplayName("resolve() returns fallback when no plugin-version.properties is found next to the class")
    void returnsFallbackWhenResourceMissing() {
        // This test class has no plugin-version.properties next to it
        String version = PluginVersionResolver.resolve(PluginVersionResolverTest.class, "dev");
        assertEquals("dev", version);
    }

    @Test
    @DisplayName("resolve() returns fallback when fallback is a real version string")
    void returnsFallbackVersionString() {
        String version = PluginVersionResolver.resolve(PluginVersionResolverTest.class, "1.2.3");
        assertEquals("1.2.3", version);
    }

    @Test
    @DisplayName("resolve() returns fallback when fallback is blank")
    void returnsBlankFallback() {
        String version = PluginVersionResolver.resolve(PluginVersionResolverTest.class, "");
        assertEquals("", version);
    }

    @Test
    @DisplayName("resolve() on PluginVersionResolver itself returns fallback (no resource next to it)")
    void resolveOnItselfReturnsFallback() {
        String version = PluginVersionResolver.resolve(PluginVersionResolver.class, "test");
        assertEquals("test", version);
    }
}

