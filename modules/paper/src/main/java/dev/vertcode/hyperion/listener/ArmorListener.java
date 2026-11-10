package dev.vertcode.hyperion.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.event.ArmorChangeCause;
import dev.vertcode.hyperion.armor.ArmorTracker;

/**
 * Feed armor changes to the tracker, which fires the armor events.
 *
 * <p>Paper reports every armor change, whatever caused it; the other handlers here only tell
 * the tracker why a change is about to happen. Every handler runs on the player's own thread.
 */
public final class ArmorListener implements Listener {

    private final ArmorTracker tracker;

    /**
     * Create the listener.
     *
     * @param tracker the tracker
     */
    public ArmorListener(final ArmorTracker tracker) {
        this.tracker = tracker;
    }

    /**
     * Compare the player's armor after any change.
     *
     * @param event the change
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorChange(final PlayerArmorChangeEvent event) {
        this.tracker.update(event.getPlayer(), ArmorChangeCause.OTHER);
    }

    /**
     * Start tracking a player, firing the events for what they join in.
     *
     * @param event the join
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        this.tracker.join(event.getPlayer());
    }

    /**
     * Stop tracking a player, firing the events for what they leave in.
     *
     * @param event the quit
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        this.tracker.quit(event.getPlayer());
    }

    /**
     * Note that a dying player's armor is about to drop.
     *
     * @param event the death
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(final PlayerDeathEvent event) {
        if (event.getKeepInventory()) {
            return;
        }

        Player player = event.getEntity();
        for (ArmorSlot slot : ArmorSlot.values()) {
            this.tracker.expect(player.getUniqueId(), slot, ArmorChangeCause.DEATH);
        }
    }

    /**
     * Note that a piece of armor is about to break.
     *
     * @param event the break
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(final PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack broken = event.getBrokenItem();
        for (ArmorSlot slot : ArmorSlot.values()) {
            if (broken.isSimilar(player.getEquipment().getItem(slot.equipmentSlot()))) {
                this.tracker.expect(player.getUniqueId(), slot, ArmorChangeCause.BREAK);
            }
        }
    }
}
