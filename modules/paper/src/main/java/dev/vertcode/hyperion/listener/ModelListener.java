package dev.vertcode.hyperion.listener;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.behavior.HyperionBehaviorRegistry;
import dev.vertcode.hyperion.model.HyperionModelRegistry;
import dev.vertcode.hyperion.model.ModelManager;

/**
 * Keep live models in step with the world: who tracks the entity carrying one, entities that
 * leave, players whose client drops every entity, and plugins that disable.
 *
 * <p>Tracking and removal events fire on the thread that owns the entity, which is the thread
 * that owns its models, so the models change right there.
 */
public final class ModelListener implements Listener {

    private final ModelManager models;
    private final HyperionModelRegistry registry;
    private final HyperionBehaviorRegistry behaviors;

    /**
     * Create the listener.
     *
     * @param models the live models
     * @param registry the blueprints, for dropping a disabled plugin's models
     * @param behaviors the behavior types, for dropping a disabled plugin's types
     */
    public ModelListener(
            final ModelManager models,
            final HyperionModelRegistry registry,
            final HyperionBehaviorRegistry behaviors
    ) {
        this.models = models;
        this.registry = registry;
        this.behaviors = behaviors;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrack(final PlayerTrackEntityEvent event) {
        this.models.tracked(event.getPlayer(), event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUntrack(final PlayerUntrackEntityEvent event) {
        this.models.untracked(event.getPlayer(), event.getEntity());
    }

    /**
     * Remove the models on entities that unload with their chunk. This fires before they leave
     * the world, so {@link #onRemove} finds nothing left on them.
     *
     * @param event the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnload(final EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            this.models.entityRemoved(entity, RemovalReason.ENTITY_UNLOADED);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(final EntityRemoveFromWorldEvent event) {
        this.models.entityRemoved(event.getEntity(), RemovalReason.ENTITY_REMOVED);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        this.models.lose(event.getPlayer().getUniqueId(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangeWorld(final PlayerChangedWorldEvent event) {
        this.models.lose(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(final PlayerPostRespawnEvent event) {
        this.models.lose(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(final PluginDisableEvent event) {
        this.registry.unregisterAll(event.getPlugin());
        this.behaviors.unregisterAll(event.getPlugin());
    }
}
