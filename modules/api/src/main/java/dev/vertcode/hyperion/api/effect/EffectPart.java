package dev.vertcode.hyperion.api.effect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.kyori.adventure.key.Key;

/**
 * One track of an effect: a particle emitter, a sound, a burst, an event, or a type a plugin
 * registered, with its settings and keyframes.
 *
 * <p>The settings are where the part starts; each keyframe lays its values over them. Two
 * settings mean the same for every type: {@code at}, where the part is ({@code anchor},
 * {@code bone:<name>}, {@code locator:<name>} or another part's name), and {@code offset}.
 *
 * @param name the part's name, unique in its effect
 * @param type the part type's key, such as {@link #PARTICLE}
 * @param settings the values the part starts with
 * @param keyframes the keyframes, by tick
 */
public record EffectPart(String name, Key type, EffectFrame settings, List<EffectKeyframe> keyframes) {

    /**
     * A particle emitter: a point, ring, helix, sphere or line of particles, sent every few ticks
     * while visible.
     */
    public static final Key PARTICLE = Key.key("hyperion", "particle");

    /**
     * Sounds, one per keyframe.
     */
    public static final Key SOUND = Key.key("hyperion", "sound");

    /**
     * Bursts of particles, one per keyframe.
     */
    public static final Key BURST = Key.key("hyperion", "burst");

    /**
     * Named moments, one per keyframe, that fire {@code EffectKeyframeEvent} for plugins.
     */
    public static final Key EVENT = Key.key("hyperion", "event");

    /**
     * Sort the keyframes by tick and copy them.
     */
    public EffectPart {
        List<EffectKeyframe> sorted = new ArrayList<>(keyframes);
        sorted.sort(Comparator.comparingInt(EffectKeyframe::tick));
        keyframes = List.copyOf(sorted);
    }

    /**
     * Start building a particle emitter.
     *
     * @param name the part's name
     * @return the builder
     */
    public static Builder particle(final String name) {
        return of(name, PARTICLE);
    }

    /**
     * Start building a part that plays sounds.
     *
     * @param name the part's name
     * @return the builder
     */
    public static Builder sound(final String name) {
        return of(name, SOUND);
    }

    /**
     * Start building a part that sends bursts of particles.
     *
     * @param name the part's name
     * @return the builder
     */
    public static Builder burst(final String name) {
        return of(name, BURST);
    }

    /**
     * Start building a part that fires events.
     *
     * @param name the part's name
     * @return the builder
     */
    public static Builder event(final String name) {
        return of(name, EVENT);
    }

    /**
     * Start building a part of any type.
     *
     * @param name the part's name
     * @param type the part type's key
     * @return the builder
     */
    public static Builder of(final String name, final Key type) {
        return new Builder(name, type);
    }

    /**
     * Builds a part.
     */
    public static final class Builder {

        private final String name;
        private final Key type;
        private final Map<String, Object> settings = new LinkedHashMap<>();
        private final List<EffectKeyframe> keyframes = new ArrayList<>();

        private Builder(final String name, final Key type) {
            this.name = name;
            this.type = type;
        }

        /**
         * Set a value the part starts with, such as {@code particle} or {@code shape}.
         *
         * @param key the value's name
         * @param value text, a number, a switch or a vector
         * @return this builder
         */
        public Builder setting(final String key, final Object value) {
            this.settings.put(key, value);

            return this;
        }

        /**
         * Add a keyframe that eases linearly.
         *
         * @param tick the tick
         * @param frame sets the keyframe's values
         * @return this builder
         */
        public Builder keyframe(final int tick, final Consumer<EffectFrame.Builder> frame) {
            return this.keyframe(tick, Easing.LINEAR, frame);
        }

        /**
         * Add a keyframe.
         *
         * @param tick the tick
         * @param easing how values move towards it
         * @param frame sets the keyframe's values
         * @return this builder
         */
        public Builder keyframe(final int tick, final Easing easing, final Consumer<EffectFrame.Builder> frame) {
            EffectFrame.Builder values = EffectFrame.builder();
            frame.accept(values);
            this.keyframes.add(new EffectKeyframe(tick, values.build(), easing));

            return this;
        }

        /**
         * Build the part.
         *
         * @return the part
         */
        public EffectPart build() {
            EffectFrame.Builder values = EffectFrame.builder();
            this.settings.forEach(values::set);

            return new EffectPart(this.name, this.type, values.build(), this.keyframes);
        }
    }
}
