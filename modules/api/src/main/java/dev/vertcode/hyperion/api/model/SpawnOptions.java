package dev.vertcode.hyperion.api.model;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.bone.BoneTransform;
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
     * Hide or show the entity carrying the model. Ignored for a model on its own.
     *
     * @param hidden true to hide the entity; true by default
     * @return these options
     */
    SpawnOptions baseEntityHidden(boolean hidden);

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
     * Play an animation from the first frame.
     *
     * @param name the animation's name in the blueprint
     * @return these options
     * @throws IllegalArgumentException if the blueprint has no animation by that name
     */
    SpawnOptions animation(String name);
}
