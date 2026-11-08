package dev.vertcode.hyperion.render;

import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * The transformation of one display entity: a translation, a turn and a stretch, in the order
 * the client applies them.
 *
 * @param translation the offset from the entity's position, in blocks
 * @param rotation the turn, the display's left rotation
 * @param scale the stretch along each axis, after the turn
 */
public record DisplayTransform(Vector3fc translation, Quaternionfc rotation, Vector3fc scale) {

    /**
     * Copy the parts, so a transform never changes after it is made.
     */
    public DisplayTransform {
        translation = new Vector3f(translation);
        rotation = new Quaternionf(rotation);
        scale = new Vector3f(scale);
    }

    /**
     * Split a matrix built from translations, turns and stretches into a display transform.
     *
     * <p>A stretch on one axis followed by a turn can skew a shape, which a display cannot show;
     * the result then keeps the turn and the length of each axis, the closest it can get. Models
     * only skew when a plugin stretches a bone unevenly and turns a bone below it.
     *
     * @param matrix the matrix
     * @return the transform
     */
    public static DisplayTransform of(final Matrix4fc matrix) {
        Vector3f translation = matrix.getTranslation(new Vector3f());
        Vector3f scale = matrix.getScale(new Vector3f());

        // A mirrored matrix has no rotation of its own: mirror it back through every axis first.
        if (matrix.determinant3x3() < 0f) {
            scale.negate();
        }

        Matrix3f basis = matrix.get3x3(new Matrix3f()).scale(1f / scale.x, 1f / scale.y, 1f / scale.z);
        Quaternionf rotation = new Quaternionf().setFromNormalized(basis).normalize();

        return new DisplayTransform(translation, rotation, scale);
    }

    /**
     * Return whether another transform is close enough to look the same, so it need not be sent.
     *
     * @param other the other transform
     * @param epsilon the largest difference that does not count
     * @return true when every part is within the epsilon
     */
    public boolean closeTo(final DisplayTransform other, final float epsilon) {
        return this.translation.equals(other.translation, epsilon)
                && this.scale.equals(other.scale, epsilon)
                && sameTurn(this.rotation, other.rotation, epsilon);
    }

    /**
     * Return whether two rotations turn the same way, either sign of the quaternion.
     *
     * @param first the first rotation
     * @param second the second rotation
     * @param epsilon the largest difference that does not count
     * @return true when they turn the same way
     */
    private static boolean sameTurn(final Quaternionfc first, final Quaternionfc second, final float epsilon) {
        float dot = first.x() * second.x() + first.y() * second.y() + first.z() * second.z() + first.w() * second.w();

        return Math.abs(Math.abs(dot) - 1f) <= epsilon;
    }
}
