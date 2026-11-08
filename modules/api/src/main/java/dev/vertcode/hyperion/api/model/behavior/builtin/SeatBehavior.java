package dev.vertcode.hyperion.api.model.behavior.builtin;

import java.util.Optional;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.behavior.BoneBehavior;

/**
 * A bone an entity can ride, moving with the bone's animation.
 *
 * <p>{@link #mount} and {@link #dismount} run on the region that owns the model.
 */
@ApiStatus.NonExtendable
public interface SeatBehavior extends BoneBehavior {

    /**
     * Return who rides this seat.
     *
     * @return the passenger, or empty when the seat is free
     */
    Optional<Entity> passenger();

    /**
     * Put an entity in this seat.
     *
     * @param entity the entity to seat
     * @return false when the seat is taken or the entity cannot ride
     * @throws IllegalStateException if this is not the region that owns the model
     */
    boolean mount(Entity entity);

    /**
     * Take the passenger out of this seat. Does nothing when the seat is free.
     *
     * @throws IllegalStateException if this is not the region that owns the model
     */
    void dismount();

    /**
     * Return whether the passenger is kept from leaving by sneaking.
     *
     * @return true when locked
     */
    boolean locked();

    /**
     * Keep the passenger from leaving by sneaking.
     *
     * @param locked true to lock the seat
     * @return this behavior
     */
    SeatBehavior locked(boolean locked);

    /**
     * Return what lets the rider steer the entity carrying the model.
     *
     * @return the controller, or null when the rider only rides along
     */
    @Nullable SeatController controller();

    /**
     * Let the rider steer the entity carrying the model. Safe from any thread. Has no effect on a
     * model on its own, which has nothing to move.
     *
     * @param controller the controller, or null to let the rider only ride along
     * @return this behavior
     */
    SeatBehavior controller(@Nullable SeatController controller);
}
