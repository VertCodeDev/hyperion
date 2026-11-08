package dev.vertcode.hyperion.api.model.source;

import java.util.Collection;
import java.util.Optional;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Every model format, built in or added by a plugin. Every method is safe from any thread.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface FormatRegistry {

    /**
     * Register a format owned by a plugin.
     *
     * @param owner the plugin the format belongs to
     * @param format the format
     * @throws IllegalArgumentException if another format already uses the key
     */
    void register(Plugin owner, ModelFormat format);

    /**
     * Remove every format a plugin registered. Hyperion calls this when the plugin disables.
     *
     * @param owner the plugin whose formats to remove
     */
    void unregisterAll(Plugin owner);

    /**
     * Find the format that reads a source. Formats from plugins are asked before the built-in
     * ones.
     *
     * @param source the source
     * @return the format, or empty if none reads it
     */
    Optional<ModelFormat> find(ModelSource source);

    /**
     * Return every registered format.
     *
     * @return a snapshot of the formats
     */
    @Unmodifiable Collection<ModelFormat> all();
}
