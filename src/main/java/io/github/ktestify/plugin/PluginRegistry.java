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

import io.github.ktestify.exceptions.PluginException;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers, loads, initializes, and holds all active {@link KtestifyPlugin} instances for the current JVM run.
 *
 * <h2>Two loading phases</h2>
 *
 * <ol>
 *   <li><b>Classpath (Phase 1)</b> — {@link ServiceLoader#load(Class)} on the current thread's context classloader.
 *       Picks up all plugins that are on the classpath (i.e. bundled as Maven dependencies in the fat JAR). The Shade
 *       {@code ServicesResourceTransformer} ensures all {@code META-INF/services} descriptors survive JAR merging.
 *   <li><b>External directory (Phase 2)</b> — scans the directory configured by {@code ktestify.plugins.dir} (default
 *       {@code /workspace/plugins}) for {@code *.jar} files. Each JAR is added to a shared {@link URLClassLoader}
 *       (parent = current context classloader) and its plugins are discovered independently via
 *       {@link ServiceLoader#load(Class, ClassLoader)}.
 * </ol>
 *
 * <h2>Initialization order</h2>
 *
 * Plugins are initialized in discovery order: classpath plugins first, then external plugins in filesystem order. A
 * plugin whose {@link KtestifyPlugin#initialize(PluginContext)} throws will cause an immediate {@link PluginException}
 * — the run is aborted.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * PluginContext ctx = () -> KtestifyConfig.getOrLoad();
 * PluginRegistry registry = PluginRegistry.load(ctx);
 *
 * // Inject plugin glue packages into Cucumber CLI
 * registry.getGluePackages().forEach(pkg -> args.add("--glue"); args.add(pkg));
 *
 * // On JVM shutdown
 * registry.shutdown();
 * </pre>
 *
 * @since 1.1.0
 * @see KtestifyPlugin
 * @see PluginContext
 */
public final class PluginRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(PluginRegistry.class);

    /** HOCON path for the external plugin directory. */
    private static final String PLUGINS_DIR_PATH = "ktestify.plugins.dir";

    private final List<KtestifyPlugin> plugins;

    private PluginRegistry(List<KtestifyPlugin> plugins) {
        this.plugins = Collections.unmodifiableList(plugins);
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Discovers, loads, and initializes all plugins.
     *
     * @param ctx the plugin context (config + services) handed to each plugin at init time
     * @return a fully initialized {@code PluginRegistry}
     * @throws PluginException if any plugin fails to initialize
     */
    public static PluginRegistry load(PluginContext ctx) {
        List<KtestifyPlugin> all = new ArrayList<>();

        LOG.info("╔══ KTestify Plugin System ═════════════════════════════════════");

        // Phase 1 — classpath / fat-jar plugins
        loadFromClasspath(ctx, all);

        // Phase 2 — external plugin directory
        String pluginsDir = resolvePluginsDir(ctx);
        loadFromDirectory(pluginsDir, ctx, all);

        if (all.isEmpty()) {
            LOG.info("║  No plugins loaded.");
        } else {
            LOG.info(
                    "║  {} plugin(s) active: [{}]",
                    all.size(),
                    all.stream().map(p -> p.getId() + "@" + p.getVersion()).collect(Collectors.joining(", ")));
        }
        LOG.info("╚═══════════════════════════════════════════════════════════════");

        return new PluginRegistry(all);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns an unmodifiable list of all loaded and initialized plugins.
     *
     * @return the loaded plugins in initialization order
     */
    public List<KtestifyPlugin> getPlugins() {
        return plugins;
    }

    /**
     * Returns the Cucumber glue packages contributed by all loaded plugins.
     *
     * <p>Each non-blank value returned by {@link KtestifyPlugin#getGluePackage()} is included. The caller should add
     * each as a separate {@code --glue <package>} argument to the Cucumber CLI.
     *
     * @return an ordered list of glue package names — may be empty, never {@code null}
     */
    public List<String> getGluePackages() {
        return plugins.stream()
                .map(KtestifyPlugin::getGluePackage)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * Shuts down all plugins in reverse initialization order.
     *
     * <p>Exceptions thrown by individual plugins are caught, logged as warnings, and swallowed so the remaining plugins
     * can still be shut down cleanly.
     */
    public void shutdown() {
        LOG.info("Shutting down {} plugin(s)…", plugins.size());
        List<KtestifyPlugin> reversed = new ArrayList<>(plugins);
        Collections.reverse(reversed);
        for (KtestifyPlugin plugin : reversed) {
            try {
                plugin.shutdown();
                LOG.info("Plugin '{}' shut down.", plugin.getId());
            } catch (Exception e) {
                LOG.warn("Error shutting down plugin '{}' — ignored: {}", plugin.getId(), e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Phase 1: discovers plugins already on the current classloader (fat-jar deps). */
    private static void loadFromClasspath(PluginContext ctx, List<KtestifyPlugin> target) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        ServiceLoader<KtestifyPlugin> loader = ServiceLoader.load(KtestifyPlugin.class, cl);
        int before = target.size();
        for (KtestifyPlugin plugin : loader) {
            LOG.info(
                    "║  [classpath] Discovered plugin: {} v{} — author: {} <{}>",
                    plugin.getId(),
                    plugin.getVersion(),
                    plugin.getAuthorName(),
                    plugin.getAuthorEmail());
            initPlugin(plugin, ctx);
            target.add(plugin);
        }
        int loaded = target.size() - before;
        LOG.debug("Phase 1 (classpath): {} plugin(s) discovered.", loaded);
    }

    /**
     * Phase 2: scans an external directory for {@code .jar} files, loads them via a {@link URLClassLoader}, and
     * discovers plugins inside each JAR.
     */
    private static void loadFromDirectory(String dirPath, PluginContext ctx, List<KtestifyPlugin> target) {
        if (dirPath == null || dirPath.isBlank()) {
            LOG.debug("Phase 2 (external): plugins dir not configured — skipping.");
            return;
        }

        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            LOG.debug("Phase 2 (external): directory '{}' does not exist — no external plugins loaded.", dirPath);
            return;
        }

        File[] jars = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            LOG.debug("Phase 2 (external): no *.jar files found in '{}' — skipping.", dirPath);
            return;
        }

        LOG.info("║  [external] Scanning '{}' — {} JAR(s) found.", dirPath, jars.length);

        // Build a single URLClassLoader for all external jars (parent = current context CL)
        URL[] urls = Arrays.stream(jars)
                .map(f -> {
                    try {
                        return f.toURI().toURL();
                    } catch (Exception e) {
                        throw new PluginException("Cannot convert plugin JAR path to URL: " + f.getAbsolutePath(), e);
                    }
                })
                .toArray(URL[]::new);

        URLClassLoader pluginCL =
                new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());

        ServiceLoader<KtestifyPlugin> loader = ServiceLoader.load(KtestifyPlugin.class, pluginCL);
        int before = target.size();
        for (KtestifyPlugin plugin : loader) {
            LOG.info(
                    "║  [external] Discovered plugin: {} v{} — author: {} <{}> (from '{}')",
                    plugin.getId(),
                    plugin.getVersion(),
                    plugin.getAuthorName(),
                    plugin.getAuthorEmail(),
                    dirPath);
            initPlugin(plugin, ctx);
            target.add(plugin);
        }
        int loaded = target.size() - before;
        LOG.debug("Phase 2 (external): {} plugin(s) discovered from '{}'.", loaded, dirPath);
    }

    /** Calls {@link KtestifyPlugin#initialize(PluginContext)}, wrapping any exception in a {@link PluginException}. */
    private static void initPlugin(KtestifyPlugin plugin, PluginContext ctx) {
        try {
            plugin.initialize(ctx);
            LOG.info("║  Plugin '{}' initialized successfully.", plugin.getId());
        } catch (PluginException e) {
            throw e; // already wrapped
        } catch (Exception e) {
            throw new PluginException("Plugin '" + plugin.getId() + "' failed to initialize: " + e.getMessage(), e);
        }
    }

    /** Reads the configured plugins directory from HOCON, returning {@code null} if the path is absent. */
    private static String resolvePluginsDir(PluginContext ctx) {
        try {
            return ctx.getConfig().getRaw().getString(PLUGINS_DIR_PATH);
        } catch (Exception e) {
            LOG.debug("Could not read '{}' from config — external plugins disabled.", PLUGINS_DIR_PATH);
            return null;
        }
    }
}
