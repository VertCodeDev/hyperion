package dev.vertcode.hyperion.api.armor;

import java.util.Collection;
import java.util.Optional;

import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

/**
 * Every armor set Hyperion loaded, and what each player wears.
 *
 * <pre>{@code
 * ArmorSet frost = Hyperion.armor().find(Key.key("hyperion", "frost")).orElseThrow();
 * player.getInventory().addItem(frost.piece(ArmorSlot.CHEST).orElseThrow().item());
 *
 * if (Hyperion.armor().worn(player).hasFullSet(frost)) {
 *     // the set bonus
 * }
 * }</pre>
 *
 * <p>For changes, listen to the armor events instead of polling: {@code ArmorEquipEvent},
 * {@code ArmorUnequipEvent}, {@code ArmorSetCompleteEvent} and {@code ArmorSetBreakEvent}.
 * Every method is safe from any thread.
 */
@ApiStatus.NonExtendable
public interface ArmorRegistry {

    /**
     * Find a set by its key.
     *
     * @param key the key, such as {@code hyperion:frost}
     * @return the set, or empty when no set has that key
     */
    Optional<ArmorSet> find(Key key);

    /**
     * Return every loaded set.
     *
     * @return the sets, sorted by key
     */
    @Unmodifiable Collection<ArmorSet> all();

    /**
     * Find the piece an item is.
     *
     * @param item the item, or null
     * @return the piece, or empty for null, air or an item that is no Hyperion armor
     */
    Optional<ArmorPiece> pieceOf(@Nullable ItemStack item);

    /**
     * Return whether an item is a piece of Hyperion armor.
     *
     * @param item the item, or null
     * @return true for a Hyperion piece
     */
    boolean isArmor(@Nullable ItemStack item);

    /**
     * Return the Hyperion armor a player wears.
     *
     * @param player the player
     * @return a snapshot of what the player wears
     */
    WornArmor worn(Player player);
}
