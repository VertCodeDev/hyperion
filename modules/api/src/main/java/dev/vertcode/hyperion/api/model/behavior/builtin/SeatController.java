package dev.vertcode.hyperion.api.model.behavior.builtin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Lets the player in a seat steer the entity carrying the model: walk, jump or fly.
 *
 * <p>Works when the model rides an entity and the passenger is a player. Every tick Hyperion
 * reads the rider's keys, asks {@link #steer} where the vehicle goes and moves it there, holding
 * back the vehicle's own AI while a player steers. Called on the vehicle's region, so keep it
 * cheap. The ready-made controllers cover most mounts:
 *
 * <pre>{@code
 * seat.controller(SeatController.walking(0.25f, 0.5f));
 * dragonSeat.controller(SeatController.flying(0.6f));
 * }</pre>
 */
public interface SeatController {

    /**
     * Steer like a horse: move where the rider looks, strafe with the side keys, jump from the
     * ground.
     *
     * @param speed the speed in blocks per tick, above 0; 0.2 is about a walking player
     * @param jumpVelocity the upward speed of a jump in blocks per tick; 0.42 is a player's jump
     * @return the controller
     * @throws IllegalArgumentException if the speed is not above 0
     */
    static SeatController walking(final float speed, final float jumpVelocity) {
        return new SteeringController(speed, jumpVelocity, false);
    }

    /**
     * Steer like a creative flyer: move along the rider's look, climb with jump, no gravity.
     *
     * @param speed the speed in blocks per tick, above 0
     * @return the controller
     * @throws IllegalArgumentException if the speed is not above 0
     */
    static SeatController flying(final float speed) {
        return new SteeringController(speed, 0f, true);
    }

    /**
     * Work out where the vehicle goes this tick.
     *
     * @param rider the player steering
     * @param vehicle the entity carrying the model
     * @param input the keys the rider holds
     * @return the vehicle's velocity in blocks per tick
     */
    Vector steer(Player rider, Entity vehicle, RiderInput input);

    /**
     * Return whether the vehicle flies: no gravity while a player steers.
     *
     * @return true for a flying vehicle
     */
    default boolean flies() {
        return false;
    }

    /**
     * Return whether the vehicle turns to face where the rider looks.
     *
     * @return true to turn with the rider
     */
    default boolean turnsWithRider() {
        return true;
    }

    /**
     * Return whether the rider can hurt the entity they ride.
     *
     * @return true to let the rider hit their own mount
     */
    default boolean riderCanHurtVehicle() {
        return false;
    }
}
