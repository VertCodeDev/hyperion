package dev.vertcode.hyperion.api.animation;

import org.jetbrains.annotations.ApiStatus;

/**
 * One playing animation.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface AnimationHandle {

    /**
     * Return the animation's name.
     *
     * @return the name
     */
    String name();

    /**
     * Return whether the animation still plays.
     *
     * @return false once it has stopped or finished
     */
    boolean isPlaying();

    /**
     * Stop the animation. Does nothing when it already stopped.
     */
    void stop();
}
