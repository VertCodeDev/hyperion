package dev.vertcode.hyperion.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import dev.vertcode.hyperion.api.event.InteractionType;
import dev.vertcode.hyperion.api.event.ModelInteractEvent;
import dev.vertcode.hyperion.api.model.EntityAnchor;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.behavior.builtin.SeatBehavior;
import dev.vertcode.hyperion.api.model.behavior.builtin.SeatController;
import dev.vertcode.hyperion.model.ModelInteractions;
import dev.vertcode.hyperion.model.ModelManager;
import dev.vertcode.hyperion.model.SeatEntry;

/**
 * Seat players who right-click a model with a free seat, keep riders in locked seats, and clear a
 * seat when its rider gets off.
 *
 * <p>A click on a hitbox arrives as {@link ModelInteractEvent}, a click on the entity carrying a
 * model as a plain entity click; sneaking clicks are left to the model menu. Every event here
 * fires on the thread that owns the player, which is the model's thread for a model in reach.
 */
public final class SeatListener implements Listener {

    private final ModelManager models;
    private final ModelInteractions index;

    /**
     * Create the listener.
     *
     * @param models the live models
     */
    public SeatListener(final ModelManager models) {
        this.models = models;
        this.index = models.interactions();
    }

    /**
     * Seat a player who right-clicks a model's hitbox.
     *
     * @param event the event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTouch(final ModelInteractEvent event) {
        Player player = event.player();
        boolean wanted = event.type() == InteractionType.INTERACT && event.hand() == EquipmentSlot.HAND;
        if (wanted && !player.isSneaking() && this.ride(event.model(), player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Seat a player who right-clicks the entity carrying a model.
     *
     * @param event the event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(final PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND || player.isSneaking()) {
            return;
        }

        for (Model model : this.models.of(event.getRightClicked())) {
            if (this.ride(model, player)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Keep a rider in a locked seat, and clear a seat its rider left.
     *
     * @param event the event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDismount(final EntityDismountEvent event) {
        SeatEntry entry = this.index.seat(event.getDismounted().getEntityId());
        if (entry == null) {
            return;
        }

        if (entry.seat().locked() && event.isCancellable()) {
            event.setCancelled(true);
            return;
        }

        this.models.runOnOwner(entry.model(), entry.seat()::dismount);
    }

    /**
     * Keep a rider from hurting the entity they ride, unless the seat's controller allows it.
     *
     * @param event the event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player rider) || rider.getVehicle() == null) {
            return;
        }

        SeatEntry entry = this.index.seat(rider.getVehicle().getEntityId());
        if (entry == null || !(entry.model().anchor() instanceof EntityAnchor(Entity carrier)) || !carrier.equals(event.getEntity())) {
            return;
        }

        SeatController controller = entry.seat().controller();
        if (controller == null || !controller.riderCanHurtVehicle()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        this.index.forget(event.getPlayer().getUniqueId());
    }

    /**
     * Put a player in a model's first free seat, when riding by click is on.
     *
     * @param model the model
     * @param player the player
     * @return true when the player now sits in it
     */
    private boolean ride(final Model model, final Player player) {
        if (!this.models.settings().behaviors().clickToRide()) {
            return false;
        }

        SeatBehavior free = null;
        for (SeatBehavior seat : model.seats()) {
            Entity rider = seat.passenger().orElse(null);
            if (player.equals(rider)) {
                return false;
            }

            if (rider == null && free == null) {
                free = seat;
            }
        }

        return free != null && free.mount(player);
    }
}
