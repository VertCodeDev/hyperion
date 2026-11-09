package dev.vertcode.hyperion.debug;

import java.util.Locale;
import java.util.Optional;

/**
 * One thing {@code /hy debug} can draw.
 */
public enum DebugView {

    /**
     * Each bone's pivot, its axes and its name.
     */
    BONES,

    /**
     * Hitbox outlines, or the box around the whole model.
     */
    HITBOXES,

    /**
     * Each locator, the way it faces and its name.
     */
    LOCATORS,

    /**
     * Each seat and who sits in it.
     */
    SEATS,

    /**
     * Each running effect's anchor, where its parts are, and its tick.
     */
    EFFECTS;

    private final String key = this.name().toLowerCase(Locale.ROOT);

    /**
     * Return the name the command uses.
     *
     * @return the name, such as {@code bones}
     */
    public String key() {
        return this.key;
    }

    /**
     * Find a view by the name the command uses.
     *
     * @param key the name
     * @return the view, or empty when there is none by that name
     */
    public static Optional<DebugView> fromKey(final String key) {
        for (DebugView view : values()) {
            if (view.key.equals(key)) {
                return Optional.of(view);
            }
        }

        return Optional.empty();
    }
}
