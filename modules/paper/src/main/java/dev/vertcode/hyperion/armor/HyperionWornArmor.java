package dev.vertcode.hyperion.armor;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.armor.ArmorPiece;
import dev.vertcode.hyperion.api.armor.ArmorSet;
import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.armor.WornArmor;

/**
 * What one player wore at the last change: the piece and item in each slot.
 */
final class HyperionWornArmor implements WornArmor {

    private final UUID player;
    private final Map<ArmorSlot, HyperionArmorPiece> pieces = new EnumMap<>(ArmorSlot.class);
    private final Map<ArmorSlot, ItemStack> items = new EnumMap<>(ArmorSlot.class);

    /**
     * Create a snapshot.
     *
     * @param player the player's id
     * @param pieces the piece in each slot that holds one
     * @param items the item of each of those pieces
     */
    HyperionWornArmor(final UUID player, final Map<ArmorSlot, HyperionArmorPiece> pieces, final Map<ArmorSlot, ItemStack> items) {
        this.player = player;
        this.pieces.putAll(pieces);
        items.forEach((slot, item) -> this.items.put(slot, item.clone()));
    }

    /**
     * Return a snapshot of a player wearing nothing.
     *
     * @param player the player's id
     * @return the snapshot
     */
    static HyperionWornArmor empty(final UUID player) {
        return new HyperionWornArmor(player, Map.of(), Map.of());
    }

    @Override
    public UUID player() {
        return this.player;
    }

    @Override
    public Optional<ArmorPiece> piece(final ArmorSlot slot) {
        return Optional.ofNullable(this.pieces.get(slot));
    }

    /**
     * Return the piece in a slot as Hyperion's own type.
     *
     * @param slot the slot
     * @return the piece, or null
     */
    @Nullable HyperionArmorPiece at(final ArmorSlot slot) {
        return this.pieces.get(slot);
    }

    /**
     * Return the item of the piece in a slot.
     *
     * @param slot the slot
     * @return a copy of the item, or an empty item when the slot holds no piece
     */
    ItemStack item(final ArmorSlot slot) {
        ItemStack item = this.items.get(slot);

        return item == null ? ItemStack.empty() : item.clone();
    }

    @Override
    public int count(final ArmorSet set) {
        int count = 0;
        for (HyperionArmorPiece piece : this.pieces.values()) {
            if (piece.set().key().equals(set.key())) {
                count++;
            }
        }

        return count;
    }

    @Override
    public boolean hasFullSet(final ArmorSet set) {
        return this.count(set) == set.pieces().size();
    }

    @Override
    public Set<ArmorSet> sets() {
        Set<ArmorSet> sets = new LinkedHashSet<>();
        for (HyperionArmorPiece piece : this.pieces.values()) {
            sets.add(piece.set());
        }

        return Collections.unmodifiableSet(sets);
    }
}
