package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired when an animation on a player ends: it is over, it was stopped, or the player left, died
 * or changed worlds. The player is shown again right after.
 */
public final class PlayerAnimationEndEvent extends PlayerAnimationPhaseEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final boolean finished;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param player the player
     * @param animation the animation's name
     * @param finished whether the animation played to its end
     */
    @ApiStatus.Internal
    public PlayerAnimationEndEvent(final Player player, final String animation, final boolean finished) {
        super(player, animation);
        this.finished = finished;
    }

    /**
     * Return whether the animation played to its end.
     *
     * @return false when it was stopped before that
     */
    public boolean finished() {
        return this.finished;
    }

    /**
     * Return the handlers listening to this event. Bukkit finds this method by name.
     *
     * @return the handler list
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
