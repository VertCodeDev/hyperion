package dev.vertcode.hyperion.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.ModelBlueprint;

/**
 * Fired off the server thread when a blueprint has loaded or reloaded and been published.
 */
public final class BlueprintLoadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ModelBlueprint blueprint;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param blueprint the blueprint that loaded
     */
    @ApiStatus.Internal
    public BlueprintLoadEvent(final ModelBlueprint blueprint) {
        super(true);
        this.blueprint = blueprint;
    }

    /**
     * Return the blueprint that loaded.
     *
     * @return the blueprint
     */
    public ModelBlueprint blueprint() {
        return this.blueprint;
    }

    /**
     * Return whether this replaced an earlier load of the same key.
     *
     * @return true for a reload
     */
    public boolean reload() {
        return this.blueprint.revision() > 1;
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
