package dev.vertcode.hyperion.api.armor;

/**
 * How much one armor piece protects: the numbers a vanilla piece has too.
 *
 * @param armor the armor points, the chestplate icons on the armor bar
 * @param toughness how much big hits are reduced
 * @param knockbackResistance how much knockback is resisted, from 0 to 1
 */
public record ArmorStats(double armor, double toughness, double knockbackResistance) {
}
