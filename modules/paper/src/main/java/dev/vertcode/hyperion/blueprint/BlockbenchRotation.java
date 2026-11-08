package dev.vertcode.hyperion.blueprint;

import org.joml.Quaternionf;
import org.joml.Vector3fc;

/**
 * Blockbench's rotations: Euler angles in degrees, applied in ZYX order.
 *
 * <p>Every built-in Blockbench format turns X first, then Y, then Z, each around the fixed
 * model axes, for groups, cubes and locators alike.
 */
public final class BlockbenchRotation {

    private BlockbenchRotation() {
    }

    /**
     * Turn Blockbench's Euler angles into a rotation.
     *
     * @param degrees the angles around X, Y and Z in degrees
     * @return the rotation
     */
    public static Quaternionf of(final Vector3fc degrees) {
        return new Quaternionf().rotationZYX(
                (float) Math.toRadians(degrees.z()),
                (float) Math.toRadians(degrees.y()),
                (float) Math.toRadians(degrees.x())
        );
    }
}
