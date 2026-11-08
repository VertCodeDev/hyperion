package dev.vertcode.hyperion.api.model.view;

import java.util.Optional;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.Appearance;
import dev.vertcode.hyperion.api.model.Model;

/**
 * How one player sees a model, on top of what everyone sees.
 *
 * <p>A value set here wins over the model's and its bones' for this player only: tint a boss red
 * for the player it targets, or hide a quest marker once a player has finished the quest. The
 * view lives until {@link #reset()}, the model is removed or the player quits.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface PlayerView extends Appearance<PlayerView> {

    /**
     * Return the player this view is for.
     *
     * @return the player
     */
    Player player();

    /**
     * Return the model this view is of.
     *
     * @return the model
     */
    Model model();

    /**
     * Find the overrides for one bone, for this player.
     *
     * @param name the bone's name
     * @return the bone's overrides, or empty if the model has no bone by that name
     */
    Optional<ViewBone> bone(String name);

    /**
     * Drop every override in this view, so the player sees what everyone sees.
     */
    void reset();
}
