package dev.vertcode.hyperion.api.event;

import java.util.Optional;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.bone.Bone;

/**
 * Fired when a player hits or clicks a model's hitbox.
 *
 * <p>Fires on the region that owns the model. Cancel it to stop the hit or click from reaching
 * the entity carrying the model, and Hyperion from acting on it, such as seating the player.
 */
public final class ModelInteractEvent extends ModelEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final @Nullable Bone bone;
    private final InteractionType type;
    private final EquipmentSlot hand;
    private boolean cancelled;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param model the model that was touched
     * @param player the player who touched it
     * @param bone the hitbox bone that was touched, or null for the box around the whole model
     * @param type whether it was a hit or a click
     * @param hand the hand the player used
     */
    @ApiStatus.Internal
    public ModelInteractEvent(
            final Model model,
            final Player player,
            final @Nullable Bone bone,
            final InteractionType type,
            final EquipmentSlot hand
    ) {
        super(model);
        this.player = player;
        this.bone = bone;
        this.type = type;
        this.hand = hand;
    }

    /**
     * Return the player who hit or clicked.
     *
     * @return the player
     */
    public Player player() {
        return this.player;
    }

    /**
     * Return the hitbox bone that was hit or clicked.
     *
     * @return the bone, or empty when the player touched the box around the whole model
     */
    public Optional<Bone> bone() {
        return Optional.ofNullable(this.bone);
    }

    /**
     * Return whether this was a hit or a click.
     *
     * @return the interaction type
     */
    public InteractionType type() {
        return this.type;
    }

    /**
     * Return the hand the player used.
     *
     * @return the hand
     */
    public EquipmentSlot hand() {
        return this.hand;
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
