package dev.vertcode.hyperion.api.armor;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The Hyperion armor one player wears, as of the last change Hyperion saw.
 *
 * <p>A snapshot: it does not change when the player's armor does, so ask again for the current
 * state. Safe to read from any thread.
 */
@ApiStatus.NonExtendable
public interface WornArmor {

    /**
     * Return the player this is about.
     *
     * @return the player's id
     */
    UUID player();

    /**
     * Return the Hyperion piece in one slot.
     *
     * @param slot the slot
     * @return the piece, or empty when the slot holds nothing or a non-Hyperion item
     */
    Optional<ArmorPiece> piece(ArmorSlot slot);

    /**
     * Return how many pieces of a set the player wears.
     *
     * @param set the set
     * @return the count, from 0 to the set's number of pieces
     */
    int count(ArmorSet set);

    /**
     * Return whether the player wears every piece the set has.
     *
     * @param set the set
     * @return true for the full set
     */
    boolean hasFullSet(ArmorSet set);

    /**
     * Return every set the player wears at least one piece of.
     *
     * @return the sets
     */
    @Unmodifiable Set<ArmorSet> sets();
}
