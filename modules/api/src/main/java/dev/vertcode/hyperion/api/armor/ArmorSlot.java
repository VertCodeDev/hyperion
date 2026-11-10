package dev.vertcode.hyperion.api.armor;

import java.util.Locale;
import java.util.Optional;

import org.bukkit.inventory.EquipmentSlot;

/**
 * The four armor slots, and the piece of a set that goes in each.
 */
public enum ArmorSlot {

    /**
     * The helmet.
     */
    HEAD(EquipmentSlot.HEAD, "helmet"),

    /**
     * The chestplate, with both arms.
     */
    CHEST(EquipmentSlot.CHEST, "chestplate"),

    /**
     * The leggings.
     */
    LEGS(EquipmentSlot.LEGS, "leggings"),

    /**
     * The boots.
     */
    FEET(EquipmentSlot.FEET, "boots");

    private final EquipmentSlot equipmentSlot;
    private final String piece;

    ArmorSlot(final EquipmentSlot equipmentSlot, final String piece) {
        this.equipmentSlot = equipmentSlot;
        this.piece = piece;
    }

    /**
     * Return the Bukkit slot.
     *
     * @return the equipment slot
     */
    public EquipmentSlot equipmentSlot() {
        return this.equipmentSlot;
    }

    /**
     * Return the name of the piece that goes here, such as {@code chestplate}. It is also how the
     * piece is named in commands and in a set's files.
     *
     * @return the piece's name
     */
    public String piece() {
        return this.piece;
    }

    /**
     * Find the armor slot of a Bukkit slot.
     *
     * @param slot the equipment slot
     * @return the armor slot, or empty for a hand or body slot
     */
    public static Optional<ArmorSlot> of(final EquipmentSlot slot) {
        for (ArmorSlot armor : values()) {
            if (armor.equipmentSlot == slot) {
                return Optional.of(armor);
            }
        }

        return Optional.empty();
    }

    /**
     * Find a slot by its piece's name, such as {@code boots}, or by its own name, such as
     * {@code feet}.
     *
     * @param name the name, in any case
     * @return the slot, or empty when the name is neither
     */
    public static Optional<ArmorSlot> fromName(final String name) {
        String lower = name.trim().toLowerCase(Locale.ROOT);
        for (ArmorSlot armor : values()) {
            if (armor.piece.equals(lower) || armor.name().toLowerCase(Locale.ROOT).equals(lower)) {
                return Optional.of(armor);
            }
        }

        return Optional.empty();
    }
}
