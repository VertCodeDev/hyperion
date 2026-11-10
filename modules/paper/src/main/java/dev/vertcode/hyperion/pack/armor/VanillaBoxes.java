package dev.vertcode.hyperion.pack.armor;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.armor.ArmorPart;

/**
 * Where each side of vanilla's armor boxes sits on the 64 by 32 texture, and which way it faces.
 *
 * <p>Sides are numbered in the order vanilla builds a box: down, up, west, north, east, south.
 * North is the front of the player, west is the player's right. The shader numbers them the same,
 * from the order the faces reach it.
 */
final class VanillaBoxes {

    /**
     * The side facing down in the model's space, the top of the limb in the world.
     */
    static final int DOWN = 0;

    /**
     * The side facing up in the model's space, the bottom of the limb in the world.
     */
    static final int UP = 1;

    /**
     * The player's right side of the limb.
     */
    static final int WEST = 2;

    /**
     * The front of the limb.
     */
    static final int NORTH = 3;

    /**
     * The player's left side of the limb.
     */
    static final int EAST = 4;

    /**
     * The back of the limb.
     */
    static final int SOUTH = 5;

    private VanillaBoxes() {
    }

    /**
     * Return a side's rectangle on the texture as vanilla writes it: the U and V of the first
     * corner, then of the opposite corner. Up's V runs backwards.
     *
     * @param part the limb
     * @param side the side
     * @return {@code {u1, v1, u2, v2}} in texture pixels
     */
    static float[] rect(final ArmorPart part, final int side) {
        float u = part.u();
        float v = part.v();
        float w = part.size().x();
        float h = part.size().y();
        float d = part.size().z();

        return switch (side) {
            case DOWN -> new float[] {u + d, v, u + d + w, v + d};
            case UP -> new float[] {u + d + w, v + d, u + d + 2 * w, v};
            case WEST -> new float[] {u, v + d, u + d, v + d + h};
            case NORTH -> new float[] {u + d, v + d, u + d + w, v + d + h};
            case EAST -> new float[] {u + d + w, v + d, u + 2 * d + w, v + d + h};
            default -> new float[] {u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h};
        };
    }

    /**
     * Return the point on a side of a limb's box that a texture pixel covers.
     *
     * @param part the limb
     * @param side the side
     * @param u the pixel's U, in texture pixels
     * @param v the pixel's V
     * @param grow how far the box is grown
     * @return the point, in the limb's model space
     */
    static Vector3f point(final ArmorPart part, final int side, final float u, final float v, final float grow) {
        float[] rect = rect(part, side);
        float tu = (u - rect[0]) / (rect[2] - rect[0]);
        float tv = (v - rect[1]) / (rect[3] - rect[1]);
        Vector3fc min = part.min();
        Vector3fc size = part.size();
        float x1 = min.x() - grow;
        float x2 = min.x() + size.x() + grow;
        if (part.mirror()) {
            float swap = x1;
            x1 = x2;
            x2 = swap;
        }

        float y1 = min.y() - grow;
        float y2 = min.y() + size.y() + grow;
        float z1 = min.z() - grow;
        float z2 = min.z() + size.z() + grow;

        return switch (side) {
            case DOWN -> new Vector3f(lerp(x1, x2, tu), y1, lerp(z2, z1, tv));
            case UP -> new Vector3f(lerp(x1, x2, tu), y2, lerp(z1, z2, tv));
            case WEST -> new Vector3f(x1, lerp(y1, y2, tv), lerp(z2, z1, tu));
            case NORTH -> new Vector3f(lerp(x1, x2, tu), lerp(y1, y2, tv), z1);
            case EAST -> new Vector3f(x2, lerp(y1, y2, tv), lerp(z1, z2, tu));
            default -> new Vector3f(lerp(x2, x1, tu), lerp(y1, y2, tv), z2);
        };
    }

    /**
     * Return the way into a limb's box through one of its sides.
     *
     * @param part the limb
     * @param side the side
     * @return the unit direction
     */
    static Vector3f inward(final ArmorPart part, final int side) {
        float x = part.mirror() ? -1f : 1f;

        return switch (side) {
            case DOWN -> new Vector3f(0, 1, 0);
            case UP -> new Vector3f(0, -1, 0);
            case WEST -> new Vector3f(x, 0, 0);
            case NORTH -> new Vector3f(0, 0, 1);
            case EAST -> new Vector3f(-x, 0, 0);
            default -> new Vector3f(0, 0, -1);
        };
    }

    /**
     * Mix two values.
     *
     * @param from the value at 0
     * @param to the value at 1
     * @param amount how far along
     * @return the mixed value
     */
    private static float lerp(final float from, final float to, final float amount) {
        return from + (to - from) * amount;
    }
}
