package dev.vertcode.hyperion.api.effect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

/**
 * Particles and sounds on a timeline: parts, each with keyframes at ticks.
 *
 * <p>Load effects from YAML in {@code plugins/Hyperion/effects}, from a model, or build them:
 *
 * <pre>{@code
 * Effect zap = Effect.builder(Key.key("myplugin", "zap"), 20)
 *         .part(EffectPart.particle("ring").setting("particle", "END_ROD").setting("shape", "ring")
 *                 .keyframe(0, frame -> frame.radius(0.2f))
 *                 .keyframe(12, Easing.EASE_OUT, frame -> frame.radius(1.5f)))
 *         .part(EffectPart.sound("crack")
 *                 .keyframe(0, frame -> frame.sound("entity.lightning_bolt.thunder").pitch(1.6f)))
 *         .build();
 * }</pre>
 *
 * @param key the effect's key
 * @param duration how long one run lasts, in ticks
 * @param loop whether it starts over at its end until stopped
 * @param ambient whether it plays beside its owner's other effect, see {@link EffectOptions#owner}
 * @param reveal the tick it shows what it is about, 0 when it gives nothing away
 * @param hidePlayers how far around it other players are hidden from its viewers, 0 for none
 * @param viewers who sees and hears it, unless a play chooses otherwise
 * @param parameters the default value of every {@code %parameter%}
 * @param parts the parts
 */
public record Effect(
        Key key,
        int duration,
        boolean loop,
        boolean ambient,
        int reveal,
        double hidePlayers,
        EffectViewers viewers,
        Map<String, String> parameters,
        List<EffectPart> parts
) implements Keyed {

    private static final String NOT_POSITIVE = "An effect must last at least 1 tick, got %d";
    private static final String BAD_REVEAL = "The effect %s reveals at tick %d, outside its %d ticks";
    private static final String BAD_RADIUS = "The effect %s hides players within %s blocks, which is below 0";
    private static final String TWICE = "The effect %s has two parts named '%s'";

    /**
     * Check the effect and copy its collections.
     *
     * @throws IllegalArgumentException if the duration is below 1, the reveal is outside the
     *     effect, the radius is below 0 or two parts share a name
     */
    public Effect {
        if (duration < 1) {
            throw new IllegalArgumentException(String.format(NOT_POSITIVE, duration));
        }

        if (reveal < 0 || reveal > duration) {
            throw new IllegalArgumentException(String.format(BAD_REVEAL, key.asString(), reveal, duration));
        }

        if (!(hidePlayers >= 0d)) {
            throw new IllegalArgumentException(String.format(BAD_RADIUS, key.asString(), hidePlayers));
        }

        Set<String> names = new HashSet<>();
        for (EffectPart part : parts) {
            if (!names.add(part.name())) {
                throw new IllegalArgumentException(String.format(TWICE, key.asString(), part.name()));
            }
        }

        parameters = Map.copyOf(parameters);
        parts = List.copyOf(parts);
    }

    /**
     * Start building an effect that plays once, for the model's viewers.
     *
     * @param key the effect's key
     * @param duration how long it lasts, in ticks
     * @return the builder
     */
    public static Builder builder(final Key key, final int duration) {
        return new Builder(key, duration);
    }

    /**
     * Builds an effect.
     */
    public static final class Builder {

        private final Key key;
        private final int duration;
        private final Map<String, String> parameters = new LinkedHashMap<>();
        private final List<EffectPart> parts = new ArrayList<>();
        private boolean loop;
        private boolean ambient;
        private int reveal;
        private double hidePlayers;
        private EffectViewers viewers = EffectViewers.model();

        private Builder(final Key key, final int duration) {
            this.key = key;
            this.duration = duration;
        }

        /**
         * Start over at the end until stopped.
         *
         * @param loop true to loop
         * @return this builder
         */
        public Builder loop(final boolean loop) {
            this.loop = loop;

            return this;
        }

        /**
         * Let it play beside its owner's other effect, in place of taking that effect's turn.
         * Use it for small things that pile up, such as a number popping out of a block.
         *
         * @param ambient true to play beside
         * @return this builder
         * @see EffectOptions#owner
         */
        public Builder ambient(final boolean ambient) {
            this.ambient = ambient;

            return this;
        }

        /**
         * Name the tick the effect shows what it is about, such as the prize of a crate. A play's
         * {@link EffectOptions#onReveal} runs then, so chat never spoils the moment.
         *
         * @param tick the tick, from 0 to the duration
         * @return this builder
         */
        public Builder reveal(final int tick) {
            this.reveal = tick;

            return this;
        }

        /**
         * Hide the other players around the effect from its viewers while it plays, so nobody
         * stands in the middle of the moment.
         *
         * @param radius blocks around where the effect starts, 0 for none
         * @return this builder
         */
        public Builder hidePlayers(final double radius) {
            this.hidePlayers = radius;

            return this;
        }

        /**
         * Choose who sees and hears it.
         *
         * @param viewers the viewers
         * @return this builder
         */
        public Builder viewers(final EffectViewers viewers) {
            this.viewers = viewers;

            return this;
        }

        /**
         * Give a parameter a default value.
         *
         * @param name the parameter's name, written {@code %name%} in the parts
         * @param value the default value
         * @return this builder
         */
        public Builder parameter(final String name, final String value) {
            this.parameters.put(name, value);

            return this;
        }

        /**
         * Add a part.
         *
         * @param part the part's builder
         * @return this builder
         */
        public Builder part(final EffectPart.Builder part) {
            return this.part(part.build());
        }

        /**
         * Add a part.
         *
         * @param part the part
         * @return this builder
         */
        public Builder part(final EffectPart part) {
            this.parts.add(part);

            return this;
        }

        /**
         * Build the effect.
         *
         * @return the effect
         * @throws IllegalArgumentException if the duration is below 1, the reveal is outside the
         *     effect, the radius is below 0 or two parts share a name
         */
        public Effect build() {
            return new Effect(
                    this.key,
                    this.duration,
                    this.loop,
                    this.ambient,
                    this.reveal,
                    this.hidePlayers,
                    this.viewers,
                    this.parameters,
                    this.parts
            );
        }
    }
}
