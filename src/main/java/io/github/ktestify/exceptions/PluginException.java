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
package io.github.ktestify.exceptions;

/**
 * Thrown when a {@link io.github.ktestify.plugin.KtestifyPlugin} fails to load or initialize.
 *
 * <p>A {@code PluginException} during
 * {@link io.github.ktestify.plugin.PluginRegistry#load(io.github.ktestify.plugin.PluginContext)} is fatal — the run is
 * aborted immediately. This prevents silent partial initialization where some plugins are active and others are not.
 *
 * @since 1.1.0
 */
public class PluginException extends RuntimeException {

    public PluginException(String message) {
        super(message);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
