package dev.vertcode.hyperion.api.animation;

import org.jetbrains.annotations.ApiStatus;

/**
 * How to play one animation. Every setter returns this object, so calls chain:
 *
 * <pre>{@code
 * model.animator().play("attack", options -> options.speed(1.5f).loop(LoopMode.ONCE).fadeIn(3));
 * }</pre>
 */
@ApiStatus.NonExtendable
public interface PlayOptions {

    /**
     * Play faster or slower than made.
     *
     * @param speed how fast, 1 as made, 2 twice as fast; above 0
     * @return these options
     * @throws IllegalArgumentException if the speed is not above 0
     */
    PlayOptions speed(float speed);

    /**
     * Use another loop mode than the one set in Blockbench.
     *
     * @param loop the loop mode
     * @return these options
     */
    PlayOptions loop(LoopMode loop);

    /**
     * Blend the animation in over some ticks instead of starting at full strength.
     *
     * @param ticks the ticks, 0 or more; 0 by default
     * @return these options
     * @throws IllegalArgumentException if the ticks are below 0
     */
    PlayOptions fadeIn(int ticks);

    /**
     * Blend the animation out over some ticks when it is stopped or ends, instead of snapping
     * back.
     *
     * @param ticks the ticks, 0 or more; 0 by default
     * @return these options
     * @throws IllegalArgumentException if the ticks are below 0
     */
    PlayOptions fadeOut(int ticks);
}
