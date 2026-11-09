package dev.vertcode.hyperion.api.model;

import java.util.Set;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.bone.BoneTransform;
import dev.vertcode.hyperion.api.model.hitbox.HitboxMode;
import dev.vertcode.hyperion.api.model.view.ViewerPolicy;

/**
 * Settings for a model, applied before anyone sees it.
 *
 * <p>Everything here can also be changed on the live model. Setting it here means the first
 * packets a viewer gets are already right: no pop-in at the wrong size, and no player outside the
 * viewer policy sees even one frame.
 *
 * <pre>{@code
 * blueprint.attach(zombie, options -> options.scale(2f).viewDistance(64).animation("spawn"));
 * }</pre>
 */
@ApiStatus.NonExtendable
public interface SpawnOptions {

    /**
     * Scale the whole model.
     *
     * @param scale the scale, above 0; 1 by default
     * @return these options
     * @throws IllegalArgumentException if the scale is not above 0
     */
    SpawnOptions scale(float scale);

    /**
     * Offset, turn or stretch the whole model on top of its anchor.
     *
     * @param transform the root transform; identity by default
     * @return these options
     */
    SpawnOptions transform(BoneTransform transform);

    /**
     * Set how far away, in blocks, players can see the model.
     *
     * @param blocks the view distance, above 0; the config value by default
     * @return these options
     * @throws IllegalArgumentException if the distance is not above 0
     */
    SpawnOptions viewDistance(int blocks);

    /**
     * Set who may see the model.
     *
     * @param policy the viewer policy; {@link ViewerPolicy#everyone()} by default
     * @return these options
     */
    SpawnOptions viewers(ViewerPolicy policy);

    /**
     * Work out the scale every frame in place of a fixed one.
     *
     * @param scaler the scaler
     * @return these options
     */
    SpawnOptions scaler(ModelScaler scaler);

    /**
     * Work out which way the model faces every frame.
     *
     * @param rotator the rotator; {@link ModelRotator#anchor()} by default
     * @return these options
     */
    SpawnOptions rotator(ModelRotator rotator);

    /**
     * Set over how many ticks viewers' clients smooth each move between frames.
     *
     * @param ticks the interpolation in ticks, 0 or more; the config value by default
     * @return these options
     * @throws IllegalArgumentException if the ticks are below 0
     */
    SpawnOptions interpolation(int ticks);

    /**
     * Choose which parts of the entity carrying the model are hidden. Ignored for a model on its
     * own.
     *
     * @param parts the parts to hide, copied; {@link EntityPart#ALL} by default
     * @return these options
     */
    SpawnOptions hiddenParts(Set<EntityPart> parts);

    /**
     * Hide or show the whole entity carrying the model. Ignored for a model on its own.
     *
     * @param hidden true to hide every part; true by default
     * @return these options
     */
    SpawnOptions hideBaseEntity(boolean hidden);

    /**
     * Tint the whole model.
     *
     * @param tint the tint, or null for none
     * @return these options
     */
    SpawnOptions tint(@Nullable Color tint);

    /**
     * Give the whole model the glowing outline.
     *
     * @param glowing true to glow
     * @return these options
     */
    SpawnOptions glowing(boolean glowing);

    /**
     * Color the glowing outline.
     *
     * @param color the outline color, or null for the default
     * @return these options
     */
    SpawnOptions glowColor(@Nullable Color color);

    /**
     * Fix the light level of the whole model.
     *
     * @param brightness the light to render with, or null to follow the world
     * @return these options
     */
    SpawnOptions brightness(Display.@Nullable Brightness brightness);

    /**
     * Give the whole model the enchant glint.
     *
     * @param glint true for the glint
     * @return these options
     */
    SpawnOptions glint(boolean glint);

    /**
     * Turn the whole model to face each viewer.
     *
     * @param billboard the billboard, or null to stay as posed
     * @return these options
     */
    SpawnOptions billboard(Display.@Nullable Billboard billboard);

    /**
     * Play an animation from the first frame.
     *
     * @param name the animation's name in the blueprint
     * @return these options
     * @throws IllegalArgumentException if the blueprint has no animation by that name
     */
    SpawnOptions animation(String name);

    /**
     * Turn the automatic idle and walk animations on or off for this model. A model on an
     * entity plays its {@code idle} animation while the entity stands and {@code walk} while it
     * moves, when it has them; {@code config.yml} sets the names and the default, for every
     * entity and per entity type.
     *
     * @param enabled true to play them, false to leave every animation to the caller
     * @return these options
     */
    SpawnOptions autoAnimations(boolean enabled);

    /**
     * Choose how players hit and click this model; {@code config.yml} sets the default.
     *
     * @param mode a hitbox per hitbox bone, or one box around the whole model
     * @return these options
     */
    SpawnOptions hitboxMode(HitboxMode mode);
}
