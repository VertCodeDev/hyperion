package dev.vertcode.hyperion.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.vertcode.hyperion.bedrock.BedrockPlayers;

/**
 * Work out who is on Bedrock as they join, before any model is sent to them, and forget them as
 * they leave.
 */
public final class BedrockListener implements Listener {

    private final BedrockPlayers players;

    /**
     * Create the listener.
     *
     * @param players who is on Bedrock
     */
    public BedrockListener(final BedrockPlayers players) {
        this.players = players;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(final PlayerJoinEvent event) {
        this.players.joined(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        this.players.left(event.getPlayer().getUniqueId());
    }
}
