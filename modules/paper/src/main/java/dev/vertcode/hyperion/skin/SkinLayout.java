package dev.vertcode.hyperion.skin;

import dev.vertcode.hyperion.api.skin.Skin;

/**
 * Knows the layout of a Minecraft skin: where the outer layer is, and how an old 64 by 32 skin
 * becomes a 64 by 64 one.
 */
public final class SkinLayout {

    private static final int SIZE = Skin.SIZE;
    private static final int LEGACY_HEIGHT = 32;
    private static final int HALF = 32;
    private static final int LIMB_ROW = 16;
    private static final int OUTER_ROW = 32;
    private static final int NEW_ROW = 48;
    private static final int LIMB_SIZE = 16;
    private static final int RIGHT_LEG_X = 0;
    private static final int RIGHT_ARM_X = 40;
    private static final int LEFT_LEG_X = 16;
    private static final int LEFT_ARM_X = 32;
    private static final int LEFT_ARM_OUTER_X = 48;
    private static final int SIDE = 4;
    private static final int TOP_FACE = 1;
    private static final int BOTTOM_FACE = 2;
    private static final int OUTER_FACE = 0;
    private static final int INNER_FACE = 2;
    // Head, body, arms, legs, in the colors of nobody in particular.
    private static final int SKIN_TONE = 0xFFC8A078;
    private static final int HAIR = 0xFF4A3424;
    private static final int SHIRT = 0xFF3C8C8C;
    private static final int TROUSERS = 0xFF3B3F78;
    private static final int HEAD_BOTTOM = 16;
    private static final int HAIR_BOTTOM = 8;
    private static final int LEG_RIGHT = 16;
    private static final int BODY_RIGHT = 40;
    private static final int SLEEVE_BOTTOM = 24;

    private SkinLayout() {
    }

    /**
     * Return whether a pixel belongs to the outer layer of a skin: the hat, the jacket, the
     * sleeves and the trouser legs, which may be see-through.
     *
     * @param x the pixel's column
     * @param y the pixel's row
     * @return true for the outer layer
     */
    public static boolean outer(final int x, final int y) {
        if (y < LIMB_ROW) {
            return x >= HALF;
        }

        if (y < OUTER_ROW) {
            return false;
        }

        if (y < NEW_ROW) {
            return true;
        }

        return x < LEFT_LEG_X || x >= LEFT_ARM_OUTER_X;
    }

    /**
     * Turn the pixels of a skin image into 64 by 64, converting the old 64 by 32 layout the way
     * the game does: the left arm and leg are the right ones mirrored.
     *
     * @param argb the image's pixels, row by row, 64 wide
     * @param height the image's height, 64 or 32
     * @return 64 by 64 pixels, row by row
     */
    public static int[] normalize(final int[] argb, final int height) {
        int[] pixels = new int[SIZE * SIZE];
        System.arraycopy(argb, 0, pixels, 0, Math.min(argb.length, pixels.length));
        if (height != LEGACY_HEIGHT) {
            return pixels;
        }

        mirrorLimb(pixels, RIGHT_LEG_X, LEFT_LEG_X);
        mirrorLimb(pixels, RIGHT_ARM_X, LEFT_ARM_X);

        return pixels;
    }

    /**
     * Paint a plain skin for when a player's own cannot be had: skin, hair, a shirt and
     * trousers, with no outer layer.
     *
     * @return 64 by 64 pixels, row by row
     */
    public static int[] plain() {
        int[] pixels = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                pixels[y * SIZE + x] = outer(x, y) ? 0 : plainColor(x, y);
            }
        }

        return pixels;
    }

    /**
     * Pick the color of one pixel of the plain skin.
     *
     * @param x the pixel's column
     * @param y the pixel's row
     * @return the color
     */
    private static int plainColor(final int x, final int y) {
        if (y < HEAD_BOTTOM) {
            return y < HAIR_BOTTOM ? HAIR : SKIN_TONE;
        }

        if (y >= NEW_ROW) {
            return x < HALF ? TROUSERS : sleeve(y - NEW_ROW + LIMB_ROW);
        }

        if (x < LEG_RIGHT) {
            return TROUSERS;
        }

        return x < BODY_RIGHT ? SHIRT : sleeve(y);
    }

    /**
     * Pick the color of an arm pixel: a short sleeve over a bare arm.
     *
     * @param y the pixel's row, as in the right arm
     * @return the color
     */
    private static int sleeve(final int y) {
        return y < SLEEVE_BOTTOM ? SHIRT : SKIN_TONE;
    }

    /**
     * Copy a limb of the old layout to its place in the new one, mirrored: each of its faces is
     * flipped, and the outer two side faces swap.
     *
     * @param pixels the skin, changed in place
     * @param fromX the column the limb starts at, in row 16
     * @param toX the column its copy starts at, in row 48
     */
    private static void mirrorLimb(final int[] pixels, final int fromX, final int toX) {
        for (int y = 0; y < LIMB_SIZE; y++) {
            boolean top = y < SIDE;
            for (int x = 0; x < LIMB_SIZE; x++) {
                int source = mirroredColumn(x, top);
                if (source < 0) {
                    continue;
                }

                pixels[(NEW_ROW + y) * SIZE + toX + x] = pixels[(LIMB_ROW + y) * SIZE + fromX + source];
            }
        }
    }

    /**
     * Return the column of the old limb that a column of its mirrored copy shows.
     *
     * @param x the column in the copy, from 0 to 15
     * @param top whether the row is one of the top and bottom faces
     * @return the column in the old limb, or -1 where the layout has nothing
     */
    private static int mirroredColumn(final int x, final boolean top) {
        int face = x / SIDE;
        int within = SIDE - 1 - x % SIDE;
        if (top) {
            // The top and the bottom face each flip in place; the rest of these rows is unused.
            return face == TOP_FACE || face == BOTTOM_FACE ? face * SIDE + within : -1;
        }

        // The four sides run outer, front, inner, back: mirrored, the outer and inner swap.
        int source = face == OUTER_FACE ? INNER_FACE : face == INNER_FACE ? OUTER_FACE : face;

        return source * SIDE + within;
    }
}
