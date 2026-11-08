package dev.vertcode.hyperion.blueprint.data;

import java.util.Map;

/**
 * One data point of a keyframe, every value kept as the text Blockbench saved.
 *
 * <p>A transform keyframe has {@code x}, {@code y} and {@code z}; each is a number or a Molang
 * expression such as {@code math.sin(query.anim_time * 90) * 10}. An effect keyframe has its
 * own fields, such as {@code effect} and {@code locator} for a particle. Turning the text into
 * numbers and running Molang belongs to animation playback, not to loading.
 *
 * @param values the fields of the data point
 */
public record KeyframePoint(Map<String, String> values) {

    /**
     * Copy the fields, so a data point never changes after it is read.
     */
    public KeyframePoint {
        values = Map.copyOf(values);
    }

    /**
     * Read one field.
     *
     * @param field the field name, such as {@code x}
     * @param fallback the text to return when the field is missing
     * @return the field's text, or the fallback
     */
    public String value(final String field, final String fallback) {
        return this.values.getOrDefault(field, fallback);
    }
}
