package dev.vertcode.hyperion.api.model.metadata;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Custom data from a Blockbench file that Hyperion keeps rather than throws away.
 *
 * <p>Blockbench plugins, Hyperion's own included, store extra fields on the project, on groups
 * and on locators. They land here as plain values, read only, so they reach the server without
 * an api change. Safe to read from any thread.
 */
@ApiStatus.NonExtendable
public interface Metadata {

    /**
     * Return the names of every field.
     *
     * @return the field names
     */
    @Unmodifiable Set<String> fields();

    /**
     * Return whether a field is present.
     *
     * @param field the field name
     * @return true when present
     */
    boolean has(String field);

    /**
     * Read a text field.
     *
     * @param field the field name
     * @return the text, or empty when missing or not text
     */
    Optional<String> string(String field);

    /**
     * Read a number field.
     *
     * @param field the field name
     * @return the number, or empty when missing or not a number
     */
    OptionalDouble number(String field);

    /**
     * Read a true or false field.
     *
     * @param field the field name
     * @return the value, or empty when missing or not true or false
     */
    Optional<Boolean> bool(String field);

    /**
     * Read a list of text. A single text value reads as a list of one.
     *
     * @param field the field name
     * @return the texts, empty when missing
     */
    @Unmodifiable List<String> strings(String field);

    /**
     * Read a nested object.
     *
     * @param field the field name
     * @return the nested metadata, or empty when missing or not an object
     */
    Optional<Metadata> section(String field);
}
