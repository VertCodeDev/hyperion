package dev.vertcode.hyperion.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.Model;

/**
 * Fired after a model spawns and before any player sees it.
 *
 * <p>Change the model here and the first frame already shows it. To stop a model, call
 * {@link Model#remove()}.
 */
public final class ModelSpawnEvent extends ModelEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param model the model that spawned
     */
    @ApiStatus.Internal
    public ModelSpawnEvent(final Model model) {
        super(model);
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
