package dev.vertcode.hyperion.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.vertcode.hyperion.effect.HiddenPlayers;
import dev.vertcode.hyperion.effect.part.CameraLock;

/**
 * Give players back from an effect's camera when something takes them away, keep a held player
 * safe when the camera asks, and show a player again who leaves while an effect hides them.
 */
public final class EffectListener implements Listener {

    private final CameraLock cameras;
    private final HiddenPlayers hidden;

    /**
     * Create the listener.
     *
     * @param cameras the players cameras hold
     * @param hidden the players effects hide from their viewers
     */
    public EffectListener(final CameraLock cameras, final HiddenPlayers hidden) {
        this.cameras = cameras;
        this.hidden = hidden;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && this.cameras.safe(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(final PlayerDeathEvent event) {
        this.cameras.release(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangeWorld(final PlayerChangedWorldEvent event) {
        this.cameras.release(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        this.cameras.forget(event.getPlayer().getUniqueId());
        this.hidden.left(event.getPlayer());
    }
}
