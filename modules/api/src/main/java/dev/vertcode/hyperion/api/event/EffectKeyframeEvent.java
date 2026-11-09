package dev.vertcode.hyperion.api.event;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.effect.EffectHandle;

/**
 * Fired when an effect reaches a keyframe of an {@code event} part, on the thread it plays on.
 *
 * <p>This is how an effect tells plugins a moment came, such as the tick a breath should hurt.
 */
public final class EffectKeyframeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EffectHandle effect;
    private final String name;
    private final Location location;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param effect the effect
     * @param name the keyframe's {@code name}
     * @param location where the part is, copied
     */
    @ApiStatus.Internal
    public EffectKeyframeEvent(final EffectHandle effect, final String name, final Location location) {
        this.effect = effect;
        this.name = name;
        this.location = location.clone();
    }

    /**
     * Return the effect.
     *
     * @return the effect
     */
    public EffectHandle effect() {
        return this.effect;
    }

    /**
     * Return the keyframe's name.
     *
     * @return the name
     */
    public String name() {
        return this.name;
    }

    /**
     * Return where the part is.
     *
     * @return a copy of the location
     */
    public Location location() {
        return this.location.clone();
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
