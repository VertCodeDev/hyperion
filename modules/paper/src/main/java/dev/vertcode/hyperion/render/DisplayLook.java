package dev.vertcode.hyperion.render;

import java.util.Objects;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.jspecify.annotations.Nullable;

/**
 * How one display looks, apart from where it is: what a frame compares to know what to send.
 *
 * @param visible whether the display shows anything
 * @param tint the tint over the textures, or null for none
 * @param glint whether the item shimmers with the enchant glint
 * @param glowing whether the display has the glowing outline
 * @param glowColor the outline's color, or null for the default white
 * @param billboard how the display turns to face the viewer
 * @param brightness the fixed light level, or null to take the world's light
 */
public record DisplayLook(
        boolean visible,
        @Nullable Color tint,
        boolean glint,
        boolean glowing,
        @Nullable Color glowColor,
        Display.Billboard billboard,
        Display.@Nullable Brightness brightness
) {

    /**
     * Return whether the item has to be built again to go from this look to another.
     *
     * @param other the other look
     * @return true when visibility, tint or glint differ
     */
    public boolean itemDiffers(final DisplayLook other) {
        return this.visible != other.visible
                || this.glint != other.glint
                || !Objects.equals(this.tint, other.tint);
    }

    /**
     * Return whether the display's own data has to be sent to go from this look to another.
     *
     * @param other the other look
     * @return true when the outline, its color, the billboard or the light differ
     */
    public boolean styleDiffers(final DisplayLook other) {
        return this.glowing != other.glowing
                || this.billboard != other.billboard
                || !Objects.equals(this.glowColor, other.glowColor)
                || !Objects.equals(this.brightness, other.brightness);
    }
}
