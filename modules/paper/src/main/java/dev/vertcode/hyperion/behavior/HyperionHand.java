package dev.vertcode.hyperion.behavior;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.api.model.behavior.builtin.HandBehavior;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.api.model.bone.ItemSource;

/**
 * A bone that shows what the entity carrying the model holds in one hand, and its own cubes
 * while the hand is empty.
 */
final class HyperionHand implements HandBehavior {

    private static final int INTERVAL = 2;

    private final Bone bone;
    private final EquipmentSlot slot;
    private final ItemSource source;

    /**
     * Create the hand of a bone.
     *
     * @param bone the bone
     * @param slot {@link EquipmentSlot#HAND} or {@link EquipmentSlot#OFF_HAND}
     */
    HyperionHand(final Bone bone, final EquipmentSlot slot) {
        this.bone = bone;
        this.slot = slot;
        ItemSource held = ItemSource.equipment(slot);
        this.source = ItemSource.of(context -> {
            ItemStack item = held.item(context);
            return item == null || item.isEmpty() ? null : item;
        }).every(INTERVAL);
    }

    @Override
    public void onSpawn() {
        if (this.bone.itemSource() == null) {
            this.bone.itemSource(this.source);
        }
    }

    @Override
    public void onRemove(final RemovalReason reason) {
        if (this.source.equals(this.bone.itemSource())) {
            this.bone.itemSource(null);
        }
    }

    @Override
    public EquipmentSlot slot() {
        return this.slot;
    }
}
