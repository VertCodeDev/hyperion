package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.ModelBlueprint;

/**
 * Fired before an animation starts playing on a player. Cancel it to keep it from playing, in a
 * fight or an arena for example:
 *
 * <pre>{@code
 * @EventHandler
 * public void onStart(PlayerAnimationStartEvent event) {
 *     if (inCombat(event.getPlayer())) {
 *         event.setCancelled(true);
 *     }
 * }
 * }</pre>
 */
public final class PlayerAnimationStartEvent extends PlayerAnimationPhaseEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ModelBlueprint blueprint;
    private boolean cancelled;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param player the player
     * @param blueprint the player model shown in their place
     * @param animation the animation's name
     */
    @ApiStatus.Internal
    public PlayerAnimationStartEvent(final Player player, final ModelBlueprint blueprint, final String animation) {
        super(player, animation);
        this.blueprint = blueprint;
    }

    /**
     * Return the player model that will be shown in the player's place.
     *
     * @return the blueprint
     */
    public ModelBlueprint blueprint() {
        return this.blueprint;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(final boolean cancel) {
        this.cancelled = cancel;
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
