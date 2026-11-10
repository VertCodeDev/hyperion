package dev.vertcode.hyperion.api.event;

import java.util.Collection;
import java.util.List;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

import dev.vertcode.hyperion.api.armor.ArmorSet;

/**
 * Fired off the server thread when the armor sets have loaded or reloaded and been published.
 *
 * <p>The sets before the reload are gone; look sets up again by key.
 */
public final class ArmorLoadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final List<ArmorSet> sets;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param sets every set now loaded
     */
    @ApiStatus.Internal
    public ArmorLoadEvent(final Collection<ArmorSet> sets) {
        super(true);
        this.sets = List.copyOf(sets);
    }

    /**
     * Return every set now loaded.
     *
     * @return the sets
     */
    public @Unmodifiable List<ArmorSet> sets() {
        return this.sets;
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
