package dev.vertcode.hyperion.blueprint.data;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * One keyframe of an animation, as Blockbench saved it.
 *
 * <p>Values keep Blockbench's own sign conventions; animation playback converts them.
 *
 * @param channel what the keyframe changes
 * @param time when the keyframe sits, in seconds from the start
 * @param interpolation how the animation moves towards the next keyframe
 * @param points the data points: one, or two when the value jumps at this keyframe
 * @param bezier the handles, or null when the interpolation is not bezier
 */
public record Keyframe(
        KeyframeChannel channel,
        float time,
        Interpolation interpolation,
        List<KeyframePoint> points,
        @Nullable Bezier bezier
) {

    /**
     * Copy the data points, so a keyframe never changes after it is read.
     */
    public Keyframe {
        points = List.copyOf(points);
    }
}
