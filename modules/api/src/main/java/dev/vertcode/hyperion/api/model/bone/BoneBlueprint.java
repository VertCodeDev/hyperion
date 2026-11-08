package dev.vertcode.hyperion.api.model.bone;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.api.model.metadata.Metadata;

/**
 * A group from the Blockbench outliner: a bone that cubes, locators and other bones hang from.
 *
 * <p>Positions are in blocks, relative to the model's origin, not in Blockbench pixels.
 */
@ApiStatus.NonExtendable
public interface BoneBlueprint {

    /**
     * Return the bone's name as written in Blockbench, prefixes included.
     *
     * <p>Names are unique within a model. When a file repeats a name, the loader adds a number
     * to the later ones and logs a warning.
     *
     * @return the name
     */
    String name();

    /**
     * Return the bone this one hangs from.
     *
     * @return the parent, or empty for a root bone
     */
    Optional<BoneBlueprint> parent();

    /**
     * Return the bones that hang from this one.
     *
     * @return the children
     */
    @Unmodifiable List<BoneBlueprint> children();

    /**
     * Return the point this bone turns around.
     *
     * @return the pivot in blocks
     */
    Vector3fc pivot();

    /**
     * Return the rotation the bone has at rest.
     *
     * @return the rest rotation
     */
    Quaternionfc rotation();

    /**
     * Return whether the bone has cubes of its own to render.
     *
     * @return false for a bone that only holds children or logic
     */
    boolean hasGeometry();

    /**
     * Return whether the bone was visible in Blockbench when the model was saved.
     *
     * @return the starting visibility
     */
    boolean visibleByDefault();

    /**
     * Return the custom data stored on this group.
     *
     * @return the bone's metadata, empty when there is none
     */
    Metadata metadata();
}
