package dev.vertcode.hyperion.pack;

import java.util.List;

import org.joml.Vector3f;

import dev.vertcode.hyperion.api.skin.Skin;
import dev.vertcode.hyperion.blueprint.data.Cube;
import dev.vertcode.hyperion.blueprint.data.CubeFace;
import dev.vertcode.hyperion.blueprint.data.FaceSide;
import dev.vertcode.hyperion.skin.SkinLayout;
import dev.vertcode.hyperion.skin.SkinPixel;
import dev.vertcode.hyperion.skin.SkinVariant;

/**
 * Cuts a face that shows the skin into one flat square per skin pixel.
 *
 * <p>The pack cannot draw a texture it does not have, and it does not have a player's skin. So
 * a skin face is drawn as small white squares, one for each pixel it shows, and the item colors
 * each square with that pixel of the skin. The squares lie exactly where the face's texture
 * would put its pixels, following the face's UV box, its mirroring and its rotation.
 */
final class SkinFaces {

    private static final float EPSILON = 0.001f;
    private static final int QUARTER_TURN = 90;
    private static final int HALF_TURN = 180;
    private static final int THREE_QUARTER_TURN = 270;
    private static final int FULL_TURN = 360;

    private SkinFaces() {
    }

    /**
     * Cut one face of a cube into its skin pixels.
     *
     * @param cube the cube, in the model's pixels
     * @param side which face
     * @param face the face's UV box, on the skin texture
     * @param uvWidth the width the texture's UVs are measured in
     * @param uvHeight the height the texture's UVs are measured in
     * @param variant which skins the cube is drawn for
     * @param into where the squares are added
     */
    static void cut(
            final Cube cube,
            final FaceSide side,
            final CubeFace face,
            final int uvWidth,
            final int uvHeight,
            final SkinVariant variant,
            final List<SkinQuad> into
    ) {
        float u1 = face.u1() * Skin.SIZE / Math.max(1, uvWidth);
        float u2 = face.u2() * Skin.SIZE / Math.max(1, uvWidth);
        float v1 = face.v1() * Skin.SIZE / Math.max(1, uvHeight);
        float v2 = face.v2() * Skin.SIZE / Math.max(1, uvHeight);
        if (Math.abs(u2 - u1) < EPSILON || Math.abs(v2 - v1) < EPSILON) {
            return;
        }

        float inflate = cube.inflate();
        Vector3f low = new Vector3f(cube.from()).sub(inflate, inflate, inflate);
        Vector3f high = new Vector3f(cube.to()).add(inflate, inflate, inflate);
        int turn = Math.floorMod(face.rotation(), FULL_TURN);
        int firstX = (int) Math.floor(Math.min(u1, u2) + EPSILON);
        int lastX = (int) Math.ceil(Math.max(u1, u2) - EPSILON) - 1;
        int firstY = (int) Math.floor(Math.min(v1, v2) + EPSILON);
        int lastY = (int) Math.ceil(Math.max(v1, v2) - EPSILON) - 1;
        for (int y = firstY; y <= lastY; y++) {
            float b1 = clamp((y - v1) / (v2 - v1));
            float b2 = clamp((y + 1 - v1) / (v2 - v1));
            for (int x = firstX; x <= lastX; x++) {
                float a1 = clamp((x - u1) / (u2 - u1));
                float a2 = clamp((x + 1 - u1) / (u2 - u1));
                Vector3f first = place(side, low, high, across(turn, a1, b1), down(turn, a1, b1));
                Vector3f second = place(side, low, high, across(turn, a2, b2), down(turn, a2, b2));
                boolean optional = variant != SkinVariant.ANY || SkinLayout.outer(x, y);
                into.add(new SkinQuad(
                        new Vector3f(first).min(second),
                        new Vector3f(first).max(second),
                        side,
                        new SkinPixel(x, y, variant),
                        optional
                ));
            }
        }
    }

    /**
     * Return how far across a face a point of its UV box lies, from the corner the box starts
     * at for an unturned face.
     *
     * @param turn the face's rotation, 0, 90, 180 or 270
     * @param a how far along the box's U the point is, from 0 to 1
     * @param b how far along the box's V the point is, from 0 to 1
     * @return the distance across the face, from 0 to 1
     */
    private static float across(final int turn, final float a, final float b) {
        return switch (turn) {
            case QUARTER_TURN -> 1f - b;
            case HALF_TURN -> 1f - a;
            case THREE_QUARTER_TURN -> b;
            default -> a;
        };
    }

    /**
     * Return how far down a face a point of its UV box lies.
     *
     * @param turn the face's rotation, 0, 90, 180 or 270
     * @param a how far along the box's U the point is, from 0 to 1
     * @param b how far along the box's V the point is, from 0 to 1
     * @return the distance down the face, from 0 to 1
     */
    private static float down(final int turn, final float a, final float b) {
        return switch (turn) {
            case QUARTER_TURN -> a;
            case HALF_TURN -> 1f - b;
            case THREE_QUARTER_TURN -> 1f - a;
            default -> b;
        };
    }

    /**
     * Find the point of a cube's face that lies a given way across and down it, the way the
     * game lays a texture on that face.
     *
     * @param side which face
     * @param low the cube's lowest corner
     * @param high the cube's highest corner
     * @param across how far across the face, from 0 to 1
     * @param down how far down the face, from 0 to 1
     * @return the point
     */
    private static Vector3f place(
            final FaceSide side,
            final Vector3f low,
            final Vector3f high,
            final float across,
            final float down
    ) {
        float width = high.x - low.x;
        float height = high.y - low.y;
        float depth = high.z - low.z;

        return switch (side) {
            case NORTH -> new Vector3f(high.x - across * width, high.y - down * height, low.z);
            case SOUTH -> new Vector3f(low.x + across * width, high.y - down * height, high.z);
            case WEST -> new Vector3f(low.x, high.y - down * height, low.z + across * depth);
            case EAST -> new Vector3f(high.x, high.y - down * height, high.z - across * depth);
            case UP -> new Vector3f(low.x + across * width, high.y, low.z + down * depth);
            case DOWN -> new Vector3f(low.x + across * width, low.y, high.z - down * depth);
        };
    }

    /**
     * Keep a fraction between 0 and 1.
     *
     * @param value the fraction
     * @return the value, no lower than 0 and no higher than 1
     */
    private static float clamp(final float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
