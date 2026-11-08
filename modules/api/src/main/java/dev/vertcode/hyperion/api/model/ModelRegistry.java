package dev.vertcode.hyperion.api.model;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

import dev.vertcode.hyperion.api.model.source.ModelSource;

/**
 * Every model blueprint Hyperion knows, by key.
 *
 * <p>Models in Hyperion's {@code models/} folder load on startup: {@code models/dragon.bbmodel}
 * becomes {@code hyperion:dragon}, {@code models/bosses/dragon.bbmodel} becomes
 * {@code bosses:dragon}. Other plugins register their own with {@link #register}, usually from a
 * file inside their jar.
 *
 * <p>Loading runs off the server thread and a finished blueprint is published in one step, so a
 * lookup never sees a half-loaded model. A blueprint registered after startup joins the resource
 * pack the next time it is built. Every method is safe from any thread.
 */
@ApiStatus.NonExtendable
public interface ModelRegistry {

    /**
     * Find a blueprint by key.
     *
     * @param key the blueprint's key, such as {@code bosses:dragon}
     * @return the blueprint, or empty if none is loaded under that key
     */
    Optional<ModelBlueprint> find(Key key);

    /**
     * Return every loaded blueprint.
     *
     * @return a snapshot of the loaded blueprints
     */
    @Unmodifiable Collection<ModelBlueprint> all();

    /**
     * Load a model and register it under a key owned by a plugin.
     *
     * <p>The blueprint is dropped when the owner disables.
     *
     * @param owner the plugin the blueprint belongs to
     * @param key the key to register it under
     * @param source where the model is read from
     * @return a future completed off the server thread with the blueprint, or exceptionally if
     *     the model cannot be read or the key is taken by another plugin
     */
    CompletableFuture<ModelBlueprint> register(Plugin owner, Key key, ModelSource source);

    /**
     * Remove a blueprint. Live models of it are removed with
     * {@link RemovalReason#BLUEPRINT_UNREGISTERED}.
     *
     * @param key the blueprint's key
     * @return whether a blueprint was removed
     */
    boolean unregister(Key key);

    /**
     * Remove every blueprint a plugin registered. Hyperion calls this when the plugin disables.
     *
     * @param owner the plugin whose blueprints to remove
     */
    void unregisterAll(Plugin owner);

    /**
     * Read one blueprint again from its source and swap it in.
     *
     * <p>Live models of it rebuild onto the new blueprint on their next frame, keeping their
     * appearance and the overrides of bones that still exist.
     *
     * @param key the blueprint's key
     * @return a future completed with the new blueprint, or exceptionally if it cannot be read
     */
    CompletableFuture<ModelBlueprint> reload(Key key);

    /**
     * Read every blueprint again from its source.
     *
     * @return a future completed once every blueprint has been read and published
     */
    CompletableFuture<Void> reloadAll();
}
