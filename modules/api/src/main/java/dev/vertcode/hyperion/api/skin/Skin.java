package dev.vertcode.hyperion.api.skin;

import org.jetbrains.annotations.ApiStatus;

/**
 * A player skin a model can wear: 64 by 64 pixels in the layout of a Minecraft skin.
 *
 * <p>Immutable and safe to read from any thread. Get one from {@link SkinService}.
 */
@ApiStatus.NonExtendable
public interface Skin {

    /**
     * The width and height of every skin, in pixels. Older 64 by 32 skins are converted.
     */
    int SIZE = 64;

    /**
     * Return whether the skin is made for the slim arms, 3 pixels wide in place of 4.
     *
     * @return true for a slim skin
     */
    boolean slim();

    /**
     * Return one pixel of the skin.
     *
     * @param x the column, from 0 to {@link #SIZE} - 1
     * @param y the row, from 0 at the top to {@link #SIZE} - 1
     * @return the color as ARGB; a pixel outside the skin is fully transparent
     */
    int pixel(int x, int y);
}
