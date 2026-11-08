package dev.vertcode.hyperion.api.model.bone;

import java.util.function.Function;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.EntityAnchor;

/**
 * Picks the item a bone shows, again and again, so the item can follow the game.
 *
 * <p>A fixed item never changes. A source is asked every {@link #interval()} ticks while the
 * model has viewers, on the region that owns the model, and Hyperion sends only items that
 * changed. Return null to show the bone's own cubes, or an empty stack to show nothing.
 *
 * <pre>{@code
 * // the model's hand shows whatever the zombie carrying it holds
 * model.bone("right_hand").ifPresent(hand -> hand.itemSource(ItemSource.mainHand()));
 *
 * // a torch that is lit at night, checked once a second
 * torch.itemSource(ItemSource.of(context -> isNight(context.bone()) ? LIT : UNLIT).every(20));
 * }</pre>
 *
 * <p>Hyperion drops a source when the plugin it comes from disables.
 */
@FunctionalInterface
public interface ItemSource {

    /**
     * Show one item that never changes.
     *
     * @param item the item, copied; null shows the bone's own cubes
     * @return the source
     */
    static ItemSource fixed(final @Nullable ItemStack item) {
        ItemStack copy = item == null ? null : item.clone();

        return context -> copy;
    }

    /**
     * Show the same item to every viewer, picked by a function.
     *
     * @param picker picks the item for this frame
     * @return the source
     */
    static ItemSource of(final Function<ItemContext, @Nullable ItemStack> picker) {
        return picker::apply;
    }

    /**
     * Show each viewer their own item, such as their own head on a statue.
     *
     * <p>The picker runs once per viewer, so cache what it builds. It runs on the model's
     * region, which may not own the viewer: read only what is safe from anywhere, such as the
     * viewer's id, name, profile and permissions, not their inventory or location.
     *
     * @param picker picks the item for one viewer
     * @return the source
     */
    static ItemSource perViewer(final Function<Player, @Nullable ItemStack> picker) {
        return new PerViewerItemSource(picker);
    }

    /**
     * Show what the entity carrying the model has in an equipment slot. Shows nothing on a model
     * on its own, or when the slot is empty.
     *
     * @param slot the slot to mirror
     * @return the source
     */
    static ItemSource equipment(final EquipmentSlot slot) {
        return context -> {
            if (!(context.bone().model().anchor() instanceof EntityAnchor(Entity entity)) || !(entity instanceof LivingEntity living)) {
                return ItemStack.empty();
            }

            EntityEquipment equipment = living.getEquipment();

            return equipment == null ? ItemStack.empty() : equipment.getItem(slot);
        };
    }

    /**
     * Show what the entity carrying the model holds in its main hand.
     *
     * @return the source
     */
    static ItemSource mainHand() {
        return equipment(EquipmentSlot.HAND);
    }

    /**
     * Show what the entity carrying the model holds in its off hand.
     *
     * @return the source
     */
    static ItemSource offHand() {
        return equipment(EquipmentSlot.OFF_HAND);
    }

    /**
     * Pick the item for this frame.
     *
     * @param context the bone, and the viewer when {@link #perViewer()} is true
     * @return the item, null for the bone's own cubes, or an empty stack for nothing
     */
    @Nullable ItemStack item(ItemContext context);

    /**
     * Return whether this source is asked once per viewer rather than once for everyone.
     *
     * @return true when each viewer gets their own item
     */
    default boolean perViewer() {
        return false;
    }

    /**
     * Return how many ticks pass between two asks.
     *
     * @return the interval in ticks, 1 or more
     */
    default int interval() {
        return 1;
    }

    /**
     * Ask this source only every so many ticks, for items that change rarely.
     *
     * @param ticks the interval in ticks, 1 or more
     * @return a source that asks this one on that interval
     * @throws IllegalArgumentException if the ticks are below 1
     */
    default ItemSource every(final int ticks) {
        return new IntervalItemSource(this, ticks);
    }
}
