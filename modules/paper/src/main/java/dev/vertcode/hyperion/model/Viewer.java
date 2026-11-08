package dev.vertcode.hyperion.model;

import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.entity.Player;

/**
 * A player who sees a model, with the connection packets go to.
 *
 * @param player the player
 * @param user the player's PacketEvents connection
 */
record Viewer(Player player, User user) {
}
