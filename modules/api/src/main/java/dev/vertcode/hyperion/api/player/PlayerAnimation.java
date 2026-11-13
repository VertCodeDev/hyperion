package dev.vertcode.hyperion.api.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.Model;

/**
 * An animation playing on a player: the model that stands in for them and the animation on it.
 *
 * <p>Safe to read from any thread.
 */
@ApiStatus.NonExtendable
public interface PlayerAnimation {

    /**
     * Return the player the animation plays on.
     *
     * @return the player
     */
    Player player();

    /**
     * Return the model that stands in for the player. It wears the player's skin and is removed
     * when the animation ends. Play more animations on it through its animator, tint it or give
     * its bones items, like any model.
     *
     * @return the model
     */
    Model model();

    /**
     * Return the name of the animation.
     *
     * @return the animation's name
     */
    String animation();

    /**
     * Return whether the player is held in place while the animation plays.
     *
     * @return true when they cannot walk
     */
    boolean locked();

    /**
     * Return whether the animation is still playing.
     *
     * @return false once it ended or was stopped
     */
    boolean isPlaying();

    /**
     * Stop the animation and show the player again. Safe from any thread; does nothing when it
     * already ended.
     */
    void stop();
}
