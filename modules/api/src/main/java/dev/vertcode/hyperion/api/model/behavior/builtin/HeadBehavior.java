package dev.vertcode.hyperion.api.model.behavior.builtin;

import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.behavior.BoneBehavior;

/**
 * A bone that turns with the entity's head, or towards a point you choose.
 *
 * <p>Setters are safe from any thread and apply on the next frame.
 */
@ApiStatus.NonExtendable
public interface HeadBehavior extends BoneBehavior {

    /**
     * Return whether the bone follows the head.
     *
     * @return true when following
     */
    boolean enabled();

    /**
     * Start or stop following. When stopped, the bone keeps only its animation.
     *
     * @param enabled true to follow
     * @return this behavior
     */
    HeadBehavior enabled(boolean enabled);

    /**
     * Return how far the bone may turn left or right from the body.
     *
     * @return the limit in degrees
     */
    float maxYaw();

    /**
     * Limit how far the bone may turn left or right from the body.
     *
     * @param degrees the limit, 0 to 180
     * @return this behavior
     */
    HeadBehavior maxYaw(float degrees);

    /**
     * Return how far the bone may look up or down.
     *
     * @return the limit in degrees
     */
    float maxPitch();

    /**
     * Limit how far the bone may look up or down.
     *
     * @param degrees the limit, 0 to 90
     * @return this behavior
     */
    HeadBehavior maxPitch(float degrees);

    /**
     * Return the point the bone looks at in place of the entity's head.
     *
     * @return a copy of the target, or null when following the head
     */
    @Nullable Location target();

    /**
     * Look at a point in place of the entity's head. This is how a model on its own looks around.
     *
     * @param target the point to look at, or null to follow the head again
     * @return this behavior
     */
    HeadBehavior target(@Nullable Location target);
}
