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

/**
 * Service Provider Interface (SPI) for ktestify plugins.
 *
 * <p>A plugin extends ktestify with new capabilities — typically a new transport (e.g. Azure Blob Storage, IBM MQ,
 * Amazon S3) plus a set of Cucumber step definitions that exercise it.
 *
 * <h2>How to implement a plugin</h2>
 *
 * <ol>
 *   <li>Create a class implementing {@code KtestifyPlugin}.
 *   <li>Declare it in {@code META-INF/services/io.github.ktestify.plugin.KtestifyPlugin} inside your JAR.
 *   <li>Read your plugin-specific config via {@link PluginContext#getConfig()} using your own
 *       {@code ktestify.plugins.<plugin-id>} HOCON subtree (defined in your {@code reference.conf}).
 *   <li>Register your Cucumber step package by returning it from {@link #getGluePackage()}.
 * </ol>
 *
 * <h2>Loading</h2>
 *
 * <ul>
 *   <li><b>First-party plugins</b> (shipped with {@code ktestify-cucumber} as Maven dependencies) are discovered via
 *       {@link java.util.ServiceLoader} on the current classloader. The Shade {@code ServicesResourceTransformer}
 *       merges all {@code META-INF/services} files automatically.
 *   <li><b>Third-party plugins</b> are placed as {@code .jar} files in the directory configured by
 *       {@code ktestify.plugins.dir} (default: {@code /workspace/plugins}). Each JAR is loaded via a
 *       {@link java.net.URLClassLoader} and its service descriptors are discovered independently.
 * </ul>
 *
 * <h2>Lifecycle</h2>
 *
 * <ol>
 *   <li>{@link #initialize(PluginContext)} — called once at JVM startup, before any Cucumber scenario runs.
 *   <li>Plugin steps execute normally during scenario runs.
 *   <li>{@link #shutdown()} — called once on JVM shutdown.
 * </ol>
 *
 * @since 1.1.0
 * @see PluginContext
 * @see PluginRegistry
 */
public interface KtestifyPlugin {

    /**
     * Returns a stable, unique identifier for this plugin. Use kebab-case (e.g. {@code azure-blob}, {@code ibm-mq}).
     *
     * <p>This ID is used in log messages and as the HOCON config subtree key ({@code ktestify.plugins.<id>}).
     *
     * @return the plugin identifier — never {@code null} or blank
     */
    String getId();

    /**
     * Returns the plugin version string (e.g. {@code "1.0.0"}). Used for informational logging only.
     *
     * @return the plugin version
     */
    String getVersion();

    /**
     * Returns the display name of the plugin author or maintaining team.
     *
     * <p>Shown in the ktestify startup banner alongside the plugin ID and version so operators can immediately identify
     * who is responsible for each loaded plugin. Override this in your implementation to provide a real name.
     *
     * @return the author name — {@code "unknown"} by default
     */
    default String getAuthorName() {
        return "unknown";
    }

    /**
     * Returns the contact email address of the plugin author or maintaining team.
     *
     * <p>Displayed next to {@link #getAuthorName()} in the plugin registry startup log. Override this to provide a real
     * email address.
     *
     * @return the author email — empty string by default
     */
    default String getAuthorEmail() {
        return "";
    }

    /**
     * Returns the fully-qualified Java package that contains this plugin's Cucumber step definitions (e.g.
     * {@code "io.github.ktestify.azureblob.steps"}).
     *
     * <p>The ktestify runtime injects this package as a {@code --glue} argument to the Cucumber CLI so step definitions
     * are discovered automatically — no manual configuration required.
     *
     * <p>Return {@code null} or an empty string if the plugin does not contribute any step definitions.
     *
     * @return the Cucumber glue package, or {@code null} if none
     */
    String getGluePackage();

    /**
     * Initializes the plugin. Called once at JVM startup, before any Cucumber scenario runs.
     *
     * <p>Use this method to validate configuration, create shared clients/connections, or register resources that must
     * outlive individual scenarios.
     *
     * @param context the plugin context providing access to the loaded {@link io.github.ktestify.config.KtestifyConfig}
     * @throws io.github.ktestify.exceptions.PluginException if initialization fails and the run should be aborted
     */
    void initialize(PluginContext context);

    /**
     * Shuts down the plugin. Called once on JVM shutdown, after all Cucumber scenarios have finished.
     *
     * <p>Implementations should release all resources (connections, thread pools, etc.) and must not throw exceptions —
     * log and swallow instead.
     */
    void shutdown();
}
