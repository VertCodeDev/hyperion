package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.armor.ArmorPiece;

/**
 * Fired when a player tries to put on a piece of Hyperion armor. Cancel it to keep the piece
 * off, for a level or class requirement:
 *
 * <pre>{@code
 * @EventHandler
 * public void onAttempt(ArmorEquipAttemptEvent event) {
 *     if (event.getPlayer().getLevel() < 30) {
 *         event.setCancelled(true);
 *     }
 * }
 * }</pre>
 *
 * <p>It covers what a player does: right-clicking, placing, shift-clicking, dragging, hotbar and
 * offhand swaps, and dispensers. A command or plugin that sets armor directly does not ask; the
 * {@link ArmorEquipEvent} after it still fires.
 */
public final class ArmorEquipAttemptEvent extends ArmorEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ArmorPiece piece;
    private final ItemStack item;
    private final ArmorEquipMethod method;
    private boolean cancelled;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param player the player
     * @param piece the piece they try to put on
     * @param item the item
     * @param method how they try it
     */
    @ApiStatus.Internal
    public ArmorEquipAttemptEvent(
            final Player player,
            final ArmorPiece piece,
            final ItemStack item,
            final ArmorEquipMethod method
    ) {
        super(player, piece.set());
        this.piece = piece;
        this.item = item.clone();
        this.method = method;
    }

    /**
     * Return the piece the player tries to put on.
     *
     * @return the piece
     */
    public ArmorPiece piece() {
        return this.piece;
    }

    /**
     * Return the item the player tries to put on.
     *
     * @return a copy of the item
     */
    public ItemStack item() {
        return this.item.clone();
    }

    /**
     * Return how the player tries it.
     *
     * @return the method
     */
    public ArmorEquipMethod method() {
        return this.method;
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
