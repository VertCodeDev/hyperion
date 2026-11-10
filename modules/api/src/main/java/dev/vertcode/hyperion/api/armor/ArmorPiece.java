package dev.vertcode.hyperion.api.armor;

import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * One piece of an armor set, such as its chestplate.
 */
@ApiStatus.NonExtendable
public interface ArmorPiece {

    /**
     * Return the set this piece belongs to.
     *
     * @return the set
     */
    ArmorSet set();

    /**
     * Return the slot this piece is worn in.
     *
     * @return the slot
     */
    ArmorSlot slot();

    /**
     * Return how much this piece protects.
     *
     * @return the stats
     */
    ArmorStats stats();

    /**
     * Build a new item of this piece, made for the server's version: its model, how it is worn,
     * its stats and its name. Hyperion knows the item again by a tag, so renaming, enchanting or
     * trimming it keeps it this piece.
     *
     * @return a new item
     * @throws IllegalStateException if the resource pack has not been built yet
     */
    ItemStack item();

    /**
     * Build a new item of this piece in a dye color. Only a {@link ArmorSet#dyeable() dyeable}
     * set shows the color, and only from 1.21.2.
     *
     * @param color the dye color
     * @return a new item
     * @throws IllegalStateException if the resource pack has not been built yet
     */
    ItemStack item(Color color);
}
