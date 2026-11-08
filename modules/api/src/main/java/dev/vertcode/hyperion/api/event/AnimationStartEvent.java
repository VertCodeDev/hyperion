package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.animation.AnimationHandle;
import dev.vertcode.hyperion.api.model.Model;

/**
 * An animation started playing on a model, for everyone or for one player.
 */
public final class AnimationStartEvent extends ModelEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final AnimationHandle animation;
    private final @Nullable Player viewer;

    /**
     * Create the event.
     *
     * @param model the model
     * @param animation the animation that started
     * @param viewer the player it plays for, or null when it plays for everyone
     */
    @ApiStatus.Internal
    public AnimationStartEvent(final Model model, final AnimationHandle animation, final @Nullable Player viewer) {
        super(model);
        this.animation = animation;
        this.viewer = viewer;
    }

    /**
     * Return the animation that started.
     *
     * @return the animation
     */
    public AnimationHandle animation() {
        return this.animation;
    }

    /**
     * Return the player the animation plays for.
     *
     * @return the player, or null when it plays for everyone
     */
    public @Nullable Player viewer() {
        return this.viewer;
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
