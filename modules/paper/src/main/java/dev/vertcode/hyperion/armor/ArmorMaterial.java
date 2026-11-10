package dev.vertcode.hyperion.armor;

import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.armor.ArmorStats;

/**
 * The vanilla armor a set is made from: its items, its stats unless the set gives its own, and
 * its equip sound.
 *
 * <p>The items are real armor items, so enchanting, repairing and Bedrock players work as they
 * do for that armor. On 1.21 to 1.21.1 every set is made from leather instead, because only
 * leather can carry the color that tells the pack which set to draw.
 */
public enum ArmorMaterial {

    /**
     * Leather: 1, 3, 2, 1.
     */
    LEATHER("leather", 1, 3, 2, 1, 0, 0, "leather", null),

    /**
     * Chainmail: 2, 5, 4, 1.
     */
    CHAINMAIL("chainmail", 2, 5, 4, 1, 0, 0, "chain", null),

    /**
     * Iron: 2, 6, 5, 2.
     */
    IRON("iron", 2, 6, 5, 2, 0, 0, "iron", "iron"),

    /**
     * Gold: 2, 5, 3, 1.
     */
    GOLD("golden", 2, 5, 3, 1, 0, 0, "gold", "gold"),

    /**
     * Diamond: 3, 8, 6, 3, with 2 toughness.
     */
    DIAMOND("diamond", 3, 8, 6, 3, 2, 0, "diamond", "diamond"),

    /**
     * Netherite: 3, 8, 6, 3, with 3 toughness and a tenth knockback resistance.
     */
    NETHERITE("netherite", 3, 8, 6, 3, 3, 0.1, "netherite", "netherite");

    private final String prefix;
    private final double helmet;
    private final double chestplate;
    private final double leggings;
    private final double boots;
    private final double toughness;
    private final double knockbackResistance;
    private final String sound;
    private final @Nullable String trim;

    ArmorMaterial(
            final String prefix,
            final double helmet,
            final double chestplate,
            final double leggings,
            final double boots,
            final double toughness,
            final double knockbackResistance,
            final String sound,
            final @Nullable String trim
    ) {
        this.prefix = prefix;
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.sound = sound;
        this.trim = trim;
    }

    /**
     * Return the item of one piece, such as {@code netherite_chestplate}.
     *
     * @param slot the slot
     * @return the item's name, without namespace
     */
    public String item(final ArmorSlot slot) {
        return this.prefix + "_" + slot.piece();
    }

    /**
     * Return what one piece of this material protects in vanilla.
     *
     * @param slot the slot
     * @return the stats
     */
    public ArmorStats stats(final ArmorSlot slot) {
        double armor = switch (slot) {
            case HEAD -> this.helmet;
            case CHEST -> this.chestplate;
            case LEGS -> this.leggings;
            case FEET -> this.boots;
        };

        return new ArmorStats(armor, this.toughness, this.knockbackResistance);
    }

    /**
     * Return the sound of putting this armor on.
     *
     * @return the sound's key
     */
    public String equipSound() {
        return "minecraft:item.armor.equip_" + this.sound;
    }

    /**
     * Return the trim material that has a darker look on this armor, such as {@code iron} on
     * iron armor, so its inventory icon is not lost.
     *
     * @return the trim material, or null when no trim has one
     */
    public @Nullable String darkerTrim() {
        return this.trim;
    }

    /**
     * Find a material by name, such as {@code netherite} or {@code gold}.
     *
     * @param name the name, in any case
     * @return the material, or empty when there is none by that name
     */
    public static Optional<ArmorMaterial> fromName(final String name) {
        String lower = name.trim().toLowerCase(Locale.ROOT);
        for (ArmorMaterial material : values()) {
            if (material.name().toLowerCase(Locale.ROOT).equals(lower) || material.prefix.equals(lower)) {
                return Optional.of(material);
            }
        }

        return Optional.empty();
    }
}
