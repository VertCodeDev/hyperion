package dev.vertcode.hyperion.skin;

import java.util.ArrayList;
import java.util.List;


/**
 * The skin pixels one model part shows, in the order the pack numbered them.
 *
 * <p>The pack draws every skin pixel as a small white face. The item that shows the part carries
 * a color for each of them, read from the skin here, and a flag for each face that can be
 * absent: a pixel of the skin's outer layer, which may be see-through, or a cube made for the
 * other arm shape.
 *
 * <p>Two maps are the same only when they are the same object, so a skin can remember what it
 * worked out for a part without comparing every pixel.
 */
public final class SkinMap {

    private static final int OPAQUE = 0xFF000000;
    private static final int VISIBLE_ALPHA = 0x80;

    private final List<SkinPixel> solid;
    private final List<SkinPixel> optional;

    /**
     * Create the map of a part.
     *
     * @param solid the pixels that are always drawn; their colors come first
     * @param optional the pixels drawn only when their flag is set; their colors follow, and the
     *     flags are in the same order
     */
    public SkinMap(final List<SkinPixel> solid, final List<SkinPixel> optional) {
        this.solid = List.copyOf(solid);
        this.optional = List.copyOf(optional);
    }

    /**
     * Return the pixels that are always drawn.
     *
     * @return the pixels, in color order
     */
    public List<SkinPixel> solid() {
        return this.solid;
    }

    /**
     * Return the pixels drawn only when their flag is set.
     *
     * @return the pixels, in flag order
     */
    public List<SkinPixel> optional() {
        return this.optional;
    }

    /**
     * Return whether the part shows no skin pixels at all.
     *
     * @return true when there is nothing to color
     */
    public boolean isEmpty() {
        return this.solid.isEmpty() && this.optional.isEmpty();
    }

    /**
     * Work out the colors and flags that draw a skin on the part.
     *
     * @param skin the skin
     * @return the colors and flags for the part's item
     */
    public SkinColors colors(final HyperionSkin skin) {
        List<Integer> colors = new ArrayList<>(this.solid.size() + this.optional.size());
        for (SkinPixel pixel : this.solid) {
            colors.add(skin.pixel(pixel.x(), pixel.y()) | OPAQUE);
        }

        List<Boolean> flags = new ArrayList<>(this.optional.size());
        for (SkinPixel pixel : this.optional) {
            int argb = skin.pixel(pixel.x(), pixel.y());
            colors.add(argb | OPAQUE);
            flags.add(pixel.variant().shows(skin.slim()) && visible(pixel, argb));
        }

        return new SkinColors(colors, flags);
    }

    /**
     * Return whether a pixel shows: one of the inner layer always does, as on a real player,
     * and one of the outer layer when it is not see-through.
     *
     * @param pixel the pixel
     * @param argb its color in the skin
     * @return true when it is drawn
     */
    private static boolean visible(final SkinPixel pixel, final int argb) {
        return !SkinLayout.outer(pixel.x(), pixel.y()) || (argb >>> 24) >= VISIBLE_ALPHA;
    }
}
