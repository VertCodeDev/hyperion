package dev.vertcode.hyperion.blueprint.data;

import java.util.Locale;
import java.util.Optional;

/**
 * The six faces of a cube, named as Blockbench and Minecraft name them.
 */
public enum FaceSide {

    NORTH,
    EAST,
    SOUTH,
    WEST,
    UP,
    DOWN;

    /**
     * Return the name Blockbench writes for this face.
     *
     * @return the lower-case name
     */
    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Find the face Blockbench writes under a name.
     *
     * @param key the lower-case name
     * @return the face, or empty for a name that is not a face
     */
    public static Optional<FaceSide> fromKey(final String key) {
        for (FaceSide side : values()) {
            if (side.key().equals(key)) {
                return Optional.of(side);
            }
        }

        return Optional.empty();
    }
}
