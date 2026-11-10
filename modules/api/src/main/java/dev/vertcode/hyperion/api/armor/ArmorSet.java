package dev.vertcode.hyperion.api.armor;

import java.util.Collection;
import java.util.Optional;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * One armor set: a {@code .bbmodel} in {@code plugins/Hyperion/armor}, and its pieces.
 *
 * <p>A set has a piece for each slot its model has bones for, so a file with only a
 * {@code helmet} bone is a hat. Immutable: a reload makes new sets, so look a set up by its key
 * again rather than keeping it.
 */
@ApiStatus.NonExtendable
public interface ArmorSet {

    /**
     * Return the set's key, {@code hyperion:<file name>}.
     *
     * @return the key
     */
    Key key();

    /**
     * Return the set's name for players, from its {@code .yml} or made from the key.
     *
     * @return the display name, in MiniMessage
     */
    String displayName();

    /**
     * Return the set's pieces, helmet first.
     *
     * @return the pieces
     */
    @Unmodifiable Collection<ArmorPiece> pieces();

    /**
     * Return the piece for one slot.
     *
     * @param slot the slot
     * @return the piece, or empty when the set has none there
     */
    Optional<ArmorPiece> piece(ArmorSlot slot);

    /**
     * Return whether players may dye this set; its {@code _tint} textures take the color.
     *
     * @return true when the set is dyeable
     */
    boolean dyeable();

    /**
     * Return the free values from the set's {@code .yml}, for plugins.
     *
     * @return the data
     */
    ArmorData data();
}
