package dev.vertcode.hyperion.api.model.behavior;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.RemovalReason;

/**
 * Logic that lives on one bone of a live model, from spawn to removal.
 *
 * <p>Every hook runs on the region that owns the model and has a default that does nothing, so
 * override only what you need. Keep the hooks cheap, {@link #onTick()} most of all: it runs every
 * tick for every model that has viewers.
 */
public interface BoneBehavior {

    /**
     * React to the model spawning, before any viewer sees it.
     */
    @ApiStatus.OverrideOnly
    default void onSpawn() {
    }

    /**
     * Run once per tick while the model has at least one viewer.
     */
    @ApiStatus.OverrideOnly
    default void onTick() {
    }

    /**
     * React to a player starting to see the model.
     *
     * @param viewer the player who can now see it
     */
    @ApiStatus.OverrideOnly
    default void onShow(final Player viewer) {
    }

    /**
     * React to a player no longer seeing the model.
     *
     * @param viewer the player who can no longer see it
     */
    @ApiStatus.OverrideOnly
    default void onHide(final Player viewer) {
    }

    /**
     * Clean up as the model is removed. Release anything the behavior holds here.
     *
     * @param reason why the model was removed
     */
    @ApiStatus.OverrideOnly
    default void onRemove(final RemovalReason reason) {
    }
}
