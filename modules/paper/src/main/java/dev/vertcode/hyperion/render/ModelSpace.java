package dev.vertcode.hyperion.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.animation.BonePose;
import dev.vertcode.hyperion.api.model.ModelRotation;
import dev.vertcode.hyperion.api.model.bone.BoneTransform;
import dev.vertcode.hyperion.pack.ModelPart;

/**
 * The matrices that place a model's bones and parts, in blocks.
 *
 * <p>Bones live in Blockbench's space, where a model faces north. The root turns that space to
 * the way the model faces in the world, which also turns a north-facing model to face where its
 * entity looks. An item display draws its item turned half a circle around Y, so every part's
 * matrix ends with the same half turn, which undoes it.
 *
 * <p>Every method writes into a matrix it is given, so a frame allocates nothing.
 */
public final class ModelSpace {

    private static final Quaternionfc HALF_TURN = new Quaternionf().rotationY((float) Math.PI);

    private ModelSpace() {
    }

    /**
     * Build the root: where the model faces, its scale and its root transform, then the half
     * turn from Blockbench's north to the world's south.
     *
     * @param dest where to write the matrix
     * @param rotation which way the model faces; yaw and pitch in degrees, as Minecraft has them
     * @param scale the model's scale
     * @param transform the model's root transform
     * @return dest
     */
    public static Matrix4f root(
            final Matrix4f dest,
            final ModelRotation rotation,
            final float scale,
            final BoneTransform transform
    ) {
        return dest.identity()
                .rotateY((float) Math.toRadians(-rotation.yaw()))
                .rotateX((float) Math.toRadians(rotation.pitch()))
                .scale(scale)
                .translate(transform.translation())
                .rotate(transform.rotation())
                .scale(transform.scale())
                .rotate(HALF_TURN);
    }

    /**
     * Build a bone's matrix: its parent's, then around its pivot the animation's offset, its
     * rest angles plus the animation's, the animation's stretch, then its extra transform.
     *
     * <p>The angles add up before they become a rotation, as in Blockbench, so an animation
     * turns a bone exactly as the editor shows it.
     *
     * @param dest where to write the matrix
     * @param parent the parent bone's matrix, or the identity for a root bone
     * @param pivot the bone's pivot in model space, in blocks
     * @param rest the bone's rest rotation as Blockbench angles, in degrees
     * @param pose what the animations do to the bone this frame
     * @param transform the bone's extra transform, applied on top of the animation
     * @param turn a quaternion to work in
     * @return dest
     */
    public static Matrix4f bone(
            final Matrix4f dest,
            final Matrix4fc parent,
            final Vector3fc pivot,
            final Vector3fc rest,
            final BonePose pose,
            final BoneTransform transform,
            final Quaternionf turn
    ) {
        Vector3fc offset = transform.translation();
        Vector3fc moved = pose.position();
        Vector3fc angles = pose.rotation();
        turn.rotationZYX(
                (float) Math.toRadians(rest.z() + angles.z()),
                (float) Math.toRadians(rest.y() + angles.y()),
                (float) Math.toRadians(rest.x() + angles.x())
        );

        return dest.set(parent)
                .translate(
                        pivot.x() + moved.x() + offset.x(),
                        pivot.y() + moved.y() + offset.y(),
                        pivot.z() + moved.z() + offset.z()
                )
                .rotate(turn)
                .scale(pose.scale())
                .rotate(transform.rotation())
                .scale(transform.scale())
                .translate(-pivot.x(), -pivot.y(), -pivot.z());
    }

    /**
     * Build the transformation of a display showing one part of a bone.
     *
     * @param dest where to write the matrix
     * @param root the model's root, see {@link #root}
     * @param bone the bone's matrix, see {@link #bone}
     * @param pivot the bone's pivot in model space, in blocks
     * @param part the part, placed from the bone's pivot
     * @return dest
     */
    public static Matrix4f part(
            final Matrix4f dest,
            final Matrix4fc root,
            final Matrix4fc bone,
            final Vector3fc pivot,
            final ModelPart part
    ) {
        return dest.set(root)
                .mul(bone)
                .translate(pivot)
                .translate(part.offset())
                .rotate(part.rotation())
                .scale(part.scale())
                .rotate(HALF_TURN);
    }

    /**
     * Build the transformation of a display showing an item at a bone's pivot.
     *
     * @param dest where to write the matrix
     * @param root the model's root, see {@link #root}
     * @param bone the bone's matrix, see {@link #bone}
     * @param pivot the bone's pivot in model space, in blocks
     * @return dest
     */
    public static Matrix4f item(final Matrix4f dest, final Matrix4fc root, final Matrix4fc bone, final Vector3fc pivot) {
        return dest.set(root).mul(bone).translate(pivot).rotate(HALF_TURN);
    }
}
