package dev.vertcode.hyperion.api.animation;

import java.util.Collection;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Plays a model's Blockbench animations.
 *
 * <p>Only the basics for now. Blending, speed, loop modes, per-player playback and timeline
 * effects come with the animation plan. Every method is safe from any thread and takes effect
 * on the next frame.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface Animator {

    /**
     * Play an animation from its first frame, with the loop mode set in Blockbench.
     *
     * @param name the animation's name
     * @return a handle to the playing animation
     * @throws IllegalArgumentException if the model has no animation by that name
     */
    AnimationHandle play(String name);

    /**
     * Stop an animation. Does nothing when it is not playing.
     *
     * @param name the animation's name
     */
    void stop(String name);

    /**
     * Stop every animation.
     */
    void stopAll();

    /**
     * Return the animations playing now.
     *
     * @return a snapshot of the playing animations
     */
    @Unmodifiable Collection<AnimationHandle> playing();
}
