package dev.vertcode.hyperion.animation;

import org.joml.Vector3f;

/**
 * What the playing animations do to one bone this frame: an offset, Blockbench angles added to
 * the bone's own, and a stretch. Reused every frame by the thread that owns the model.
 */
public final class BonePose {

    private final Vector3f position = new Vector3f();
    private final Vector3f rotation = new Vector3f();
    private final Vector3f scale = new Vector3f(1f);

    /**
     * Go back to doing nothing to the bone.
     */
    public void reset() {
        this.position.zero();
        this.rotation.zero();
        this.scale.set(1f);
    }

    /**
     * Copy another pose.
     *
     * @param other the pose to copy
     */
    public void set(final BonePose other) {
        this.position.set(other.position);
        this.rotation.set(other.rotation);
        this.scale.set(other.scale);
    }

    /**
     * Return whether this pose does nothing to the bone.
     *
     * @return true at rest
     */
    public boolean atRest() {
        return this.position.lengthSquared() == 0f && this.rotation.lengthSquared() == 0f
                && this.scale.x == 1f && this.scale.y == 1f && this.scale.z == 1f;
    }

    /**
     * Return the offset, in blocks, in the parent bone's space.
     *
     * @return the offset, which the caller must not change
     */
    public Vector3f position() {
        return this.position;
    }

    /**
     * Return the Blockbench angles added to the bone's own, in degrees.
     *
     * @return the angles, which the caller must not change
     */
    public Vector3f rotation() {
        return this.rotation;
    }

    /**
     * Return the stretch along each axis.
     *
     * @return the stretch, which the caller must not change
     */
    public Vector3f scale() {
        return this.scale;
    }

    /**
     * Add an animation's offset, angles and stretch at some strength, as Blockbench layers
     * animations: offsets and angles add up, stretches multiply.
     *
     * @param values the animation's values at this frame: offset, angles and stretch, nine floats
     * @param weight how strongly it applies, from 0 to 1
     * @param hasPosition whether the animation moves the bone
     * @param hasRotation whether it turns it
     * @param hasScale whether it stretches it
     */
    void add(
            final float[] values,
            final float weight,
            final boolean hasPosition,
            final boolean hasRotation,
            final boolean hasScale
    ) {
        if (hasPosition) {
            this.position.add(values[0] * weight, values[1] * weight, values[2] * weight);
        }

        if (hasRotation) {
            this.rotation.add(values[3] * weight, values[4] * weight, values[5] * weight);
        }

        if (hasScale) {
            this.scale.mul(
                    1f + (values[6] - 1f) * weight,
                    1f + (values[7] - 1f) * weight,
                    1f + (values[8] - 1f) * weight
            );
        }
    }
}
