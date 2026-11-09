package dev.vertcode.hyperion.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.effect.EffectHandle;

/**
 * Fired when an effect starts, on the thread it plays on, before its first tick.
 */
public final class EffectStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EffectHandle effect;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param effect the effect that starts
     */
    @ApiStatus.Internal
    public EffectStartEvent(final EffectHandle effect) {
        this.effect = effect;
    }

    /**
     * Return the effect that starts.
     *
     * @return the effect
     */
    public EffectHandle effect() {
        return this.effect;
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
