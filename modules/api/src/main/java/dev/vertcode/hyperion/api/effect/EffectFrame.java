package dev.vertcode.hyperion.api.effect;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The values one keyframe sets, such as {@code offset}, {@code radius} or {@code sound}.
 *
 * <p>A frame holds only what it sets. A part starts from its settings, and each keyframe lays its
 * values over what came before. Between two keyframes, numbers and vectors move smoothly; text and
 * switches change at the keyframe.
 *
 * <p>Values are text, numbers ({@code float}), switches ({@code boolean}) or vectors.
 *
 * @param values the values by name
 */
public record EffectFrame(Map<String, Object> values) {

    /**
     * A frame that sets nothing.
     */
    public static final EffectFrame EMPTY = new EffectFrame(Map.of());

    /**
     * Copy the values, vectors included, so a frame never changes.
     */
    public EffectFrame {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            copy.put(entry.getKey(), value instanceof Vector3fc vector ? new Vector3f(vector) : value);
        }

        values = Map.copyOf(copy);
    }

    /**
     * Start building a frame.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Lay this frame over another: this frame's values win.
     *
     * @param base the values so far
     * @return the combined frame
     */
    public EffectFrame over(final EffectFrame base) {
        if (this.values.isEmpty()) {
            return base;
        }

        Map<String, Object> combined = new LinkedHashMap<>(base.values);
        combined.putAll(this.values);

        return new EffectFrame(combined);
    }

    /**
     * Work out the frame between two others: numbers and vectors set in both move smoothly,
     * everything else keeps the first frame's value.
     *
     * @param from the frame at the start
     * @param to the frame at the end
     * @param progress how far between them, already eased
     * @return the frame in between
     */
    public static EffectFrame lerp(final EffectFrame from, final EffectFrame to, final float progress) {
        Map<String, Object> mixed = new LinkedHashMap<>(from.values);
        for (Map.Entry<String, Object> entry : to.values.entrySet()) {
            Object start = from.values.get(entry.getKey());
            Object end = entry.getValue();
            if (start instanceof Float a && end instanceof Float b) {
                mixed.put(entry.getKey(), a + (b - a) * progress);
            } else if (start instanceof Vector3fc a && end instanceof Vector3fc b) {
                mixed.put(entry.getKey(), new Vector3f(a).lerp(b, progress));
            } else if (start == null) {
                mixed.put(entry.getKey(), end);
            }
        }

        return new EffectFrame(mixed);
    }

    /**
     * Return whether the frame sets a value.
     *
     * @param key the value's name
     * @return true when set
     */
    public boolean has(final String key) {
        return this.values.containsKey(key);
    }

    /**
     * Return a value as text.
     *
     * @param key the value's name
     * @return the text, or empty when unset; numbers and switches come back as text too
     */
    public Optional<String> string(final String key) {
        Object value = this.values.get(key);

        return value == null || value instanceof Vector3fc ? Optional.empty() : Optional.of(String.valueOf(value));
    }

    /**
     * Return a value as text, or a fallback.
     *
     * @param key the value's name
     * @param fallback the value when unset
     * @return the text
     */
    public String string(final String key, final String fallback) {
        return this.string(key).orElse(fallback);
    }

    /**
     * Return a number.
     *
     * @param key the value's name
     * @param fallback the value when unset or not a number
     * @return the number
     */
    public float number(final String key, final float fallback) {
        return this.values.get(key) instanceof Float number ? number : fallback;
    }

    /**
     * Return a switch.
     *
     * @param key the value's name
     * @param fallback the value when unset or not a switch
     * @return the switch
     */
    public boolean bool(final String key, final boolean fallback) {
        return this.values.get(key) instanceof Boolean flag ? flag : fallback;
    }

    /**
     * Return a vector.
     *
     * @param key the value's name
     * @return a copy of the vector, or null when unset or not a vector
     */
    public @Nullable Vector3f vector(final String key) {
        return this.values.get(key) instanceof Vector3fc vector ? new Vector3f(vector) : null;
    }

    /**
     * Builds a frame from values.
     */
    public static final class Builder {

        private final Map<String, Object> values = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Set any value.
         *
         * @param key the value's name
         * @param value text, a {@code float}, a {@code boolean} or a vector
         * @return this builder
         * @throws IllegalArgumentException for any other kind of value
         */
        public Builder set(final String key, final Object value) {
            Object kept = value instanceof Number number && !(value instanceof Float) ? number.floatValue() : value;
            if (!(kept instanceof String || kept instanceof Float || kept instanceof Boolean || kept instanceof Vector3fc)) {
                throw new IllegalArgumentException("An effect value must be text, a number, a switch or a vector, got " + value);
            }

            this.values.put(key, kept);

            return this;
        }

        /**
         * Place the part, in blocks to the right, up and forward of where it is anchored.
         *
         * @param right blocks to the right
         * @param up blocks up
         * @param forward blocks forward
         * @return this builder
         */
        public Builder offset(final float right, final float up, final float forward) {
            return this.set("offset", new Vector3f(right, up, forward));
        }

        /**
         * Set the radius of a particle shape.
         *
         * @param radius the radius in blocks
         * @return this builder
         */
        public Builder radius(final float radius) {
            return this.set("radius", radius);
        }

        /**
         * Show or hide the part from this keyframe on.
         *
         * @param visible true to show it
         * @return this builder
         */
        public Builder visible(final boolean visible) {
            return this.set("visible", visible);
        }

        /**
         * Play a sound, such as {@code entity.player.levelup}.
         *
         * @param sound the sound's key
         * @return this builder
         */
        public Builder sound(final String sound) {
            return this.set("sound", sound);
        }

        /**
         * Set how loud a sound plays.
         *
         * @param volume the volume, 1 by default
         * @return this builder
         */
        public Builder volume(final float volume) {
            return this.set("volume", volume);
        }

        /**
         * Set how high a sound plays.
         *
         * @param pitch the pitch, 0.5 to 2, 1 by default
         * @return this builder
         */
        public Builder pitch(final float pitch) {
            return this.set("pitch", pitch);
        }

        /**
         * Build the frame.
         *
         * @return the frame
         */
        public EffectFrame build() {
            return new EffectFrame(this.values);
        }
    }
}
