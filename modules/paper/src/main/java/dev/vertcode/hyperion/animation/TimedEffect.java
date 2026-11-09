package dev.vertcode.hyperion.animation;

import org.jspecify.annotations.Nullable;

/**
 * An effect keyframe of an animation, from Blockbench's effects timeline.
 *
 * @param tick the tick it falls on
 * @param kind what it is
 * @param effect the sound's key, the effect's key or particle name, or the timeline script
 * @param locator the locator a particle keyframe is at, or null for the model's origin
 */
public record TimedEffect(int tick, Kind kind, String effect, @Nullable String locator) {

    /**
     * What an effect keyframe does.
     */
    public enum Kind {

        /**
         * Plays a sound at the model.
         */
        SOUND,

        /**
         * Plays an effect, or a burst of a particle, at a locator.
         */
        PARTICLE,

        /**
         * Fires {@code AnimationKeyframeEvent} with its script.
         */
        EVENT
    }
}
