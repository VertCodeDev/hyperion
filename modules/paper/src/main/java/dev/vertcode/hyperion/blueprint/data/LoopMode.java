package dev.vertcode.hyperion.blueprint.data;

import java.util.Optional;

/**
 * What an animation does when it reaches its end.
 */
public enum LoopMode {

    /**
     * Stop and return to the rest pose.
     */
    ONCE("once"),

    /**
     * Start again from the first frame.
     */
    LOOP("loop"),

    /**
     * Stop and keep the last frame.
     */
    HOLD("hold");

    private final String key;

    LoopMode(final String key) {
        this.key = key;
    }

    /**
     * Find the loop mode Blockbench writes under a name.
     *
     * @param key the name in the file
     * @return the loop mode, or empty for an unknown name
     */
    public static Optional<LoopMode> fromKey(final String key) {
        for (LoopMode mode : values()) {
            if (mode.key.equals(key)) {
                return Optional.of(mode);
            }
        }

        return Optional.empty();
    }
}
