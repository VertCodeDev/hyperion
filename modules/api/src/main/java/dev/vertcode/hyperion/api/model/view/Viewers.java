package dev.vertcode.hyperion.api.model.view;

import java.util.Collection;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Who sees a model now, and who may.
 *
 * <p>A player sees a model when they are in range and the policy allows them, unless you
 * {@link #show} or {@link #hide} them by hand, which wins over the policy until you
 * {@link #reset} them. Every method is safe from any thread; changes apply on the next frame.
 */
@ApiStatus.NonExtendable
public interface Viewers {

    /**
     * Return the players who see the model now.
     *
     * @return a snapshot of the viewers
     */
    @Unmodifiable Collection<Player> current();

    /**
     * Return whether a player sees the model now.
     *
     * @param player the player
     * @return true when they see it
     */
    boolean isViewing(Player player);

    /**
     * Return the policy deciding who may see the model.
     *
     * @return the policy
     */
    ViewerPolicy policy();

    /**
     * Change who may see the model. Current viewers the new policy refuses lose sight of it.
     *
     * @param policy the new policy
     */
    void policy(ViewerPolicy policy);

    /**
     * Return how far away players can see the model.
     *
     * @return the view distance in blocks
     */
    int viewDistance();

    /**
     * Change how far away players can see the model.
     *
     * @param blocks the view distance, above 0
     * @throws IllegalArgumentException if the distance is not above 0
     */
    void viewDistance(int blocks);

    /**
     * Let a player see the model whatever the policy says, while in range.
     *
     * @param player the player
     */
    void show(Player player);

    /**
     * Keep a player from seeing the model whatever the policy says.
     *
     * @param player the player
     */
    void hide(Player player);

    /**
     * Drop a {@link #show} or {@link #hide} for a player, so the policy decides again.
     *
     * @param player the player
     */
    void reset(Player player);
}
