package dev.vertcode.hyperion.api.animation;

import org.jetbrains.annotations.ApiStatus;

/**
 * One playing animation.
 *
 * <p>Safe from any thread. Changes take effect on the next frame.
 */
@ApiStatus.NonExtendable
public interface AnimationHandle {

    /**
     * Return the animation's name.
     *
     * @return the name
     */
    String name();

    /**
     * Return whether the animation still plays, fading out included.
     *
     * @return false once it has stopped or finished
     */
    boolean isPlaying();

    /**
     * Return how far into the animation it is, as of the last frame.
     *
     * @return the time in seconds, from 0 to {@link #length()}
     */
    float time();

    /**
     * Return how long one run of the animation lasts at speed 1.
     *
     * @return the length in seconds
     */
    float length();

    /**
     * Return how fast it plays.
     *
     * @return the speed, 1 as made
     */
    float speed();

    /**
     * Play faster or slower from the next frame.
     *
     * @param speed the speed, above 0
     * @return this handle
     * @throws IllegalArgumentException if the speed is not above 0
     */
    AnimationHandle speed(float speed);

    /**
     * Return what it does at its end.
     *
     * @return the loop mode
     */
    LoopMode loop();

    /**
     * Run something once the animation has stopped or finished, on the thread that owns the
     * model. Runs on the next frame when it already ended.
     *
     * @param action what to run
     * @return this handle
     */
    AnimationHandle onEnd(Runnable action);

    /**
     * Stop the animation, fading it out when it was played with a fade out. Does nothing when
     * it already stopped.
     */
    void stop();
}
