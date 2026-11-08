package dev.vertcode.hyperion.animation;

import org.jspecify.annotations.Nullable;

/**
 * One bone's part of a baked animation: a sample per tick of each channel the animation keys.
 *
 * <p>Offsets are in blocks and angles in degrees, both already turned into the bone's own space,
 * so playing only reads and blends numbers. Never changed once baked, so any number of models
 * share it.
 *
 * @param position three floats per tick, or null when the animation does not move the bone
 * @param rotation three floats per tick, or null when it does not turn it
 * @param scale three floats per tick, or null when it does not stretch it
 */
public record BoneTrack(float @Nullable [] position, float @Nullable [] rotation, float @Nullable [] scale) {

    /**
     * Add this track at a point in time to a pose.
     *
     * @param frame the tick into the animation; between two ticks the samples are blended
     * @param pose the pose to add to
     * @param weight how strongly it applies, from 0 to 1
     * @param scratch nine floats to work in
     */
    public void apply(final float frame, final BonePose pose, final float weight, final float[] scratch) {
        sample(this.position, frame, scratch, 0, 0f);
        sample(this.rotation, frame, scratch, 3, 0f);
        sample(this.scale, frame, scratch, 6, 1f);
        pose.add(scratch, weight, this.position != null, this.rotation != null, this.scale != null);
    }

    /**
     * Read one channel at a point in time, blending the two ticks around it.
     *
     * @param samples the channel's samples, or null when it is not keyed
     * @param frame the tick into the animation
     * @param into where the three values go
     * @param offset where in {@code into} they go
     * @param rest the value of a channel that is not keyed
     */
    private static void sample(
            final float @Nullable [] samples,
            final float frame,
            final float[] into,
            final int offset,
            final float rest
    ) {
        if (samples == null) {
            into[offset] = rest;
            into[offset + 1] = rest;
            into[offset + 2] = rest;
            return;
        }

        int last = samples.length / 3 - 1;
        float clamped = Math.clamp(frame, 0f, last);
        int first = (int) clamped;
        int second = Math.min(first + 1, last);
        float blend = clamped - first;
        for (int axis = 0; axis < 3; axis++) {
            float from = samples[first * 3 + axis];
            float to = samples[second * 3 + axis];
            into[offset + axis] = from + (to - from) * blend;
        }
    }
}
