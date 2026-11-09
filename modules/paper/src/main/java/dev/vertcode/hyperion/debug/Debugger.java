package dev.vertcode.hyperion.debug;

import java.util.Set;

import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.entity.Player;

/**
 * A player with debug views on, and the connection their labels go to.
 *
 * @param player the player
 * @param user the player's PacketEvents connection
 * @param views the views they have on
 */
public record Debugger(Player player, User user, Set<DebugView> views) {

    /**
     * Return whether this player has a view on.
     *
     * @param view the view
     * @return true when it is on
     */
    public boolean has(final DebugView view) {
        return this.views.contains(view);
    }
}
