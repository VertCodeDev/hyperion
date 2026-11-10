/**
 * Custom armor sets made in Blockbench: the sets, their pieces, and what a player wears.
 *
 * <p>Start at {@link ArmorRegistry}, through {@code Hyperion.armor()}. Equipping and taking off a
 * piece fires the armor events in {@code dev.vertcode.hyperion.api.event}, so a plugin builds set
 * bonuses and abilities on top without tracking inventories itself.
 *
 * <p>Null-marked: nothing in this package is null unless it says {@code @Nullable}.
 */
@NullMarked
package dev.vertcode.hyperion.api.armor;

import org.jspecify.annotations.NullMarked;
