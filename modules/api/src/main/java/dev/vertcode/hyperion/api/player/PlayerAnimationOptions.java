package dev.vertcode.hyperion.api.player;

import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.animation.LoopMode;

/**
 * How an animation plays on a player. Every setter returns these options, so calls chain.
 */
@ApiStatus.NonExtendable
public interface PlayerAnimationOptions {

    /**
     * Choose what happens when the animation reaches its end. By default the animation's own
     * loop mode from Blockbench is used.
     *
     * <p>{@link LoopMode#ONCE} shows the player again when the animation is over.
     * {@link LoopMode#LOOP} and {@link LoopMode#HOLD} keep it going until it is stopped, for a
     * pose a player keeps for as long as your plugin wants.
     *
     * @param loop the loop mode
     * @return these options
     */
    PlayerAnimationOptions loop(LoopMode loop);

    /**
     * Hold the player in place while the animation plays. Off by default: the player walks
     * around and the model walks with them.
     *
     * @param locked true to hold them in place
     * @return these options
     */
    PlayerAnimationOptions locked(boolean locked);

    /**
     * Play the model's idle and walk animations under this one, so the legs move while the
     * player walks. On by default; a {@link #locked} player never walks.
     *
     * <p>Animations add up, so one that moves only the arms keeps them up while the legs walk.
     *
     * @param enabled true to play them
     * @return these options
     */
    PlayerAnimationOptions walkAnimations(boolean enabled);

    /**
     * End the animation when the player starts sneaking. {@code config.yml} sets the default,
     * which is off.
     *
     * @param stop true to end it on a sneak
     * @return these options
     */
    PlayerAnimationOptions stopOnSneak(boolean stop);

    /**
     * End the animation when the player takes damage. {@code config.yml} sets the default,
     * which is off.
     *
     * @param stop true to end it on damage
     * @return these options
     */
    PlayerAnimationOptions stopOnDamage(boolean stop);

    /**
     * Play the animation faster or slower.
     *
     * @param speed the speed, 1 for normal; must be above 0
     * @return these options
     * @throws IllegalArgumentException if the speed is not above 0
     */
    PlayerAnimationOptions speed(float speed);
}
