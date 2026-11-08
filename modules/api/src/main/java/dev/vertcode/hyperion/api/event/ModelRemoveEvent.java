package dev.vertcode.hyperion.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.RemovalReason;

/**
 * Fired as a model is removed, while it can still be read.
 */
public final class ModelRemoveEvent extends ModelEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final RemovalReason reason;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param model the model being removed
     * @param reason why it is removed
     */
    @ApiStatus.Internal
    public ModelRemoveEvent(final Model model, final RemovalReason reason) {
        super(model);
        this.reason = reason;
    }

    /**
     * Return why the model is removed.
     *
     * @return the reason
     */
    public RemovalReason reason() {
        return this.reason;
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
