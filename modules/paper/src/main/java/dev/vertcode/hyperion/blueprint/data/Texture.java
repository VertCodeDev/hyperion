package dev.vertcode.hyperion.blueprint.data;

import java.util.Arrays;
import java.util.List;

/**
 * A texture from a Blockbench file: the PNG itself, its size and its animation.
 *
 * <p>A class rather than a record because it holds an array: the PNG is copied in and out so
 * nothing can change it, and two textures compare by their bytes.
 */
public final class Texture {

    private static final String DESCRIPTION = "Texture[name=%s, size=%dx%d, frames=%d]";

    private final String name;
    private final int width;
    private final int height;
    private final int uvWidth;
    private final int uvHeight;
    private final byte[] png;
    private final int frameTime;
    private final boolean frameInterpolate;
    private final List<Integer> frameOrder;

    /**
     * Create a texture.
     *
     * @param name the texture's name, usually its file name
     * @param width the image width in pixels
     * @param height the image height in pixels
     * @param uvWidth the width the cube UVs are measured against
     * @param uvHeight the height the cube UVs are measured against
     * @param png the PNG bytes, copied
     * @param frameTime the ticks each animation frame shows, 1 or more
     * @param frameInterpolate whether the client blends between frames
     * @param frameOrder the frame indices to play, empty to play them in order
     */
    public Texture(
            final String name,
            final int width,
            final int height,
            final int uvWidth,
            final int uvHeight,
            final byte[] png,
            final int frameTime,
            final boolean frameInterpolate,
            final List<Integer> frameOrder
    ) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.uvWidth = uvWidth;
        this.uvHeight = uvHeight;
        this.png = png.clone();
        this.frameTime = frameTime;
        this.frameInterpolate = frameInterpolate;
        this.frameOrder = List.copyOf(frameOrder);
    }

    /**
     * Return the texture's name.
     *
     * @return the name, usually its file name
     */
    public String name() {
        return this.name;
    }

    /**
     * Return the image width.
     *
     * @return the width in pixels
     */
    public int width() {
        return this.width;
    }

    /**
     * Return the image height. Taller than the width for an animated texture.
     *
     * @return the height in pixels
     */
    public int height() {
        return this.height;
    }

    /**
     * Return the width the cube UVs are measured against.
     *
     * @return the UV width
     */
    public int uvWidth() {
        return this.uvWidth;
    }

    /**
     * Return the height the cube UVs are measured against.
     *
     * @return the UV height
     */
    public int uvHeight() {
        return this.uvHeight;
    }

    /**
     * Return the PNG.
     *
     * @return a copy of the PNG bytes
     */
    public byte[] png() {
        return this.png.clone();
    }

    /**
     * Return how many frames the image holds, stacked from top to bottom.
     *
     * <p>A frame has the shape of the UV size, so a 16 by 32 image with a 16 by 32 UV size is one
     * tall frame, and a 16 by 48 image with a 16 by 16 UV size is three.
     *
     * @return the frame count, 1 for a still texture
     */
    public int frames() {
        return frames(this.width, this.height, this.uvWidth, this.uvHeight);
    }

    /**
     * Work out how many frames an image holds.
     *
     * @param width the image width
     * @param height the image height
     * @param uvWidth the UV width
     * @param uvHeight the UV height of one frame
     * @return the frame count, at least 1
     */
    public static int frames(final int width, final int height, final int uvWidth, final int uvHeight) {
        if (width <= 0 || uvHeight <= 0) {
            return 1;
        }

        return Math.max(1, (height * uvWidth) / (width * uvHeight));
    }

    /**
     * Return how long each animation frame shows.
     *
     * @return the ticks per frame
     */
    public int frameTime() {
        return this.frameTime;
    }

    /**
     * Return whether the client blends between animation frames.
     *
     * @return true to blend
     */
    public boolean frameInterpolate() {
        return this.frameInterpolate;
    }

    /**
     * Return the order the animation frames play in.
     *
     * @return the frame indices, empty to play them top to bottom
     */
    public List<Integer> frameOrder() {
        return this.frameOrder;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Texture texture
                && this.name.equals(texture.name)
                && this.width == texture.width
                && this.height == texture.height
                && this.uvWidth == texture.uvWidth
                && this.uvHeight == texture.uvHeight
                && this.frameTime == texture.frameTime
                && this.frameInterpolate == texture.frameInterpolate
                && this.frameOrder.equals(texture.frameOrder)
                && Arrays.equals(this.png, texture.png);
    }

    @Override
    public int hashCode() {
        return 31 * this.name.hashCode() + Arrays.hashCode(this.png);
    }

    @Override
    public String toString() {
        return String.format(DESCRIPTION, this.name, this.width, this.height, this.frames());
    }
}
