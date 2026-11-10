package dev.vertcode.hyperion.pack.armor;

import java.awt.image.BufferedImage;

/**
 * How armor textures speak to the shader: the header, the alpha codes and the record layout.
 *
 * <p>Vanilla cuts out every pixel with alpha under 0.1, so anything stored at alpha 1 to 8 is
 * invisible when the shader is not there, on Iris or OptiFine, or on a client of another version.
 * That is how a texture carries data and the 3D model's own pixels without ever showing them.
 *
 * <ul>
 *   <li>Pixel (0, 0) is the magic: {@code H}, {@code Y}, {@code P} at alpha 3. On the flat layer
 *       it sits in a corner of the 64 by 32 layout vanilla never draws.</li>
 *   <li>Pixel (1, 0): version, kind (flat or 3D), the mask of limbs drawn in 3D, and the growth
 *       of the boxes in 64ths of a pixel.</li>
 *   <li>3D layers: row 1 + limb * 2 + side holds the face drawn on that limb's front (side 0) or
 *       back (side 1). Each corner takes five pixels, X, Y, Z, U, V, as 24-bit numbers, and a
 *       sixth pixel holds the face's normal. An empty row starts at alpha 0.</li>
 *   <li>3D layers: the model's textures, from column {@link #ATLAS_X}, with alpha 0 for a hole,
 *       1 for a plain pixel, 2 tinted, 3 glowing, 4 both.</li>
 *   <li>Flat layers: a pixel at alpha 254 glows.</li>
 * </ul>
 */
final class ArmorShaderData {

    /**
     * The layout version the shader reads.
     */
    static final int VERSION = 1;

    /**
     * A flat layer, drawn as vanilla draws it, hidden near the camera where 3D shows.
     */
    static final int KIND_FLAT = 0;

    /**
     * A layer that carries 3D faces.
     */
    static final int KIND_GEOMETRY = 1;

    /**
     * The magic's red, green and blue: {@code H}, {@code Y}, {@code P}.
     */
    static final int[] MAGIC = {72, 89, 80};

    /**
     * The alpha of the magic and the header.
     */
    static final int HEADER_ALPHA = 3;

    /**
     * The alpha of a record pixel.
     */
    static final int DATA_ALPHA = 5;

    /**
     * The alpha of a plain 3D model pixel.
     */
    static final int PLAIN = 1;

    /**
     * Added to {@link #PLAIN} for a tinted pixel.
     */
    static final int TINT = 1;

    /**
     * Added to {@link #PLAIN} for a glowing pixel.
     */
    static final int GLOW = 2;

    /**
     * The alpha of a glowing pixel on a flat layer.
     */
    static final int FLAT_GLOW_ALPHA = 254;

    /**
     * Pixels per corner: X, Y, Z, U and V.
     */
    static final int CORNER_PIXELS = 5;

    /**
     * Pixels per record: four corners and the normal.
     */
    static final int RECORD_PIXELS = 4 * CORNER_PIXELS + 1;

    /**
     * The column the model's textures start at.
     */
    static final int ATLAS_X = 32;

    /**
     * The rows the header and records take: a header row and two per limb.
     */
    static final int RECORD_ROWS = 1 + 2 * 5;

    /**
     * Positions are stored in 256ths of a pixel around this middle.
     */
    static final int POSITION_SCALE = 256;

    /**
     * UVs are stored in 64ths of a texture pixel.
     */
    static final int UV_SCALE = 64;

    /**
     * The middle of a 24-bit number, which stands for zero.
     */
    static final int MIDDLE = 1 << 23;

    /**
     * The largest 24-bit number.
     */
    static final int MAX = (1 << 24) - 1;

    /**
     * Box growth is stored in 64ths of a pixel.
     */
    static final int INFLATE_SCALE = 64;

    private ArmorShaderData() {
    }

    /**
     * Write the magic and the header into an image.
     *
     * @param image the image
     * @param kind {@link #KIND_FLAT} or {@link #KIND_GEOMETRY}
     * @param parts the mask of limbs drawn in 3D
     * @param inflate how far vanilla grows the boxes, in pixels
     */
    static void header(final BufferedImage image, final int kind, final int parts, final float inflate) {
        image.setRGB(0, 0, ArmorImages.argb(MAGIC[0], MAGIC[1], MAGIC[2], HEADER_ALPHA));
        image.setRGB(1, 0, ArmorImages.argb(VERSION, kind, parts, HEADER_ALPHA));
        image.setRGB(2, 0, ArmorImages.argb(0, 0, Math.round(inflate * INFLATE_SCALE), HEADER_ALPHA));
    }

    /**
     * Pack a 24-bit number into a record pixel.
     *
     * @param value the number, clamped to 0 to {@link #MAX}
     * @return the pixel
     */
    static int pixel(final int value) {
        int clamped = Math.max(0, Math.min(MAX, value));

        return ArmorImages.argb(clamped >> 16, clamped >> 8, clamped, DATA_ALPHA);
    }

    /**
     * Read a 24-bit number back from a record pixel.
     *
     * @param pixel the pixel
     * @return the number
     */
    static int value(final int pixel) {
        return pixel & 0xFFFFFF;
    }

    /**
     * Store a position, in pixels.
     *
     * @param pixels the position
     * @return the 24-bit number
     */
    static int position(final float pixels) {
        return Math.round(pixels * POSITION_SCALE) + MIDDLE;
    }

    /**
     * Store a UV, in texture pixels.
     *
     * @param pixels the UV
     * @return the 24-bit number
     */
    static int uv(final float pixels) {
        return Math.round(pixels * UV_SCALE);
    }
}
