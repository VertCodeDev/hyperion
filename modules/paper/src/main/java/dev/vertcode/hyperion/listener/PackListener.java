package dev.vertcode.hyperion.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.pack.PackManager;

/**
 * Offer the pack on join, track how each player does with it, and kick players who refuse a
 * required pack.
 *
 * <p>Every event here fires on the region that owns the player, so the player may be touched
 * directly.
 */
public final class PackListener implements Listener {

    private final PackManager packs;

    /**
     * Create the listener.
     *
     * @param packs the pack to offer and track
     */
    public PackListener(final PackManager packs) {
        this.packs = packs;
    }

    /**
     * Remember the address the player joined with, where they can reach the pack's web server.
     *
     * @param event the login
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(final PlayerLoginEvent event) {
        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
            this.packs.rememberHost(event.getPlayer().getUniqueId(), event.getHostname());
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        if (this.packs.settings().sendOnJoin()) {
            this.packs.send(event.getPlayer());
        }
    }

    /**
     * Track the player's progress with the pack, log why a download failed, and kick them when
     * they refuse a required one.
     *
     * @param event the status update
     */
    @EventHandler
    public void onStatus(final PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        this.packs.explain(player, event.getID(), event.getStatus());
        if (this.packs.record(player.getUniqueId(), event.getID(), event.getStatus())) {
            player.kick(HyperionMessages.PACK__KICK.component());
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        this.packs.forget(event.getPlayer().getUniqueId());
    }
}
