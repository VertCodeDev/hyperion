package dev.vertcode.hyperion.blueprint;

import java.util.Optional;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.model.locator.LocatorBlueprint;
import dev.vertcode.hyperion.api.model.metadata.Metadata;

/**
 * A locator of a loaded blueprint.
 *
 * @param name the unique name
 * @param parentBone the bone it hangs from, or null at the top of the outliner
 * @param position the position in blocks, relative to the parent bone's pivot (or the model's
 *     origin without a parent), before the parent's rotation is applied, as Blockbench nests it
 * @param rotation the rotation relative to the parent
 * @param metadata the custom data on the locator
 */
public record BlueprintLocator(
        String name,
        @Nullable BoneBlueprint parentBone,
        Vector3fc position,
        Quaternionfc rotation,
        Metadata metadata
) implements LocatorBlueprint {

    /**
     * Copy the position and rotation, so a locator never changes after it is built.
     */
    public BlueprintLocator {
        position = new Vector3f(position);
        rotation = new Quaternionf(rotation);
    }

    @Override
    public Optional<BoneBlueprint> parent() {
        return Optional.ofNullable(this.parentBone);
    }
}
