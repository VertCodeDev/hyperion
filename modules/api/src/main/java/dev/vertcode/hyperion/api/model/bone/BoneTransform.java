package dev.vertcode.hyperion.api.model.bone;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A move, turn and stretch, applied in that order: scale, then rotate, then translate.
 *
 * <p>Immutable: the vectors are copied in, and the {@code with} methods return a new transform.
 *
 * @param translation the offset in blocks
 * @param rotation the rotation
 * @param scale the scale on each axis
 */
public record BoneTransform(Vector3fc translation, Quaternionfc rotation, Vector3fc scale) {

    /**
     * The transform that changes nothing.
     */
    public static final BoneTransform IDENTITY = new BoneTransform(new Vector3f(), new Quaternionf(), new Vector3f(1f));

    /**
     * Create a transform from copies of its parts.
     *
     * @param translation the offset in blocks
     * @param rotation the rotation
     * @param scale the scale on each axis
     */
    public BoneTransform {
        translation = new Vector3f(translation);
        rotation = new Quaternionf(rotation);
        scale = new Vector3f(scale);
    }

    /**
     * Create a transform that only moves.
     *
     * @param x the offset on x in blocks
     * @param y the offset on y in blocks
     * @param z the offset on z in blocks
     * @return the transform
     */
    public static BoneTransform ofTranslation(final float x, final float y, final float z) {
        return IDENTITY.withTranslation(new Vector3f(x, y, z));
    }

    /**
     * Create a transform that only turns.
     *
     * @param rotation the rotation
     * @return the transform
     */
    public static BoneTransform ofRotation(final Quaternionfc rotation) {
        return IDENTITY.withRotation(rotation);
    }

    /**
     * Create a transform that only scales, the same on every axis.
     *
     * @param scale the scale
     * @return the transform
     */
    public static BoneTransform ofScale(final float scale) {
        return IDENTITY.withScale(scale);
    }

    /**
     * Return a copy with another translation.
     *
     * @param translation the offset in blocks
     * @return the new transform
     */
    public BoneTransform withTranslation(final Vector3fc translation) {
        return new BoneTransform(translation, this.rotation, this.scale);
    }

    /**
     * Return a copy with another rotation.
     *
     * @param rotation the rotation
     * @return the new transform
     */
    public BoneTransform withRotation(final Quaternionfc rotation) {
        return new BoneTransform(this.translation, rotation, this.scale);
    }

    /**
     * Return a copy with another scale per axis.
     *
     * @param scale the scale on each axis
     * @return the new transform
     */
    public BoneTransform withScale(final Vector3fc scale) {
        return new BoneTransform(this.translation, this.rotation, scale);
    }

    /**
     * Return a copy with the same scale on every axis.
     *
     * @param scale the scale
     * @return the new transform
     */
    public BoneTransform withScale(final float scale) {
        return this.withScale(new Vector3f(scale));
    }
}
