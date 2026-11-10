package dev.vertcode.hyperion.armor;

import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;

import dev.vertcode.hyperion.api.armor.ArmorPiece;
import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.armor.ArmorStats;

/**
 * One piece of a loaded set.
 */
public final class HyperionArmorPiece implements ArmorPiece {

    private final HyperionArmorSet set;
    private final ArmorSlot slot;
    private final ArmorStats stats;
    private final ArmorItems items;

    /**
     * Create a piece.
     *
     * @param set the set
     * @param slot the slot it is worn in
     * @param stats how much it protects
     * @param items builds its items
     */
    HyperionArmorPiece(final HyperionArmorSet set, final ArmorSlot slot, final ArmorStats stats, final ArmorItems items) {
        this.set = set;
        this.slot = slot;
        this.stats = stats;
        this.items = items;
    }

    @Override
    public HyperionArmorSet set() {
        return this.set;
    }

    @Override
    public ArmorSlot slot() {
        return this.slot;
    }

    @Override
    public ArmorStats stats() {
        return this.stats;
    }

    @Override
    public ItemStack item() {
        return this.items.build(this, null);
    }

    @Override
    public ItemStack item(final Color color) {
        return this.items.build(this, color);
    }

    @Override
    public String toString() {
        return "ArmorPiece[" + this.set.key().asString() + " " + this.slot.piece() + "]";
    }
}
