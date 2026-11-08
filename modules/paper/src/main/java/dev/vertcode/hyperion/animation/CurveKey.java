package dev.vertcode.hyperion.animation;

import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.animation.molang.MolangContext;
import dev.vertcode.hyperion.animation.molang.MolangExpression;
import dev.vertcode.hyperion.blueprint.data.Bezier;
import dev.vertcode.hyperion.blueprint.data.Interpolation;

/**
 * One keyframe of a curve, with its values compiled.
 *
 * @param time when it is, in seconds
 * @param interpolation how the curve leaves it
 * @param arriving the value per axis the curve arrives at
 * @param leaving the value per axis the curve leaves from; the same as arriving for a keyframe
 *     with one value
 * @param bezier the handles, or null when it is not a bezier keyframe
 */
record CurveKey(
        float time,
        Interpolation interpolation,
        MolangExpression[] arriving,
        MolangExpression[] leaving,
        @Nullable Bezier bezier
) {

    /**
     * Work out the value the curve arrives at on one axis.
     *
     * @param axis the axis
     * @param context the context the Molang reads
     * @return the value
     */
    float arriving(final int axis, final MolangContext context) {
        return this.arriving[axis].evaluate(context);
    }

    /**
     * Work out the value the curve leaves from on one axis.
     *
     * @param axis the axis
     * @param context the context the Molang reads
     * @return the value
     */
    float leaving(final int axis, final MolangContext context) {
        return this.leaving[axis].evaluate(context);
    }
}
