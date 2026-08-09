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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Resolves a plugin's build version from a Maven-filtered {@code plugin-version.properties} file on the classpath.
 *
 * <p>Each plugin ships a {@code plugin-version.properties} file in its own package directory (e.g.
 * {@code /io/github/ktestify/azureblob/plugin-version.properties}). The file contains a single property:
 * <pre>
 * plugin.version=${project.version}
 * </pre>
 * Maven substitutes {@code ${project.version}} at build time. This approach works in all deployment contexts:
 * <ul>
 *   <li>Standalone plugin JARs (external plugins loaded via {@code URLClassLoader})
 *   <li>The shaded fat JAR (each plugin's file is at a unique package-relative path, so no collision)
 *   <li>IDE runs (falls back to {@code "dev"} when the file is missing or unfiltered)
 * </ul>
 *
 * <p>Plugins should call {@link #resolve(Class, String)} once at class-loading time and store the result in a
 * {@code static final} field:
 * <pre>
 * private static final String VERSION = PluginVersionResolver.resolve(MyPlugin.class, "dev");
 * </pre>
 *
 * @since 1.1.1
 */
public final class PluginVersionResolver {

    private static final Logger LOG = LoggerFactory.getLogger(PluginVersionResolver.class);

    /** Resource file name, looked up relative to the plugin class's package. */
    private static final String RESOURCE_NAME = "plugin-version.properties";

    /** Property key inside the {@code plugin-version.properties} file. */
    private static final String PROPERTY_KEY = "plugin.version";

    private PluginVersionResolver() {}

    /**
     * Resolves the plugin version from the Maven-filtered {@code plugin-version.properties} file located in the same
     * package as the given plugin class.
     *
     * <p>If the file is missing, empty, or contains an unfiltered {@code ${project.version}} placeholder (e.g. when
     * running from an IDE without Maven resource filtering), the fallback value is returned.
     *
     * @param pluginClass the plugin's main class, used to locate the resource in the correct package
     * @param fallback the version to return if the resource cannot be resolved
     * @return the resolved version string, or the fallback
     */
    public static String resolve(Class<?> pluginClass, String fallback) {
        try (InputStream is = pluginClass.getResourceAsStream(RESOURCE_NAME)) {
            if (is == null) {
                LOG.debug(
                        "No '{}' found next to {}, using fallback version '{}'.",
                        RESOURCE_NAME,
                        pluginClass.getName(),
                        fallback);
                return fallback;
            }
            Properties props = new Properties();
            props.load(is);
            String version = props.getProperty(PROPERTY_KEY);
            if (version == null || version.isBlank() || version.startsWith("${")) {
                LOG.debug(
                        "Property '{}' in '{}' is unfiltered or blank for {}, using fallback version '{}'.",
                        PROPERTY_KEY,
                        RESOURCE_NAME,
                        pluginClass.getName(),
                        fallback);
                return fallback;
            }
            return version;
        } catch (IOException e) {
            LOG.warn(
                    "Failed to read '{}' for {}, using fallback version '{}': {}",
                    RESOURCE_NAME,
                    pluginClass.getName(),
                    fallback,
                    e.getMessage());
            return fallback;
        }
    }
}

