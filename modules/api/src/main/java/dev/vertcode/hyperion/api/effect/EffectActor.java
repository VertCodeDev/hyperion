package dev.vertcode.hyperion.api.effect;

import org.bukkit.Location;
import org.jspecify.annotations.Nullable;

/**
 * One part of one playing effect: what turns the part's values into particles, sounds or
 * anything else.
 *
 * <p>Runs on the thread the effect plays on.
 */
public interface EffectActor {

    /**
     * Act on the part's values. A sampled part gets this every tick with its values eased
     * between keyframes; any other part gets it once at each keyframe, with that keyframe's
     * values over the part's settings.
     *
     * @param tick the effect's tick
     * @param frame the part's values
     */
    void frame(int tick, EffectFrame frame);

    /**
     * Return where the part is now, so other parts can be {@code at} it.
     *
     * @return the location, or null when the part has no place of its own
     */
    default @Nullable Location location() {
        return null;
    }

    /**
     * Clean up as the effect ends.
     */
    default void stop() {
    }
}
