package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

/**
 * An event about an animation playing on a player, fired on the player's own thread.
 */
public abstract class PlayerAnimationPhaseEvent extends PlayerEvent {

    private final String animation;

    /**
     * Create an event about an animation on a player.
     *
     * @param player the player
     * @param animation the animation's name
     */
    protected PlayerAnimationPhaseEvent(final Player player, final String animation) {
        super(player);
        this.animation = animation;
    }

    /**
     * Return the name of the animation.
     *
     * @return the animation's name
     */
    public String animation() {
        return this.animation;
    }
}
