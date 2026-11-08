package dev.vertcode.hyperion.blueprint.data;

import java.util.Optional;

/**
 * How an animation moves from one keyframe to the next.
 */
public enum Interpolation {

    /**
     * A straight line.
     */
    LINEAR("linear"),

    /**
     * A smooth curve through the neighboring keyframes, "smooth" in Blockbench.
     */
    CATMULLROM("catmullrom"),

    /**
     * Jump at the next keyframe.
     */
    STEP("step"),

    /**
     * A curve shaped by the keyframe's handles.
     */
    BEZIER("bezier");

    private final String key;

    Interpolation(final String key) {
        this.key = key;
    }

    /**
     * Find the interpolation Blockbench writes under a name.
     *
     * @param key the name in the file
     * @return the interpolation, or empty for an unknown name
     */
    public static Optional<Interpolation> fromKey(final String key) {
        for (Interpolation interpolation : values()) {
            if (interpolation.key.equals(key)) {
                return Optional.of(interpolation);
            }
        }

        return Optional.empty();
    }
}
