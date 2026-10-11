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

import com.typesafe.config.ConfigFactory;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.exceptions.PluginException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PluginLifecycleTest {
    private static final String SERVICE = "META-INF/services/" + KtestifyPlugin.class.getName();
    private static final List<String> EVENTS = new ArrayList<>();
    private static RuntimeException initFailure;
    private static Error initError;
    private static boolean failShutdown;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        EVENTS.clear();
        initFailure = null;
        initError = null;
        failShutdown = false;
        KtestifyConfig.reset();
    }

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    @Test
    void initializationFailureRollsBackStartedAndPartiallyInitializedPlugins() throws Exception {
        initFailure = new IllegalStateException("simulated init failure");
        failShutdown = true;
        ClassLoader original = Thread.currentThread().getContextClassLoader();

        PluginException failure = assertThrows(PluginException.class, this::loadPlugins);

        assertSame(initFailure, failure.getCause());
        assertEquals(List.of("started:init", "failing:init", "failing:shutdown", "started:shutdown"), EVENTS);
        assertSame(original, Thread.currentThread().getContextClassLoader());
    }

    @Test
    void initializationErrorStillRollsBackAndPreservesOriginalError() {
        initError = new AssertionError("simulated init error");

        assertSame(initError, assertThrows(AssertionError.class, this::loadPlugins));
        assertEquals(List.of("started:init", "failing:init", "failing:shutdown", "started:shutdown"), EVENTS);
    }

    @Test
    void shutdownContinuesAfterPluginFailureAndIsIdempotent() throws Exception {
        PluginRegistry registry = loadPlugins();
        failShutdown = true;

        assertDoesNotThrow(registry::shutdown);
        assertDoesNotThrow(registry::shutdown);

        assertEquals(
                List.of("started:init", "failing:init", "untouched:init",
                        "untouched:shutdown", "failing:shutdown", "started:shutdown"),
                EVENTS);
    }

    private PluginRegistry loadPlugins() throws IOException {
        Path descriptor = tempDir.resolve(SERVICE);
        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, StartedPlugin.class.getName() + "\n"
                + FailingPlugin.class.getName() + "\n" + UntouchedPlugin.class.getName() + "\n");
        KtestifyConfig config = KtestifyConfig.load(ConfigFactory.parseString("ktestify.plugins.dir = \"\""));
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, original) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                return SERVICE.equals(name) ? findResources(name) : super.getResources(name);
            }
        }) {
            thread.setContextClassLoader(loader);
            try {
                return PluginRegistry.load(() -> config);
            } finally {
                thread.setContextClassLoader(original);
            }
        }
    }

    public static class StartedPlugin implements KtestifyPlugin {
        @Override
        public String getId() {
            return "started";
        }

        @Override
        public String getVersion() {
            return "test";
        }

        @Override
        public String getGluePackage() {
            return null;
        }

        @Override
        public void initialize(PluginContext context) {
            EVENTS.add(getId() + ":init");
        }

        @Override
        public void shutdown() {
            EVENTS.add(getId() + ":shutdown");
        }
    }

    public static class FailingPlugin extends StartedPlugin {
        @Override
        public String getId() {
            return "failing";
        }

        @Override
        public void initialize(PluginContext context) {
            super.initialize(context);
            if (initFailure != null) {
                throw initFailure;
            }
            if (initError != null) {
                throw initError;
            }
        }

        @Override
        public void shutdown() {
            super.shutdown();
            if (failShutdown) {
                throw new IllegalStateException("simulated shutdown failure");
            }
        }
    }

    public static class UntouchedPlugin extends StartedPlugin {
        @Override
        public String getId() {
            return "untouched";
        }
    }
}
