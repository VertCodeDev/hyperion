package dev.vertcode.hyperion.animation;

import java.util.List;

import dev.vertcode.hyperion.animation.molang.MolangContext;
import dev.vertcode.hyperion.blueprint.data.Bezier;
import dev.vertcode.hyperion.blueprint.data.Interpolation;

/**
 * The keyframes of one channel of one bone, and Blockbench's math for the value between them.
 *
 * <p>Before the first keyframe the channel holds the first one's value, after the last the last
 * one's. Between two keyframes the value follows the earlier one when it is a step; a smooth
 * (catmull-rom) curve when either is smooth, through the keyframes around them; a bezier curve
 * when either is a bezier; and a straight line otherwise. A keyframe with two values arrives at
 * its first and leaves from its second.
 */
final class KeyframeCurve {

    private static final int BEZIER_STEPS = 24;

    private final List<CurveKey> keys;

    /**
     * Create the curve.
     *
     * @param keys the keyframes, sorted by time; at least one
     */
    KeyframeCurve(final List<CurveKey> keys) {
        this.keys = List.copyOf(keys);
    }

    /**
     * Work out the value at a point in time.
     *
     * @param time the time in seconds
     * @param context the context the keyframes' Molang reads
     * @param into where the three values go
     * @param offset where in {@code into} they go
     */
    void value(final float time, final MolangContext context, final float[] into, final int offset) {
        CurveKey first = this.keys.getFirst();
        CurveKey last = this.keys.getLast();
        if (this.keys.size() == 1 || time <= first.time()) {
            fill(first, true, context, into, offset);
            return;
        }

        if (time >= last.time()) {
            fill(last, false, context, into, offset);
            return;
        }

        int index = 0;
        while (this.keys.get(index + 1).time() <= time) {
            index++;
        }

        CurveKey before = this.keys.get(index);
        CurveKey after = this.keys.get(index + 1);
        float alpha = (time - before.time()) / (after.time() - before.time());
        for (int axis = 0; axis < 3; axis++) {
            into[offset + axis] = this.between(index, before, after, axis, alpha, time, context);
        }
    }

    /**
     * Work out one axis between two keyframes.
     *
     * @param index the earlier keyframe's place
     * @param before the earlier keyframe
     * @param after the later keyframe
     * @param axis the axis
     * @param alpha how far from the earlier to the later, from 0 to 1
     * @param time the time in seconds, for bezier curves
     * @param context the context the Molang reads
     * @return the value
     */
    private float between(
            final int index,
            final CurveKey before,
            final CurveKey after,
            final int axis,
            final float alpha,
            final float time,
            final MolangContext context
    ) {
        float from = before.leaving(axis, context);
        if (before.interpolation() == Interpolation.STEP) {
            return from;
        }

        float to = after.arriving(axis, context);
        if (before.interpolation() == Interpolation.CATMULLROM || after.interpolation() == Interpolation.CATMULLROM) {
            float previous = index > 0 ? this.keys.get(index - 1).leaving(axis, context) : from;
            float next = index + 2 < this.keys.size() ? this.keys.get(index + 2).arriving(axis, context) : to;

            return catmullRom(alpha, previous, from, to, next);
        }

        if (before.interpolation() == Interpolation.BEZIER || after.interpolation() == Interpolation.BEZIER) {
            return bezier(before, after, axis, from, to, time);
        }

        return from + (to - from) * alpha;
    }

    /**
     * Fill all three axes with one keyframe's value.
     *
     * @param key the keyframe
     * @param arriving true for the value it arrives at, false for the one it leaves from
     * @param context the context the Molang reads
     * @param into where the values go
     * @param offset where in {@code into} they go
     */
    private static void fill(
            final CurveKey key,
            final boolean arriving,
            final MolangContext context,
            final float[] into,
            final int offset
    ) {
        for (int axis = 0; axis < 3; axis++) {
            into[offset + axis] = arriving ? key.arriving(axis, context) : key.leaving(axis, context);
        }
    }

    /**
     * Blockbench's smooth curve: a uniform catmull-rom spline through four values.
     *
     * @param t how far from the second value to the third, from 0 to 1
     * @param p0 the value before the segment
     * @param p1 the segment's start
     * @param p2 the segment's end
     * @param p3 the value after the segment
     * @return the value
     */
    static float catmullRom(final float t, final float p0, final float p1, final float p2, final float p3) {
        float v0 = (p2 - p0) * 0.5f;
        float v1 = (p3 - p1) * 0.5f;
        float t2 = t * t;
        float t3 = t * t2;

        return (2f * p1 - 2f * p2 + v0 + v1) * t3 + (-3f * p1 + 3f * p2 - 2f * v0 - v1) * t2 + v0 * t + p1;
    }

    /**
     * Blockbench's bezier curve between two keyframes, through their handles, read at a time.
     *
     * @param before the earlier keyframe, whose right handle leaves it
     * @param after the later keyframe, whose left handle arrives at it
     * @param axis the axis
     * @param from the earlier keyframe's value
     * @param to the later keyframe's value
     * @param time the time to read at, in seconds
     * @return the value
     */
    private static float bezier(
            final CurveKey before,
            final CurveKey after,
            final int axis,
            final float from,
            final float to,
            final float time
    ) {
        Bezier leaving = before.bezier();
        Bezier arriving = after.bezier();
        float x0 = before.time();
        float x1 = x0 + (leaving == null ? 0f : leaving.rightTime().get(axis));
        float y1 = from + (leaving == null ? 0f : leaving.rightValue().get(axis));
        float x3 = after.time();
        float x2 = x3 + (arriving == null ? 0f : arriving.leftTime().get(axis));
        float y2 = to + (arriving == null ? 0f : arriving.leftValue().get(axis));

        // The curve's time is not its parameter: find the parameter at this time by halving.
        float low = 0f;
        float high = 1f;
        for (int i = 0; i < BEZIER_STEPS; i++) {
            float middle = (low + high) * 0.5f;
            if (cubic(middle, x0, x1, x2, x3) < time) {
                low = middle;
            } else {
                high = middle;
            }
        }

        return cubic((low + high) * 0.5f, from, y1, y2, to);
    }

    /**
     * A cubic bezier through four points, on one axis.
     *
     * @param t the parameter, from 0 to 1
     * @param p0 the start
     * @param p1 the first handle
     * @param p2 the second handle
     * @param p3 the end
     * @return the value
     */
    private static float cubic(final float t, final float p0, final float p1, final float p2, final float p3) {
        float u = 1f - t;

        return u * u * u * p0 + 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t * p3;
    }
}
