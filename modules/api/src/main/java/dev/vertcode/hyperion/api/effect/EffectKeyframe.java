package dev.vertcode.hyperion.api.effect;

/**
 * The values a part has at one tick of an effect.
 *
 * @param tick the tick, counted from the effect's start
 * @param frame the values it sets
 * @param easing how values move towards this keyframe from the one before
 */
public record EffectKeyframe(int tick, EffectFrame frame, Easing easing) {

    private static final String NEGATIVE = "A keyframe's tick must be 0 or more, got %d";

    /**
     * Check the tick.
     *
     * @throws IllegalArgumentException if the tick is below 0
     */
    public EffectKeyframe {
        if (tick < 0) {
            throw new IllegalArgumentException(String.format(NEGATIVE, tick));
        }
    }
}
