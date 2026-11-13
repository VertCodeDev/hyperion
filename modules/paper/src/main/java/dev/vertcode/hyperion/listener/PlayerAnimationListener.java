package dev.vertcode.hyperion.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import dev.vertcode.hyperion.api.event.ModelRemoveEvent;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.player.HyperionPlayerAnimations;
import dev.vertcode.hyperion.skin.HyperionSkins;

/**
 * Fetches the skins of joining players, ends an animation on a player when they leave, die or
 * change worlds, or sneak or are hurt when it was played to end that way, and holds a player in
 * place while a locked animation plays.
 */
public final class PlayerAnimationListener implements Listener {

    private final HyperionPlayerAnimations animations;
    private final HyperionSkins skins;
    private final BlueprintRegistry registry;

    /**
     * Create the listener.
     *
     * @param animations the animations on players it ends
     * @param skins the skin service, to fetch a joining player's skin before it is needed
     * @param registry the loaded blueprints, to tell whether any is a player model
     */
    public PlayerAnimationListener(
            final HyperionPlayerAnimations animations,
            final HyperionSkins skins,
            final BlueprintRegistry registry
    ) {
        this.animations = animations;
        this.skins = skins;
        this.registry = registry;
    }

    /**
     * Fetch a joining player's skin when the server has a player model, so the model that stands
     * in for them wears it from its first frame. The download runs off the tick threads.
     *
     * @param event the join
     */
    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        if (!this.skins.supported()) {
            return;
        }

        for (Blueprint blueprint : this.registry.all()) {
            if (blueprint.skinned()) {
                this.skins.of(event.getPlayer());
                return;
            }
        }
    }

    /**
     * Keep a player where they are while a locked animation plays. They may still look around and
     * fall.
     *
     * @param event the move
     */
    @EventHandler(ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent event) {
        if (!event.hasChangedPosition() || !this.animations.holds(event.getPlayer())) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to.getX() == from.getX() && to.getZ() == from.getZ() && to.getY() <= from.getY()) {
            return;
        }

        Location held = to.clone();
        held.setX(from.getX());
        held.setZ(from.getZ());
        held.setY(Math.min(from.getY(), to.getY()));
        event.setTo(held);
    }

    /**
     * End a player's animation when they start sneaking, when it was played to end that way.
     *
     * @param event the sneak
     */
    @EventHandler(ignoreCancelled = true)
    public void onSneak(final PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            this.animations.sneaked(event.getPlayer());
        }
    }

    /**
     * End a player's animation when they are hurt, when it was played to end that way.
     *
     * @param event the damage
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            this.animations.damaged(player);
        }
    }

    /**
     * End a player's animation when they die.
     *
     * @param event the death
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(final PlayerDeathEvent event) {
        this.animations.stop(event.getEntity());
    }

    /**
     * End a player's animation when they change worlds.
     *
     * @param event the change
     */
    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        this.animations.stop(event.getPlayer());
    }

    /**
     * End a player's animation when they leave.
     *
     * @param event the quit
     */
    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        this.animations.stop(event.getPlayer());
    }

    /**
     * End the animation on a model that something else removed.
     *
     * @param event the removal
     */
    @EventHandler
    public void onRemove(final ModelRemoveEvent event) {
        this.animations.removed(event.model());
    }
}
