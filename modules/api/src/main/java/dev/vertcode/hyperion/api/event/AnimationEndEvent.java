package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.animation.AnimationHandle;
import dev.vertcode.hyperion.api.model.Model;

/**
 * An animation stopped playing on a model: it reached its end, or it was stopped.
 */
public final class AnimationEndEvent extends ModelEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final AnimationHandle animation;
    private final boolean finished;
    private final @Nullable Player viewer;

    /**
     * Create the event.
     *
     * @param model the model
     * @param animation the animation that ended
     * @param finished true when it reached its end, false when it was stopped
     * @param viewer the player it played for, or null when it played for everyone
     */
    @ApiStatus.Internal
    public AnimationEndEvent(
            final Model model,
            final AnimationHandle animation,
            final boolean finished,
            final @Nullable Player viewer
    ) {
        super(model);
        this.animation = animation;
        this.finished = finished;
        this.viewer = viewer;
    }

    /**
     * Return the animation that ended.
     *
     * @return the animation
     */
    public AnimationHandle animation() {
        return this.animation;
    }

    /**
     * Return whether the animation reached its end rather than being stopped.
     *
     * @return true when it finished
     */
    public boolean finished() {
        return this.finished;
    }

    /**
     * Return the player the animation played for.
     *
     * @return the player, or null when it played for everyone
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
