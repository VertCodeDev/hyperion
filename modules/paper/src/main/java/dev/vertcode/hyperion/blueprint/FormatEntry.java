package dev.vertcode.hyperion.blueprint;

import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.source.ModelFormat;

/**
 * A format in the {@link HyperionFormatRegistry} and the plugin that added it.
 *
 * @param owner the plugin, or null for the built-in format
 * @param format the format
 */
record FormatEntry(@Nullable Plugin owner, ModelFormat format) {
}
