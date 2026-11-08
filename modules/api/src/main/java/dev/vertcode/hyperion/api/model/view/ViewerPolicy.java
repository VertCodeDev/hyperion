package dev.vertcode.hyperion.api.model.view;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * Who may see a model. View distance always applies on top.
 *
 * <p>Checked on the model's region whenever a player comes into range, so keep it cheap and read
 * only what is safe there: the player's id and permissions, not their inventory or location.
 */
@FunctionalInterface
public interface ViewerPolicy {

    /**
     * Let every player in range see the model.
     *
     * @return the policy
     */
    static ViewerPolicy everyone() {
        return _ -> true;
    }

    /**
     * Let only the listed players see the model.
     *
     * @param players the ids of the players, copied
     * @return the policy
     */
    static ViewerPolicy only(final Collection<UUID> players) {
        Set<UUID> allowed = Set.copyOf(players);

        return player -> allowed.contains(player.getUniqueId());
    }

    /**
     * Let only players with a permission see the model.
     *
     * @param permission the permission node
     * @return the policy
     */
    static ViewerPolicy permission(final String permission) {
        return player -> player.hasPermission(permission);
    }

    /**
     * Decide whether a player in range may see the model.
     *
     * @param player the player
     * @return true when the player may see it
     */
    boolean canSee(Player player);

    /**
     * Combine with another policy: a player may see the model when both allow it.
     *
     * @param other the other policy
     * @return the combined policy
     */
    default ViewerPolicy and(final ViewerPolicy other) {
        return player -> this.canSee(player) && other.canSee(player);
    }
}
