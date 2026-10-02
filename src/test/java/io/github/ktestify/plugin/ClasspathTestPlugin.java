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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only plugin registered through {@code src/test/resources/META-INF/services}. It counts lifecycle calls so tests
 * can assert that {@link PluginRegistry} initializes and shuts down each plugin exactly once.
 */
public class ClasspathTestPlugin implements KtestifyPlugin {

    /** Plugin id exposed by this test plugin. */
    public static final String ID = "classpath-test";

    /** Number of {@link #initialize(PluginContext)} calls across all instances. */
    public static final AtomicInteger INIT_CALLS = new AtomicInteger();

    /** Number of {@link #shutdown()} calls across all instances. */
    public static final AtomicInteger SHUTDOWN_CALLS = new AtomicInteger();

    /** Resets the lifecycle counters. */
    public static void resetCounters() {
        INIT_CALLS.set(0);
        SHUTDOWN_CALLS.set(0);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getVersion() {
        return "0.0.0-test";
    }

    @Override
    public String getGluePackage() {
        return "io.github.ktestify.plugin.testglue";
    }

    @Override
    public void initialize(PluginContext context) {
        INIT_CALLS.incrementAndGet();
    }

    @Override
    public void shutdown() {
        SHUTDOWN_CALLS.incrementAndGet();
    }
}
