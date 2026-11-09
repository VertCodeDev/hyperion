package dev.vertcode.hyperion.model;

import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.entity.Player;

/**
 * A player who sees a model, with the connection packets go to.
 *
 * @param player the player
 * @param user the player's PacketEvents connection
 * @param bedrock whether the player is on Bedrock, and sees the model through Geyser instead
 */
record Viewer(Player player, User user, boolean bedrock) {
}
