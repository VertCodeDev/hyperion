package dev.vertcode.hyperion.api.effect;

import java.util.Locale;
import java.util.Optional;

/**
 * How a value moves from one keyframe to the next.
 *
 * <p>The easing sits on the keyframe a value moves towards. The back curves go a little past
 * their target before they settle.
 */
public enum Easing {

    /**
     * At an even pace.
     */
    LINEAR,

    /**
     * Slow at first, fast at the end.
     */
    EASE_IN,

    /**
     * Fast at first, slow at the end.
     */
    EASE_OUT,

    /**
     * Slow at both ends.
     */
    EASE_IN_OUT,

    /**
     * Pulls back a little before it moves.
     */
    EASE_IN_BACK,

    /**
     * Goes a little past its target, then settles.
     */
    EASE_OUT_BACK,

    /**
     * Bounces at its target.
     */
    EASE_OUT_BOUNCE;

    private static final float BACK = 1.70158f;
    private static final float BACK_OUT = BACK + 1f;
    private static final float BOUNCE = 7.5625f;
    private static final float BOUNCE_STEP = 2.75f;

    /**
     * Find an easing by the name it has in a file, such as {@code ease-out}.
     *
     * @param key the name, any case, with dashes or underscores
     * @return the easing, or empty for an unknown name
     */
    public static Optional<Easing> fromKey(final String key) {
        String name = key.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (Easing easing : values()) {
            if (easing.name().equals(name)) {
                return Optional.of(easing);
            }
        }

        return Optional.empty();
    }

    /**
     * Return the name this easing has in a file.
     *
     * @return the name, such as {@code ease-out}
     */
    public String key() {
        return this.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * Ease a point in time between two keyframes.
     *
     * @param progress how far between them, 0 to 1
     * @return the eased progress, which the back curves take below 0 or above 1
     */
    public float apply(final float progress) {
        float t = Math.clamp(progress, 0f, 1f);

        return switch (this) {
            case LINEAR -> t;
            case EASE_IN -> t * t;
            case EASE_OUT -> 1f - (1f - t) * (1f - t);
            case EASE_IN_OUT -> t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
            case EASE_IN_BACK -> BACK_OUT * t * t * t - BACK * t * t;
            case EASE_OUT_BACK -> 1f + BACK_OUT * (float) Math.pow(t - 1f, 3) + BACK * (float) Math.pow(t - 1f, 2);
            case EASE_OUT_BOUNCE -> bounce(t);
        };
    }

    /**
     * Work out the bounce curve.
     *
     * @param t the progress, 0 to 1
     * @return the eased progress
     */
    private static float bounce(final float t) {
        if (t < 1f / BOUNCE_STEP) {
            return BOUNCE * t * t;
        }

        if (t < 2f / BOUNCE_STEP) {
            float shifted = t - 1.5f / BOUNCE_STEP;
            return BOUNCE * shifted * shifted + 0.75f;
        }

        if (t < 2.5f / BOUNCE_STEP) {
            float shifted = t - 2.25f / BOUNCE_STEP;
            return BOUNCE * shifted * shifted + 0.9375f;
        }

        float shifted = t - 2.625f / BOUNCE_STEP;

        return BOUNCE * shifted * shifted + 0.984375f;
    }
}
