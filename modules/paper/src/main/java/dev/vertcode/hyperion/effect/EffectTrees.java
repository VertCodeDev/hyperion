package dev.vertcode.hyperion.effect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;

/**
 * Turn YAML and JSON into the same plain tree, so one loader reads effects from files and from
 * models alike.
 *
 * <p>A tree is maps of text keys, lists, text, numbers and switches.
 */
final class EffectTrees {

    private EffectTrees() {
    }

    /**
     * Read a YAML section as a tree.
     *
     * @param section the section
     * @return the tree
     */
    static Map<String, Object> of(final ConfigurationSection section) {
        Map<String, Object> tree = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = value(section.get(key));
            if (value != null) {
                tree.put(key, value);
            }
        }

        return tree;
    }

    /**
     * Read a JSON object as a tree.
     *
     * @param object the object
     * @return the tree
     */
    static Map<String, Object> of(final JsonObject object) {
        Map<String, Object> tree = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            Object value = value(entry.getValue());
            if (value != null) {
                tree.put(entry.getKey(), value);
            }
        }

        return tree;
    }

    /**
     * Turn one YAML value into a tree value.
     *
     * @param value the value
     * @return the tree value, or null for nothing usable
     */
    private static @Nullable Object value(final @Nullable Object value) {
        return switch (value) {
            case ConfigurationSection section -> of(section);
            case List<?> list -> {
                List<Object> items = new ArrayList<>();
                for (Object item : list) {
                    Object converted = value(item);
                    if (converted != null) {
                        items.add(converted);
                    }
                }

                yield items;
            }
            case Map<?, ?> map -> {
                Map<String, Object> tree = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object converted = value(entry.getValue());
                    if (converted != null) {
                        tree.put(String.valueOf(entry.getKey()), converted);
                    }
                }

                yield tree;
            }
            case String text -> text;
            case Number number -> number;
            case Boolean flag -> flag;
            case null, default -> null;
        };
    }

    /**
     * Turn one JSON value into a tree value.
     *
     * @param element the value
     * @return the tree value, or null for nothing usable
     */
    private static @Nullable Object value(final JsonElement element) {
        if (element instanceof JsonObject object) {
            return of(object);
        }

        if (element instanceof JsonArray array) {
            List<Object> items = new ArrayList<>();
            for (JsonElement item : array) {
                Object converted = value(item);
                if (converted != null) {
                    items.add(converted);
                }
            }

            return items;
        }

        if (element instanceof JsonPrimitive primitive) {
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }

            return primitive.isNumber() ? primitive.getAsNumber() : primitive.getAsString();
        }

        return null;
    }
}
