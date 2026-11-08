package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.Model;

/**
 * Fired when a player starts or stops seeing a model.
 *
 * <p>This fires often on a busy server: keep listeners cheap.
 */
public final class ModelViewerEvent extends ModelEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean shown;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param model the model seen or no longer seen
     * @param player the player
     * @param shown true when the player now sees the model
     */
    @ApiStatus.Internal
    public ModelViewerEvent(final Model model, final Player player, final boolean shown) {
        super(model);
        this.player = player;
        this.shown = shown;
    }

    /**
     * Return the player who started or stopped seeing the model.
     *
     * @return the player
     */
    public Player player() {
        return this.player;
    }

    /**
     * Return whether the player now sees the model.
     *
     * @return true when shown, false when hidden
     */
    public boolean shown() {
        return this.shown;
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
