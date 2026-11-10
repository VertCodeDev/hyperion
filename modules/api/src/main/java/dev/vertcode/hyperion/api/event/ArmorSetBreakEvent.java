package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.armor.ArmorSet;

/**
 * Fired when a player wore every piece of a Hyperion armor set and now does not: switch a set
 * bonus off here.
 *
 * <p>It also fires when the player leaves in the full set. It comes after the
 * {@link ArmorUnequipEvent} of the piece that broke the set.
 */
public final class ArmorSetBreakEvent extends ArmorEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ArmorChangeCause cause;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param player the player
     * @param set the set
     * @param cause why the armor changed
     */
    @ApiStatus.Internal
    public ArmorSetBreakEvent(final Player player, final ArmorSet set, final ArmorChangeCause cause) {
        super(player, set);
        this.cause = cause;
    }

    /**
     * Return why the armor changed.
     *
     * @return the cause
     */
    public ArmorChangeCause cause() {
        return this.cause;
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
