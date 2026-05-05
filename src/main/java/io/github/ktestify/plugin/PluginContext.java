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

import io.github.ktestify.config.KtestifyConfig;

/**
 * Context object passed to a {@link KtestifyPlugin} during {@link KtestifyPlugin#initialize(PluginContext)}.
 *
 * <p>Provides the plugin with controlled access to the framework's loaded configuration. Plugins must <em>only</em>
 * read their own HOCON subtree ({@code ktestify.plugins.<plugin-id>}) and must not mutate any shared state.
 *
 * <h2>Reading plugin-specific config</h2>
 *
 * <pre>
 * // Inside MyPlugin.initialize(PluginContext ctx)
 * Config raw = ctx.getConfig().getRaw();
 * if (raw.hasPath("ktestify.plugins.my-plugin")) {
 *     Config pluginCfg = raw.getConfig("ktestify.plugins.my-plugin");
 *     String endpoint = pluginCfg.getString("endpoint");
 * }
 * </pre>
 *
 * @since 1.1.0
 * @see KtestifyPlugin
 * @see KtestifyConfig#getRaw()
 */
public interface PluginContext {

    /**
     * Returns the fully loaded {@link KtestifyConfig} singleton.
     *
     * <p>Plugins should call {@link KtestifyConfig#getRaw()} on the returned instance and navigate to their own
     * {@code ktestify.plugins.<id>} subtree. The full config object is provided (rather than a pre-sliced subtree) so
     * plugins can also access shared settings (e.g. {@code ktestify.framework.directories.assets}) when needed.
     *
     * @return the loaded framework configuration — never {@code null}
     */
    KtestifyConfig getConfig();
}
