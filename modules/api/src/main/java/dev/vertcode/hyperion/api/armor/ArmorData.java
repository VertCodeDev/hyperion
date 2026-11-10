package dev.vertcode.hyperion.api.armor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

/**
 * Free values a server owner gives a set in its {@code .yml}, for plugins to read.
 *
 * <p>Hyperion does nothing with them. They let one plugin drive many sets from config:
 *
 * <pre>{@code
 * data:
 *   element: ice
 *   bonus-damage: 1.5
 * }</pre>
 *
 * <p>read as {@code set.data().getDouble("bonus-damage", 0)}. Immutable.
 */
public final class ArmorData {

    private static final ArmorData EMPTY = new ArmorData(Map.of());

    private final Map<String, Object> values;

    /**
     * Create the data. Made by Hyperion only.
     *
     * @param values the values by key, copied
     */
    @ApiStatus.Internal
    public ArmorData(final Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /**
     * Return data without any values.
     *
     * @return the empty data
     */
    public static ArmorData empty() {
        return EMPTY;
    }

    /**
     * Return every key, in file order.
     *
     * @return the keys
     */
    public @Unmodifiable Set<String> keys() {
        return this.values.keySet();
    }

    /**
     * Return a value as it was read: a string, number, boolean, list or map.
     *
     * @param key the key
     * @return the value, or empty when the key is missing
     */
    public Optional<Object> get(final String key) {
        return Optional.ofNullable(this.values.get(key));
    }

    /**
     * Return a value as text.
     *
     * @param key the key
     * @param fallback what to return when the key is missing
     * @return the value as text
     */
    public String getString(final String key, final String fallback) {
        Object value = this.values.get(key);

        return value == null ? fallback : String.valueOf(value);
    }

    /**
     * Return a value as a number.
     *
     * @param key the key
     * @param fallback what to return when the key is missing or not a number
     * @return the number
     */
    public double getDouble(final String key, final double fallback) {
        Object value = this.values.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException exception) {
                return fallback;
            }
        }

        return fallback;
    }

    /**
     * Return a value as a whole number, rounded toward zero.
     *
     * @param key the key
     * @param fallback what to return when the key is missing or not a number
     * @return the number
     */
    public int getInt(final String key, final int fallback) {
        double value = this.getDouble(key, Double.NaN);

        return Double.isNaN(value) ? fallback : (int) value;
    }

    /**
     * Return a value as true or false. Only {@code true} and {@code false} count.
     *
     * @param key the key
     * @param fallback what to return when the key is missing or neither
     * @return the value
     */
    public boolean getBoolean(final String key, final boolean fallback) {
        Object value = this.values.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.equalsIgnoreCase("true")) {
                return true;
            }

            if (trimmed.equalsIgnoreCase("false")) {
                return false;
            }
        }

        return fallback;
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        return other instanceof ArmorData data && data.values.equals(this.values);
    }

    @Override
    public int hashCode() {
        return this.values.hashCode();
    }

    @Override
    public String toString() {
        return "ArmorData" + this.values;
    }
}
