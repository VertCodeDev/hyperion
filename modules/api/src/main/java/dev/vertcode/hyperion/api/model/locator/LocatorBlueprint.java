package dev.vertcode.hyperion.api.model.locator;

import java.util.Optional;

import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.model.metadata.Metadata;

/**
 * A Blockbench locator or null object: a named point that renders nothing.
 *
 * <p>Locators mark where things happen: where a particle spawns, a sound plays, a projectile
 * leaves or a leash ties on. Animation effects point at them by name.
 */
@ApiStatus.NonExtendable
public interface LocatorBlueprint {

    /**
     * Return the locator's name as written in Blockbench.
     *
     * @return the name
     */
    String name();

    /**
     * Return the bone the locator moves with.
     *
     * @return the parent bone, or empty when it sits at the model's root
     */
    Optional<BoneBlueprint> parent();

    /**
     * Return the locator's position relative to its parent.
     *
     * @return the position in blocks
     */
    Vector3fc position();

    /**
     * Return the locator's rotation relative to its parent.
     *
     * @return the rotation
     */
    Quaternionfc rotation();

    /**
     * Return the custom data stored on this locator.
     *
     * @return the locator's metadata, empty when there is none
     */
    Metadata metadata();
}
