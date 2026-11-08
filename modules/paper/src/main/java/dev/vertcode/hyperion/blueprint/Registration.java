package dev.vertcode.hyperion.blueprint;

import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.source.ModelSource;

/**
 * Where a key's model comes from, kept so it can be loaded again even after a failed load.
 *
 * @param owner the plugin that registered it, or null for a file in Hyperion's own folder
 * @param source where the model is read from
 */
record Registration(@Nullable Plugin owner, ModelSource source) {
}
