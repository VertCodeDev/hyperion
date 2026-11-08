package dev.vertcode.hyperion.api.model;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

import dev.vertcode.hyperion.api.animation.Animator;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.api.model.bone.BoneTransform;
import dev.vertcode.hyperion.api.model.locator.Locator;
import dev.vertcode.hyperion.api.model.view.PlayerView;
import dev.vertcode.hyperion.api.model.view.Viewers;

/**
 * A blueprint brought to life in the world: on an entity or on its own.
 *
 * <p>A model exists only as packets sent to the players who can see it. The server never ticks,
 * saves or collides with it. It lives until {@link #remove()} is called, its entity is removed,
 * its blueprint is unregistered or its owning plugin disables.
 *
 * <p>Threading: {@link #teleport(Location)} and {@link #remove()} run on the region that owns
 * the model. Getters and every appearance setter are safe from any thread; setters take effect
 * on the next frame.
 */
@ApiStatus.NonExtendable
public interface Model extends Appearance<Model> {

    /**
     * Return the id of this model, unique while the server runs.
     *
     * @return the id
     */
    UUID uniqueId();

    /**
     * Return the blueprint this model shows. After a reload this is the new blueprint.
     *
     * @return the blueprint
     */
    ModelBlueprint blueprint();

    /**
     * Return what drives this model's position.
     *
     * @return the anchor
     */
    ModelAnchor anchor();

    /**
     * Return where the model's origin is, as of the last frame.
     *
     * @return a copy of the location
     */
    Location location();

    /**
     * Return every bone, parents before their children.
     *
     * @return the bones
     */
    @Unmodifiable List<Bone> bones();

    /**
     * Find a bone by its Blockbench name.
     *
     * @param name the bone's name
     * @return the bone, or empty if there is none by that name
     */
    Optional<Bone> bone(String name);

    /**
     * Return every locator.
     *
     * @return the locators
     */
    @Unmodifiable List<Locator> locators();

    /**
     * Find a locator by its Blockbench name.
     *
     * @param name the locator's name
     * @return the locator, or empty if there is none by that name
     */
    Optional<Locator> locator(String name);

    /**
     * Return the offset applied to the whole model on top of its anchor.
     *
     * @return the root transform
     */
    BoneTransform transform();

    /**
     * Offset, turn or stretch the whole model on top of its anchor.
     *
     * @param transform the root transform
     * @return this model
     */
    Model transform(BoneTransform transform);

    /**
     * Return the uniform scale of the whole model.
     *
     * @return the scale, 1 by default
     */
    float scale();

    /**
     * Scale the whole model, hitboxes and seats included.
     *
     * @param scale the scale, above 0
     * @return this model
     * @throws IllegalArgumentException if the scale is not above 0
     */
    Model scale(float scale);

    /**
     * Return whether the entity carrying this model is hidden from viewers.
     *
     * @return true when hidden; always false for a model on its own
     */
    boolean baseEntityHidden();

    /**
     * Hide or show the entity carrying this model. Has no effect on a model on its own.
     *
     * @param hidden true to hide the entity
     * @return this model
     */
    Model baseEntityHidden(boolean hidden);

    /**
     * Move a model that stands on its own.
     *
     * <p>When the target is in another region, the model hands itself over to that region.
     *
     * @param location the new origin
     * @throws IllegalStateException if the model is on an entity, is removed, or this is not
     *     the region that owns the model
     */
    void teleport(Location location);

    /**
     * Return who sees this model.
     *
     * @return the viewers
     */
    Viewers viewers();

    /**
     * Return the overrides this model has for one player, made on first use.
     *
     * @param player the viewer
     * @return the player's view
     */
    @ApiStatus.Experimental
    PlayerView view(Player player);

    /**
     * Return the animator that plays this model's animations.
     *
     * @return the animator
     */
    Animator animator();

    /**
     * Return whether this model has been removed.
     *
     * @return true once removed
     */
    boolean isRemoved();

    /**
     * Remove this model from every viewer and stop its behaviors. Does nothing when already
     * removed.
     *
     * @throws IllegalStateException if this is not the region that owns the model
     */
    void remove();
}
