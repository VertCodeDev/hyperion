package dev.vertcode.hyperion.model;

import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.animation.TimedEffect;
import dev.vertcode.hyperion.api.animation.AnimationHandle;
import dev.vertcode.hyperion.api.model.Model;

/**
 * Plays the effect keyframes a model's animations pass: Blockbench's sounds, particles and
 * timeline scripts.
 */
@FunctionalInterface
public interface ModelEffects {

    /**
     * Nothing plays, until the effects are installed.
     */
    ModelEffects NONE = (model, animation, effect, viewer) -> {
    };

    /**
     * Play one effect keyframe. Called on the thread that owns the model.
     *
     * @param model the model
     * @param animation the animation that passed it
     * @param effect the keyframe
     * @param viewer the player the animation plays for, or null when it plays for everyone
     */
    void passed(Model model, AnimationHandle animation, TimedEffect effect, @Nullable Player viewer);
}
