package dev.vertcode.hyperion.blueprint.data;

import java.util.List;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.api.model.metadata.Metadata;

/**
 * A group from the Blockbench outliner, before it becomes a bone.
 *
 * @param uuid the id Blockbench gave the group
 * @param name the group's name
 * @param origin the pivot, in pixels
 * @param rotation the rest rotation in degrees around X, Y and Z, applied in ZYX order
 * @param visible whether the group was visible when the model was saved
 * @param children the ids of the groups, cubes and locators inside, in outliner order
 * @param metadata the fields Blockbench itself does not use
 */
public record Group(
        String uuid,
        String name,
        Vector3fc origin,
        Vector3fc rotation,
        boolean visible,
        List<String> children,
        Metadata metadata
) {

    /**
     * Copy every vector and the child list, so a group never changes after it is read.
     */
    public Group {
        origin = new Vector3f(origin);
        rotation = new Vector3f(rotation);
        children = List.copyOf(children);
    }
}
