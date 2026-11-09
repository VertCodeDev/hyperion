package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.animation.AnimationHandle;
import dev.vertcode.hyperion.api.model.Model;

/**
 * Fired when a model's animation passes a timeline keyframe from Blockbench, on the thread that
 * owns the model.
 *
 * <p>The keyframe's script is its text, trimmed, such as {@code breath_damage}.
 */
public final class AnimationKeyframeEvent extends ModelEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final AnimationHandle animation;
    private final String script;
    private final @Nullable Player viewer;

    /**
     * Create the event. Fired by Hyperion only.
     *
     * @param model the model
     * @param animation the animation that passed the keyframe
     * @param script the keyframe's script
     * @param viewer the player the animation plays for, or null when it plays for everyone
     */
    @ApiStatus.Internal
    public AnimationKeyframeEvent(
            final Model model,
            final AnimationHandle animation,
            final String script,
            final @Nullable Player viewer
    ) {
        super(model);
        this.animation = animation;
        this.script = script;
        this.viewer = viewer;
    }

    /**
     * Return the animation that passed the keyframe.
     *
     * @return the animation
     */
    public AnimationHandle animation() {
        return this.animation;
    }

    /**
     * Return the keyframe's script.
     *
     * @return the script, trimmed
     */
    public String script() {
        return this.script;
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
