package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.armor.ArmorPiece;

/**
 * Fired after a piece of Hyperion armor left a player's armor slot.
 *
 * <p>It fires however it left: taken off, broken, dropped on death, replaced, or removed by a
 * command or plugin, and when the player leaves wearing it, with {@link ArmorChangeCause#QUIT}.
 * It cannot be cancelled.
 */
public final class ArmorUnequipEvent extends ArmorEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ArmorPiece piece;
    private final ItemStack item;
    private final ArmorChangeCause cause;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param player the player
     * @param piece the piece
     * @param item the item that was in the slot
     * @param cause why it changed
     */
    @ApiStatus.Internal
    public ArmorUnequipEvent(
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
     * Return the item that was in the slot.
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
