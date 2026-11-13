package dev.vertcode.hyperion.api.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import net.kyori.adventure.key.Keyed;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.animation.AnimationBlueprint;
import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.model.locator.LocatorBlueprint;
import dev.vertcode.hyperion.api.model.metadata.Metadata;

/**
 * A loaded Blockbench model: its bones, locators, animations and custom data.
 *
 * <p>A blueprint never changes once loaded and is shared by every model spawned from it, so it
 * is safe to read from any thread. Reloading builds a new blueprint with a higher
 * {@link #revision()}; it does not change this one.
 *
 * <pre>{@code
 * ModelBlueprint dragon = Hyperion.models().find(Key.key("bosses", "dragon")).orElseThrow();
 * Model boss = dragon.attach(zombie, options -> options.scale(2.5f));
 * }</pre>
 */
@ApiStatus.NonExtendable
public interface ModelBlueprint extends Keyed {

    /**
     * Return the plugin that registered this blueprint.
     *
     * @return the owner, or null for a model from Hyperion's own {@code models/} folder
     */
    @Nullable Plugin owner();

    /**
     * Return how many times this key has been loaded, starting at 1.
     *
     * @return the revision
     */
    int revision();

    /**
     * Return every bone, parents before their children.
     *
     * @return the bones
     */
    @Unmodifiable List<BoneBlueprint> bones();

    /**
     * Return the bones that have no parent.
     *
     * @return the root bones
     */
    @Unmodifiable List<BoneBlueprint> rootBones();

    /**
     * Find a bone by its Blockbench name.
     *
     * @param name the bone's name, as written in Blockbench
     * @return the bone, or empty if there is none by that name
     */
    Optional<BoneBlueprint> bone(String name);

    /**
     * Return every locator and null object.
     *
     * @return the locators
     */
    @Unmodifiable List<LocatorBlueprint> locators();

    /**
     * Find a locator by its Blockbench name.
     *
     * @param name the locator's name
     * @return the locator, or empty if there is none by that name
     */
    Optional<LocatorBlueprint> locator(String name);

    /**
     * Return the names of the animations in this model.
     *
     * @return the animation names
     */
    @Unmodifiable Set<String> animations();

    /**
     * Find an animation by its name, to read its length and loop mode before playing it.
     *
     * @param name the animation's name
     * @return the animation, or empty if there is none by that name
     */
    Optional<AnimationBlueprint> animation(String name);

    /**
     * Return the custom data stored on the Blockbench project.
     *
     * @return the project's metadata, empty when there is none
     */
    Metadata metadata();

    /**
     * Return whether this is a player model: one with faces that use a texture named
     * {@code player_skin}, which are drawn with the skin a live model wears.
     *
     * @return true when the blueprint has skin faces
     */
    boolean skinned();

    /**
     * Return the box around every cube at rest, in blocks, relative to the model's origin.
     *
     * @return a copy of the bounding box
     */
    BoundingBox boundingBox();

    /**
     * Spawn a model on its own at a location.
     *
     * @param location where the model's origin goes; its yaw turns the model
     * @return the live model
     * @throws IllegalStateException if not called on the region that owns the location, or if
     *     this blueprint has been unregistered
     */
    Model spawn(Location location);

    /**
     * Spawn a model on its own at a location, with options.
     *
     * @param location where the model's origin goes; its yaw turns the model
     * @param options changes to the defaults, applied before anyone sees the model
     * @return the live model
     * @throws IllegalStateException if not called on the region that owns the location, or if
     *     this blueprint has been unregistered
     */
    Model spawn(Location location, Consumer<SpawnOptions> options);

    /**
     * Put a model on an entity. It follows the entity's position, body and head.
     *
     * <p>The entity is hidden from viewers by default. An entity may carry more than one model.
     *
     * @param entity the entity to carry the model
     * @return the live model
     * @throws IllegalStateException if not called on the entity's scheduler thread, or if this
     *     blueprint has been unregistered
     */
    Model attach(Entity entity);

    /**
     * Put a model on an entity, with options.
     *
     * @param entity the entity to carry the model
     * @param options changes to the defaults, applied before anyone sees the model
     * @return the live model
     * @throws IllegalStateException if not called on the entity's scheduler thread, or if this
     *     blueprint has been unregistered
     */
    Model attach(Entity entity, Consumer<SpawnOptions> options);
}
