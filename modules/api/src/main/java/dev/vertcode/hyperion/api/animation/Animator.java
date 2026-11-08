package dev.vertcode.hyperion.api.animation;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Plays a model's Blockbench animations.
 *
 * <p>Several animations play at once and add up, as in Blockbench; an animation marked to
 * override in Blockbench replaces the ones started before it on the bones it moves. Playing an
 * animation that already plays starts it again. Every method is safe from any thread and takes
 * effect on the next frame.
 */
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
     * Play an animation from its first frame, with options.
     *
     * @param name the animation's name
     * @param options sets the speed, loop mode and fades
     * @return a handle to the playing animation
     * @throws IllegalArgumentException if the model has no animation by that name, or an
     *     option is out of range
     */
    AnimationHandle play(String name, Consumer<PlayOptions> options);

    /**
     * Stop an animation, fading it out when it was played with a fade out. Does nothing when it
     * is not playing.
     *
     * @param name the animation's name
     */
    void stop(String name);

    /**
     * Stop every animation.
     */
    void stopAll();

    /**
     * Find a playing animation by name.
     *
     * @param name the animation's name
     * @return its handle, or empty when it does not play
     */
    Optional<AnimationHandle> playing(String name);

    /**
     * Return the animations playing now.
     *
     * @return a snapshot of the playing animations
     */
    @Unmodifiable Collection<AnimationHandle> playing();
}
