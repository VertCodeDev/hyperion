package dev.vertcode.hyperion.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.effect.EffectHandle;

/**
 * Fired when an effect shows what it is about, on the thread it plays on: at its reveal tick, or
 * as it ends when it was stopped sooner. Fired once per play, and not at all for an effect
 * without a reveal tick.
 */
public final class EffectRevealEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EffectHandle effect;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param effect the effect that reveals
     */
    @ApiStatus.Internal
    public EffectRevealEvent(final EffectHandle effect) {
        this.effect = effect;
    }

    /**
     * Return the effect that reveals.
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
