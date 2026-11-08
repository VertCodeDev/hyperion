package dev.vertcode.hyperion.api.model;

/**
 * Which way a whole model faces, in degrees, as Minecraft measures it.
 *
 * @param pitch the pitch: negative looks up, positive looks down
 * @param yaw the yaw: 0 faces south, 90 faces west
 */
public record ModelRotation(float pitch, float yaw) {

    /**
     * Facing south, level.
     */
    public static final ModelRotation ZERO = new ModelRotation(0f, 0f);

    /**
     * Return a copy with another pitch.
     *
     * @param pitch the pitch in degrees
     * @return the new rotation
     */
    public ModelRotation withPitch(final float pitch) {
        return new ModelRotation(pitch, this.yaw);
    }

    /**
     * Return a copy with another yaw.
     *
     * @param yaw the yaw in degrees
     * @return the new rotation
     */
    public ModelRotation withYaw(final float yaw) {
        return new ModelRotation(this.pitch, yaw);
    }
}
