package dev.vertcode.hyperion.blueprint.bbmodel;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Lenient reads from Blockbench JSON: a missing or malformed field gives the fallback instead
 * of an exception, because one odd field should cost a warning, not the whole model.
 */
final class JsonFields {

    private JsonFields() {
    }

    /**
     * Read a text field. Numbers and booleans read as their text.
     *
     * @param object the object to read from
     * @param field the field name
     * @param fallback the value when the field is missing or not a primitive
     * @return the text
     */
    static String string(final JsonObject object, final String field, final String fallback) {
        JsonElement element = object.get(field);
        if (element instanceof JsonPrimitive primitive) {
            return primitive.getAsString();
        }

        return fallback;
    }

    /**
     * Read a number field. A number written as text reads too.
     *
     * @param object the object to read from
     * @param field the field name
     * @param fallback the value when the field is missing or not a number
     * @return the number
     */
    static float number(final JsonObject object, final String field, final float fallback) {
        return number(object.get(field), fallback);
    }

    /**
     * Read a true or false field.
     *
     * @param object the object to read from
     * @param field the field name
     * @param fallback the value when the field is missing or not a boolean
     * @return the value
     */
    static boolean bool(final JsonObject object, final String field, final boolean fallback) {
        JsonElement element = object.get(field);
        if (element instanceof JsonPrimitive primitive && primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }

        return fallback;
    }

    /**
     * Read a three number array such as {@code [1, 2, 3]}.
     *
     * @param object the object to read from
     * @param field the field name
     * @return the vector, 0 for every missing or malformed axis
     */
    static Vector3f vector(final JsonObject object, final String field) {
        Vector3f vector = new Vector3f();
        if (!(object.get(field) instanceof JsonArray array)) {
            return vector;
        }

        for (int i = 0; i < 3 && i < array.size(); i++) {
            vector.setComponent(i, number(array.get(i), 0f));
        }

        return vector;
    }

    /**
     * Read an array of objects, skipping anything that is not an object.
     *
     * @param object the object to read from
     * @param field the field name
     * @return the objects, empty when the field is missing
     */
    static List<JsonObject> objects(final JsonObject object, final String field) {
        List<JsonObject> objects = new ArrayList<>();
        if (!(object.get(field) instanceof JsonArray array)) {
            return objects;
        }

        for (JsonElement element : array) {
            if (element instanceof JsonObject item) {
                objects.add(item);
            }
        }

        return objects;
    }

    /**
     * Read a number from any JSON element.
     *
     * @param element the element, or null when the field is missing
     * @param fallback the value when the element is not a number
     * @return the number
     */
    static float number(final @Nullable JsonElement element, final float fallback) {
        if (!(element instanceof JsonPrimitive primitive) || primitive.isBoolean()) {
            return fallback;
        }

        try {
            return primitive.getAsFloat();
        } catch (NumberFormatException _) {
            return fallback;
        }
    }
}
