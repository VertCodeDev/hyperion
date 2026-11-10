package dev.vertcode.hyperion.armor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import net.kyori.adventure.key.Key;

import dev.vertcode.hyperion.api.armor.ArmorSlot;

/**
 * The numbers the built pack gave armor items: custom model data before 1.21.4, and the color
 * that names each set on 1.21 to 1.21.1.
 *
 * <p>Immutable. The pack builder fills a fresh one on every build.
 */
public final class ArmorPackInfo {

    /**
     * The middle and low byte of every set color on 1.21 to 1.21.1; the high byte is the set's
     * number. The shader only reads leather dyed exactly so as a set.
     */
    public static final int COLOR_TAG = 0x4859;

    private static final int COLOR_SHIFT = 16;

    private final Map<Key, Map<ArmorSlot, Integer>> customModelData;
    private final Map<Key, Integer> setNumbers;

    /**
     * Create the info.
     *
     * @param customModelData the custom model data of each piece, by set
     * @param setNumbers the number of each set on 1.21 to 1.21.1, from 1
     */
    public ArmorPackInfo(final Map<Key, Map<ArmorSlot, Integer>> customModelData, final Map<Key, Integer> setNumbers) {
        Map<Key, Map<ArmorSlot, Integer>> copy = new HashMap<>();
        customModelData.forEach((key, pieces) -> copy.put(key, Map.copyOf(new EnumMap<>(pieces))));
        this.customModelData = Map.copyOf(copy);
        this.setNumbers = Map.copyOf(setNumbers);
    }

    /**
     * Return info without any numbers, for a pack without armor.
     *
     * @return the empty info
     */
    public static ArmorPackInfo empty() {
        return new ArmorPackInfo(Map.of(), Map.of());
    }

    /**
     * Return the custom model data of a piece.
     *
     * @param set the set's key
     * @param slot the piece's slot
     * @return the number, or empty when the pack gave it none
     */
    public OptionalInt customModelData(final Key set, final ArmorSlot slot) {
        Integer number = this.customModelData.getOrDefault(set, Map.of()).get(slot);

        return number == null ? OptionalInt.empty() : OptionalInt.of(number);
    }

    /**
     * Return the leather color that names a set on 1.21 to 1.21.1.
     *
     * @param set the set's key
     * @return the color as RGB, or empty when the pack gave the set no number
     */
    public OptionalInt setColor(final Key set) {
        Integer number = this.setNumbers.get(set);

        return number == null ? OptionalInt.empty() : OptionalInt.of(color(number));
    }

    /**
     * Return the leather color of a set number.
     *
     * @param number the set's number, from 1 to 255
     * @return the color as RGB
     */
    public static int color(final int number) {
        return (number << COLOR_SHIFT) | COLOR_TAG;
    }
}
