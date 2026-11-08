package dev.vertcode.hyperion.api.model.behavior.builtin;

import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * The controller behind {@link SeatController#walking} and {@link SeatController#flying}.
 *
 * @param speed the speed in blocks per tick
 * @param jumpVelocity the upward speed of a jump in blocks per tick, unused when flying
 * @param flies whether the vehicle flies
 */
record SteeringController(float speed, float jumpVelocity, boolean flies) implements SeatController {

    private static final String NOT_POSITIVE = "A seat controller needs a speed above 0, got %s";

    /**
     * Create a controller.
     *
     * @param speed the speed in blocks per tick, above 0
     * @param jumpVelocity the upward speed of a jump in blocks per tick
     * @param flies whether the vehicle flies
     * @throws IllegalArgumentException if the speed is not above 0
     */
    SteeringController {
        if (speed <= 0f) {
            throw new IllegalArgumentException(String.format(NOT_POSITIVE, speed));
        }
    }

    @Override
    public Vector steer(final Player rider, final Entity vehicle, final Input input) {
        Location view = rider.getLocation();
        double yaw = Math.toRadians(view.getYaw());
        Vector ahead = this.flies ? view.getDirection() : new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector left = new Vector(Math.cos(yaw), 0, Math.sin(yaw));

        Vector movement = ahead.multiply(axis(input.isForward(), input.isBackward()))
                .add(left.multiply(axis(input.isLeft(), input.isRight())));
        if (movement.lengthSquared() > 1) {
            movement.normalize();
        }

        movement.multiply(this.speed);

        if (this.flies) {
            return input.isJump() ? movement.setY(movement.getY() + this.speed) : movement;
        }

        // Walking keeps the vehicle's own fall, so gravity still pulls it down off a ledge.
        double vertical = input.isJump() && vehicle.isOnGround() ? this.jumpVelocity : vehicle.getVelocity().getY();

        return movement.setY(vertical);
    }

    /**
     * Turn two opposite keys into a direction.
     *
     * @param positive the key that counts as 1
     * @param negative the key that counts as -1
     * @return 1, -1, or 0 when both or neither are held
     */
    private static int axis(final boolean positive, final boolean negative) {
        return (positive ? 1 : 0) - (negative ? 1 : 0);
    }
}
