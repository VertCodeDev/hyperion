package dev.vertcode.hyperion.api.model.behavior;

import java.util.Collection;
import java.util.Optional;

import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Every bone behavior type, built in or added by a plugin.
 *
 * <p>Types are matched when a model spawns, so a type registered late applies to every model
 * spawned after it. When the owner disables, its behaviors are removed from live bones with
 * {@code onRemove(PLUGIN_DISABLED)}. Every method is safe from any thread.
 */
@ApiStatus.NonExtendable
public interface BehaviorRegistry {

    /**
     * Register a behavior type owned by a plugin.
     *
     * @param owner the plugin the type belongs to
     * @param type the behavior type
     * @throws IllegalArgumentException if another type already uses the key
     */
    void register(Plugin owner, BehaviorType<?> type);

    /**
     * Remove every type a plugin registered. Hyperion calls this when the plugin disables.
     *
     * @param owner the plugin whose types to remove
     */
    void unregisterAll(Plugin owner);

    /**
     * Find a behavior type by key.
     *
     * @param key the type's key
     * @return the type, or empty if none is registered under that key
     */
    Optional<BehaviorType<?>> find(Key key);

    /**
     * Return every registered type.
     *
     * @return a snapshot of the types
     */
    @Unmodifiable Collection<BehaviorType<?>> all();
}
