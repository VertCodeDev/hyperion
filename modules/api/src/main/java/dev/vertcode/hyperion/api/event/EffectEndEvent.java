package dev.vertcode.hyperion.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.effect.EffectHandle;

/**
 * Fired when an effect ends, on the thread it played on.
 */
public final class EffectEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EffectHandle effect;
    private final boolean finished;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param effect the effect that ended
     * @param finished true when it played to its end, false when it was stopped
     */
    @ApiStatus.Internal
    public EffectEndEvent(final EffectHandle effect, final boolean finished) {
        this.effect = effect;
        this.finished = finished;
    }

    /**
     * Return the effect that ended.
     *
     * @return the effect
     */
    public EffectHandle effect() {
        return this.effect;
    }

    /**
     * Return whether the effect played to its end.
     *
     * @return true when it finished, false when it was stopped or its anchor went
     */
    public boolean finished() {
        return this.finished;
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
