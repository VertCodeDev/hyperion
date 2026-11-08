package dev.vertcode.hyperion.blueprint.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dev.vertcode.hyperion.api.model.metadata.Metadata;

/**
 * Metadata over a private copy of JSON fields, read only.
 *
 * <p>The copy is taken once and never handed out, so the metadata cannot change after it is
 * made and any thread may read it.
 */
public final class JsonMetadata implements Metadata {

    /**
     * Metadata with no fields.
     */
    public static final Metadata EMPTY = new JsonMetadata(new JsonObject());

    private final JsonObject fields;

    private JsonMetadata(final JsonObject fields) {
        this.fields = fields;
    }

    /**
     * Keep every field of a JSON object except the ones Blockbench itself uses.
     *
     * @param source the object to read; it is copied, not kept
     * @param known the fields Blockbench uses, left out
     * @return the metadata, {@link #EMPTY} when no field is left
     */
    public static Metadata of(final JsonObject source, final Set<String> known) {
        JsonObject kept = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            if (!known.contains(entry.getKey())) {
                kept.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }

        return kept.isEmpty() ? EMPTY : new JsonMetadata(kept);
    }

    @Override
    public Set<String> fields() {
        return Collections.unmodifiableSet(this.fields.keySet());
    }

    @Override
    public boolean has(final String field) {
        return this.fields.has(field);
    }

    @Override
    public Optional<String> string(final String field) {
        JsonElement element = this.fields.get(field);
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            return Optional.of(primitive.getAsString());
        }

        return Optional.empty();
    }

    @Override
    public OptionalDouble number(final String field) {
        JsonElement element = this.fields.get(field);
        if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
            return OptionalDouble.of(primitive.getAsDouble());
        }

        return OptionalDouble.empty();
    }

    @Override
    public Optional<Boolean> bool(final String field) {
        JsonElement element = this.fields.get(field);
        if (element instanceof JsonPrimitive primitive && primitive.isBoolean()) {
            return Optional.of(primitive.getAsBoolean());
        }

        return Optional.empty();
    }

    @Override
    public List<String> strings(final String field) {
        JsonElement element = this.fields.get(field);
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            return List.of(primitive.getAsString());
        }

        if (!(element instanceof JsonArray array)) {
            return List.of();
        }

        List<String> texts = new ArrayList<>(array.size());
        for (JsonElement item : array) {
            if (item instanceof JsonPrimitive primitive && primitive.isString()) {
                texts.add(primitive.getAsString());
            }
        }

        return Collections.unmodifiableList(texts);
    }

    @Override
    public Optional<Metadata> section(final String field) {
        JsonElement element = this.fields.get(field);
        if (element instanceof JsonObject object) {
            return Optional.of(new JsonMetadata(object.deepCopy()));
        }

        return Optional.empty();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof JsonMetadata metadata && this.fields.equals(metadata.fields);
    }

    @Override
    public int hashCode() {
        return this.fields.hashCode();
    }

    @Override
    public String toString() {
        return this.fields.toString();
    }
}
