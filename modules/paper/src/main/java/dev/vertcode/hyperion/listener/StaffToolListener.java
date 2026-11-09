package dev.vertcode.hyperion.listener;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.vertcode.hyperion.debug.DebugViews;
import dev.vertcode.hyperion.ui.Previews;

/**
 * Forget a player's debug views and remove their gallery preview when they leave.
 */
public final class StaffToolListener implements Listener {

    private final DebugViews debug;
    private final Previews previews;

    /**
     * Create the listener.
     *
     * @param debug the debug views
     * @param previews the gallery's previews
     */
    public StaffToolListener(final DebugViews debug, final Previews previews) {
        this.debug = debug;
        this.previews = previews;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        this.debug.hideAll(player);
        this.previews.clear(player);
    }
}
