package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.armor.ArmorSet;

/**
 * Fired when a player now wears every piece of a Hyperion armor set: switch a set bonus on here.
 *
 * <p>It also fires when the player joins in the full set. It comes after the
 * {@link ArmorEquipEvent} of the last piece.
 */
public final class ArmorSetCompleteEvent extends ArmorEvent {

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
    public ArmorSetCompleteEvent(final Player player, final ArmorSet set, final ArmorChangeCause cause) {
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
