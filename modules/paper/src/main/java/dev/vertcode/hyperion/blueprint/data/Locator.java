package dev.vertcode.hyperion.blueprint.data;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.api.model.metadata.Metadata;

/**
 * A locator or null object from a Blockbench file: a named point with no geometry.
 *
 * @param uuid the id Blockbench gave the locator
 * @param name the locator's name
 * @param position the position, in pixels
 * @param rotation the rotation in degrees around X, Y and Z, applied in ZYX order
 * @param metadata the fields Blockbench itself does not use
 */
public record Locator(String uuid, String name, Vector3fc position, Vector3fc rotation, Metadata metadata) {

    /**
     * Copy every vector, so a locator never changes after it is read.
     */
    public Locator {
        position = new Vector3f(position);
        rotation = new Vector3f(rotation);
    }
}
