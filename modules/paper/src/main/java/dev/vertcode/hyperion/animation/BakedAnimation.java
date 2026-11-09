package dev.vertcode.hyperion.animation;

import java.util.List;
import java.util.Map;

import dev.vertcode.hyperion.api.animation.LoopMode;

/**
 * An animation baked into a sample per tick for each bone it moves.
 *
 * <p>Built once when the model loads and shared by every model that plays it.
 *
 * @param name the animation's name
 * @param loop what it does at its end, as set in Blockbench
 * @param length how long one run lasts, in ticks
 * @param override whether it replaces the animations started before it on the bones it moves
 * @param startDelay ticks to wait before the first run
 * @param loopDelay ticks to wait between runs of a looping animation
 * @param tracks the tracks by bone name
 * @param effects the effect keyframes, by tick
 */
public record BakedAnimation(
        String name,
        LoopMode loop,
        float length,
        boolean override,
        int startDelay,
        int loopDelay,
        Map<String, BoneTrack> tracks,
        List<TimedEffect> effects
) {

    /**
     * Copy the tracks and effects, so a baked animation never changes.
     */
    public BakedAnimation {
        tracks = Map.copyOf(tracks);
        effects = List.copyOf(effects);
    }

    /**
     * Return the length in seconds.
     *
     * @return the length
     */
    public float seconds() {
        return this.length / AnimationBaker.TICKS_PER_SECOND;
    }
}
