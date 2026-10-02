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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Discovers, loads, initializes, and holds all active {@link KtestifyPlugin} instances for the current JVM run.
 *
 * <h2>Two loading phases</h2>
 *
 * <ol>
 *   <li><b>Classpath (Phase 1)</b>: {@link ServiceLoader#load(Class, ClassLoader)} on the current thread's context
 *       classloader. Picks up all plugins that are on the classpath (i.e. bundled as Maven dependencies in the fat
 *       JAR). The Shade {@code ServicesResourceTransformer} ensures all {@code META-INF/services} descriptors survive
 *       JAR merging.
 *   <li><b>External directory (Phase 2)</b>: scans the directory configured by {@code ktestify.plugins.dir} (default
 *       {@code /workspace/plugins}) for {@code *.jar} files. All JARs are added to a shared {@link URLClassLoader}
 *       (parent = current context classloader). Only providers whose class is defined by that external classloader are
 *       loaded, so classpath plugins visible through parent delegation are never initialized a second time.
 * </ol>
 *
 * <h2>Initialization order and failure handling</h2>
 *
 * <p>Plugins are initialized in discovery order: classpath plugins first, then external plugins. Each plugin is
 * initialized with the thread context classloader set to the classloader that defined it.
 *
 * <p>Two plugins declaring the same {@link KtestifyPlugin#getId() id} abort the run with a {@link PluginException}. A
 * plugin whose {@link KtestifyPlugin#initialize(PluginContext)} throws, or a broken {@code META-INF/services} entry,
 * also aborts the run. In every failure case, plugins that were already initialized are shut down in reverse order and
 * the external classloader is closed before the exception propagates.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * PluginContext ctx = () -> KtestifyConfig.getOrLoad();
 * PluginRegistry registry = PluginRegistry.load(ctx);
 *
 * // Inject plugin glue packages into Cucumber CLI
 * registry.getGluePackages().forEach(pkg -> { args.add("--glue"); args.add(pkg); });
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

    /**
     * The {@link URLClassLoader} created for external plugin JARs, or {@code null} if no external JARs were found.
     *
     * <p>Kept open for the lifetime of the registry because loaded plugin classes reference it. It is closed in
     * {@link #shutdown()} after all plugins have been shut down.
     */
    private final URLClassLoader externalClassLoader;

    /** Guards {@link #shutdown()} so that plugins are shut down at most once. */
    private final AtomicBoolean shutDown = new AtomicBoolean(false);

    private PluginRegistry(List<KtestifyPlugin> plugins, URLClassLoader externalClassLoader) {
        this.plugins = Collections.unmodifiableList(plugins);
        this.externalClassLoader = externalClassLoader;
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Discovers, loads, and initializes all plugins.
     *
     * @param ctx the plugin context (config + services) handed to each plugin at init time
     * @return a fully initialized {@code PluginRegistry}
     * @throws PluginException if a plugin fails to initialize, a provider cannot be instantiated, or two plugins share
     *     the same id
     */
    public static PluginRegistry load(PluginContext ctx) {
        List<KtestifyPlugin> all = new ArrayList<>();
        Map<String, KtestifyPlugin> byId = new HashMap<>();
        URLClassLoader externalCL = null;

        LOG.info("Loading plugins...");

        try {
            // Phase 1: classpath / fat-jar plugins
            ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
            discover("classpath", ServiceLoader.load(KtestifyPlugin.class, contextCL), null, ctx, all, byId);

            // Phase 2: external plugin directory
            externalCL = createExternalClassLoader(resolvePluginsDir(ctx), contextCL);
            if (externalCL != null) {
                discover("external", ServiceLoader.load(KtestifyPlugin.class, externalCL), externalCL, ctx, all, byId);
            }
        } catch (RuntimeException | ServiceConfigurationError e) {
            LOG.error("Plugin loading failed, rolling back {} initialized plugin(s).", all.size());
            shutdownAll(all);
            if (externalCL != null) {
                closeClassLoaderQuietly(externalCL);
            }
            if (e instanceof PluginException pe) {
                throw pe;
            }
            throw new PluginException("Plugin discovery failed: " + e.getMessage(), e);
        }

        if (all.isEmpty()) {
            LOG.info("No plugins loaded.");
        } else {
            LOG.info(
                    "Plugin system ready: {} plugin(s) active [{}]",
                    all.size(),
                    all.stream().map(p -> p.getId() + "@" + p.getVersion()).collect(Collectors.joining(", ")));
        }

        return new PluginRegistry(all, externalCL);
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
     * <p>Each non-blank value returned by {@link KtestifyPlugin#getGluePackage()} is included once. The caller should
     * add each as a separate {@code --glue <package>} argument to the Cucumber CLI.
     *
     * @return an ordered, duplicate-free list of glue package names; may be empty, never {@code null}
     */
    public List<String> getGluePackages() {
        return plugins.stream()
                .map(KtestifyPlugin::getGluePackage)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Shuts down all plugins in reverse initialization order, then closes the external plugin {@link URLClassLoader} if
     * one was created.
     *
     * <p>Exceptions thrown by individual plugins are caught and logged as warnings so the remaining plugins can still
     * be shut down cleanly. Calling this method more than once has no further effect.
     */
    public void shutdown() {
        if (!shutDown.compareAndSet(false, true)) {
            LOG.debug("Plugin registry already shut down, ignoring repeated call.");
            return;
        }
        shutdownAll(plugins);

        // Close the external URLClassLoader to release file handles on plugin JARs.
        if (externalClassLoader != null) {
            closeClassLoaderQuietly(externalClassLoader);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Iterates over the providers of {@code loader}, instantiates and initializes each one, and appends it to
     * {@code target}.
     *
     * @param phase a short label used in log messages
     * @param loader the service loader to iterate
     * @param requiredDefiningLoader when non-null, providers whose class is not defined by this classloader are skipped
     *     (they were already discovered through the parent classloader in Phase 1)
     * @param ctx the plugin context
     * @param target the list receiving initialized plugins
     * @param byId index used to detect duplicate plugin ids
     */
    private static void discover(
            String phase,
            ServiceLoader<KtestifyPlugin> loader,
            ClassLoader requiredDefiningLoader,
            PluginContext ctx,
            List<KtestifyPlugin> target,
            Map<String, KtestifyPlugin> byId) {
        int before = target.size();
        for (ServiceLoader.Provider<KtestifyPlugin> provider : loader.stream().toList()) {
            Class<? extends KtestifyPlugin> type = provider.type();
            if (requiredDefiningLoader != null && type.getClassLoader() != requiredDefiningLoader) {
                LOG.debug("[{}] Skipping '{}': already visible on the classpath.", phase, type.getName());
                continue;
            }

            KtestifyPlugin plugin = provider.get();
            KtestifyPlugin existing = byId.putIfAbsent(plugin.getId(), plugin);
            if (existing != null) {
                throw new PluginException("Duplicate plugin id '" + plugin.getId() + "': provided by both "
                        + existing.getClass().getName() + " and " + type.getName() + ".");
            }

            LOG.info(
                    "[{}] Plugin discovered: {} v{} (author: {} <{}>)",
                    phase,
                    plugin.getId(),
                    plugin.getVersion(),
                    plugin.getAuthorName(),
                    plugin.getAuthorEmail());
            initPlugin(plugin, ctx);
            target.add(plugin);
        }
        LOG.debug("Phase '{}': {} plugin(s) loaded.", phase, target.size() - before);
    }

    /**
     * Builds a single {@link URLClassLoader} for every {@code *.jar} file in {@code dirPath}.
     *
     * @param dirPath the configured plugins directory, may be {@code null} or blank
     * @param parent the parent classloader
     * @return the classloader, or {@code null} when the directory is not configured, missing, or contains no JARs
     */
    private static URLClassLoader createExternalClassLoader(String dirPath, ClassLoader parent) {
        if (dirPath == null || dirPath.isBlank()) {
            LOG.debug("Phase 2 (external): plugins dir not configured.");
            return null;
        }

        File dir = new File(dirPath);
        if (!dir.isDirectory()) {
            LOG.debug("Phase 2 (external): directory '{}' does not exist.", dirPath);
            return null;
        }

        File[] jars = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            LOG.debug("Phase 2 (external): no *.jar files found in '{}'.", dirPath);
            return null;
        }
        Arrays.sort(jars);

        LOG.info("[external] Scanning '{}': {} JAR(s) found.", dirPath, jars.length);

        URL[] urls = Arrays.stream(jars)
                .map(f -> {
                    try {
                        return f.toURI().toURL();
                    } catch (Exception e) {
                        throw new PluginException("Cannot convert plugin JAR path to URL: " + f.getAbsolutePath(), e);
                    }
                })
                .toArray(URL[]::new);

        return new URLClassLoader("ktestify-external-plugins", urls, parent);
    }

    /** Shuts down {@code list} in reverse order, logging and swallowing individual failures. */
    private static void shutdownAll(List<KtestifyPlugin> list) {
        LOG.info("Shutting down {} plugin(s)...", list.size());
        List<KtestifyPlugin> reversed = new ArrayList<>(list);
        Collections.reverse(reversed);
        for (KtestifyPlugin plugin : reversed) {
            try {
                withContextClassLoader(plugin, plugin::shutdown);
                LOG.info("Plugin '{}' shut down.", plugin.getId());
            } catch (Exception e) {
                LOG.warn("Error shutting down plugin '{}', ignored.", plugin.getId(), e);
            }
        }
    }

    /** Closes a {@link URLClassLoader} silently, logging any failure as a warning. */
    private static void closeClassLoaderQuietly(URLClassLoader cl) {
        try {
            cl.close();
            LOG.debug("External plugin URLClassLoader closed.");
        } catch (Exception e) {
            LOG.warn("Failed to close external plugin URLClassLoader, ignored.", e);
        }
    }

    /** Calls {@link KtestifyPlugin#initialize(PluginContext)}, wrapping any exception in a {@link PluginException}. */
    private static void initPlugin(KtestifyPlugin plugin, PluginContext ctx) {
        try {
            withContextClassLoader(plugin, () -> plugin.initialize(ctx));
            LOG.info("Plugin '{}' initialized.", plugin.getId());
        } catch (PluginException e) {
            throw e; // already wrapped
        } catch (Exception e) {
            throw new PluginException("Plugin '" + plugin.getId() + "' failed to initialize: " + e.getMessage(), e);
        }
    }

    /** Runs {@code action} with the thread context classloader set to the classloader that defined {@code plugin}. */
    private static void withContextClassLoader(KtestifyPlugin plugin, Runnable action) {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        ClassLoader pluginLoader = plugin.getClass().getClassLoader();
        thread.setContextClassLoader(pluginLoader != null ? pluginLoader : previous);
        try {
            action.run();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    /** Reads the configured plugins directory from HOCON, returning {@code null} if the path is absent. */
    private static String resolvePluginsDir(PluginContext ctx) {
        try {
            return ctx.getConfig().getRaw().getString(PLUGINS_DIR_PATH);
        } catch (Exception e) {
            LOG.debug("Could not read '{}' from config, external plugins disabled.", PLUGINS_DIR_PATH);
            return null;
        }
    }
}
