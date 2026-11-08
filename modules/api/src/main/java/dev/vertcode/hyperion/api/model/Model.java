package dev.vertcode.hyperion.api.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

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
     * Return the bones that pass a test, parents first. Handy to change many bones at once:
     *
     * <pre>{@code
     * model.bones(bone -> bone.name().startsWith("wing")).forEach(wing -> wing.tint(Color.RED));
     * }</pre>
     *
     * @param filter the test a bone must pass
     * @return the bones that pass
     */
    @Unmodifiable List<Bone> bones(Predicate<Bone> filter);

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
     * Return the uniform scale of the whole model, as of the last frame.
     *
     * @return the scale, 1 by default
     */
    float scale();

    /**
     * Scale the whole model, hitboxes and seats included. Same as
     * {@code scaler(ModelScaler.fixed(scale))}.
     *
     * @param scale the scale, above 0
     * @return this model
     * @throws IllegalArgumentException if the scale is not above 0
     */
    Model scale(float scale);

    /**
     * Return what works out the model's scale every frame.
     *
     * @return the scaler
     */
    ModelScaler scaler();

    /**
     * Work out the model's scale every frame, so it can follow the game.
     *
     * @param scaler the scaler
     * @return this model
     */
    Model scaler(ModelScaler scaler);

    /**
     * Return which way the whole model faces, as of the last frame.
     *
     * @return the rotation
     */
    ModelRotation rotation();

    /**
     * Return what works out which way the model faces every frame.
     *
     * @return the rotator, {@link ModelRotator#anchor()} by default
     */
    ModelRotator rotator();

    /**
     * Work out which way the model faces every frame, starting from what its anchor says.
     *
     * @param rotator the rotator
     * @return this model
     */
    Model rotator(ModelRotator rotator);

    /**
     * Return over how many ticks viewers' clients smooth each move between frames.
     *
     * @return the interpolation in ticks
     */
    int interpolation();

    /**
     * Set over how many ticks viewers' clients smooth each move between frames. Higher is
     * smoother but lags behind; 0 snaps every frame.
     *
     * @param ticks the interpolation in ticks, 0 or more; the config value by default
     * @return this model
     * @throws IllegalArgumentException if the ticks are below 0
     */
    Model interpolation(int ticks);

    /**
     * Return which parts of the entity carrying this model are hidden from viewers.
     *
     * @return the hidden parts; always empty for a model on its own
     */
    @Unmodifiable Set<EntityPart> hiddenParts();

    /**
     * Choose which parts of the entity carrying this model are hidden from viewers. Has no effect
     * on a model on its own.
     *
     * @param parts the parts to hide, copied; {@link EntityPart#NONE} shows the whole entity
     * @return this model
     */
    Model hiddenParts(Set<EntityPart> parts);

    /**
     * Hide or show the whole entity carrying this model. Same as {@code hiddenParts(ALL)} or
     * {@code hiddenParts(NONE)}.
     *
     * @param hidden true to hide every part
     * @return this model
     */
    Model hideBaseEntity(boolean hidden);

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
     * Return whether the model plays its idle and walk animations by itself.
     *
     * @return true when it does; always false for a model on its own
     */
    boolean autoAnimations();

    /**
     * Turn the automatic idle and walk animations on or off. Turning them off stops the one
     * playing. Has no effect on a model on its own, which never moves by itself.
     *
     * @param enabled true to play them
     * @return this model
     */
    Model autoAnimations(boolean enabled);

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
