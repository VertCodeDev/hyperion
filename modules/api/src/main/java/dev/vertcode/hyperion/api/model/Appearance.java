package dev.vertcode.hyperion.api.model;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * How a model, a bone or one player's view of them looks: visibility, tint, glow and light.
 *
 * <p>Setters return this object so calls chain. They are safe from any thread: they record the
 * wanted look, and the model applies it on its next frame and sends only what changed.
 *
 * <p>A bone shows when both it and its model are visible. A bone glows when either it or its
 * model glows. Tint, glow color and brightness set on a bone win over the model's; null falls
 * back to the model.
 *
 * @param <S> the type setters return, so a chain keeps the concrete type
 */
@ApiStatus.NonExtendable
public interface Appearance<S extends Appearance<S>> {

    /**
     * Return whether this is visible.
     *
     * @return true when visible
     */
    boolean visible();

    /**
     * Show or hide this.
     *
     * @param visible true to show, false to hide
     * @return this object
     */
    S visible(boolean visible);

    /**
     * Return the tint multiplied over the textures.
     *
     * @return the tint, or null for none
     */
    @Nullable Color tint();

    /**
     * Tint the textures.
     *
     * @param tint the color to multiply over the textures, or null to clear it
     * @return this object
     */
    S tint(@Nullable Color tint);

    /**
     * Return whether this has the glowing outline.
     *
     * @return true when glowing
     */
    boolean glowing();

    /**
     * Turn the glowing outline on or off.
     *
     * @param glowing true to glow
     * @return this object
     */
    S glowing(boolean glowing);

    /**
     * Return the color of the glowing outline.
     *
     * @return the glow color, or null for the default
     */
    @Nullable Color glowColor();

    /**
     * Color the glowing outline.
     *
     * @param color the outline color, or null for the default
     * @return this object
     */
    S glowColor(@Nullable Color color);

    /**
     * Return the fixed light level.
     *
     * @return the brightness, or null to follow the light in the world
     */
    Display.@Nullable Brightness brightness();

    /**
     * Fix the light level, for a model that should stay lit in the dark.
     *
     * @param brightness the light to render with, or null to follow the world
     * @return this object
     */
    S brightness(Display.@Nullable Brightness brightness);
}
