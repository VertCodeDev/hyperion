package dev.vertcode.hyperion.api.effect;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;

/**
 * An effect that plays: where it is, and a way to stop it.
 *
 * <p>Safe from any thread.
 */
@ApiStatus.NonExtendable
public interface EffectHandle {

    /**
     * Return the effect that plays.
     *
     * @return the effect
     */
    Effect effect();

    /**
     * Return what the effect is attached to.
     *
     * @return the anchor
     */
    EffectAnchor anchor();

    /**
     * Return where the anchor is, as of the last tick.
     *
     * @return a copy of the location
     */
    Location location();

    /**
     * Return how many ticks the effect has played.
     *
     * @return the ticks, counted from 0 again on every loop
     */
    int tick();

    /**
     * Return the player the play belongs to.
     *
     * @return the owner's id, or empty when the play named none
     * @see EffectOptions#owner
     */
    Optional<UUID> owner();

    /**
     * Return whether the effect has shown what it is about.
     *
     * @return true from its {@linkplain Effect#reveal() reveal} tick on
     */
    boolean revealed();

    /**
     * Return whether the effect still plays.
     *
     * @return false once it ended or was stopped
     */
    boolean isPlaying();

    /**
     * Stop the effect on its next tick. Does nothing when it already ended.
     */
    void stop();
}
