package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.armor.ArmorPiece;

/**
 * Fired after a player put on a piece of Hyperion armor.
 *
 * <p>It fires however the piece got there: a click, a dispenser, a command or another plugin, and
 * when the player joins wearing it, with {@link ArmorChangeCause#JOIN}. It cannot be cancelled;
 * to stop a player putting a piece on, listen to {@link ArmorEquipAttemptEvent}.
 */
public final class ArmorEquipEvent extends ArmorEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ArmorPiece piece;
    private final ItemStack item;
    private final ArmorChangeCause cause;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param player the player
     * @param piece the piece
     * @param item the item now in the slot
     * @param cause why it changed
     */
    @ApiStatus.Internal
    public ArmorEquipEvent(
            final Player player,
            final ArmorPiece piece,
            final ItemStack item,
            final ArmorChangeCause cause
    ) {
        super(player, piece.set());
        this.piece = piece;
        this.item = item.clone();
        this.cause = cause;
    }

    /**
     * Return the piece.
     *
     * @return the piece
     */
    public ArmorPiece piece() {
        return this.piece;
    }

    /**
     * Return the item now in the slot.
     *
     * @return a copy of the item
     */
    public ItemStack item() {
        return this.item.clone();
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
