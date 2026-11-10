package dev.vertcode.hyperion.pack.armor;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.armor.ArmorLayer;
import dev.vertcode.hyperion.armor.ArmorPart;
import dev.vertcode.hyperion.armor.ArmorQuad;
import dev.vertcode.hyperion.armor.HyperionArmorSet;
import dev.vertcode.hyperion.blueprint.data.Texture;

/**
 * Make the flat look of a set on one vanilla armor texture: its {@code flat_outer} or
 * {@code flat_inner}, or, when it has none, a look painted from the model itself.
 *
 * <p>The painted look projects the model onto each face of vanilla's boxes, as if seen from that
 * side, which is close enough for players far away, on Iris or OptiFine, or on 1.21 to 1.21.1.
 */
final class FlatTextures {

    /**
     * Width of the 64 by 32 armor layout.
     */
    static final int LAYOUT_WIDTH = 64;

    /**
     * Height of the 64 by 32 armor layout.
     */
    static final int LAYOUT_HEIGHT = 32;

    private static final int PAINT_SCALE = 4;
    private static final int OPAQUE = 128;
    private static final int SIDES = 6;

    private FlatTextures() {
    }

    /**
     * Make the flat look of a set on one texture.
     *
     * @param set the set
     * @param layer the texture
     * @return the image, or null when the set has nothing on this texture
     */
    static @Nullable BufferedImage base(final HyperionArmorSet set, final ArmorLayer layer) {
        OptionalInt flat = set.textures().flat(layer);
        if (flat.isPresent()) {
            return layout(set, flat.getAsInt());
        }

        Map<ArmorPart, List<ArmorQuad>> quads = GeometryLayers.quads(set, layer);
        if (quads.isEmpty()) {
            return null;
        }

        return paint(set, quads);
    }

    /**
     * Return the part of a set's flat look that takes the dye color.
     *
     * @param set the set
     * @param layer the texture
     * @return the image, or null when the set has none
     */
    static @Nullable BufferedImage tint(final HyperionArmorSet set, final ArmorLayer layer) {
        OptionalInt tint = set.textures().flatTint(layer);

        return tint.isPresent() ? layout(set, tint.getAsInt()) : null;
    }

    /**
     * Mark the glowing pixels of a set's flat look, from its {@code _glow} texture, with the alpha
     * the shader lights up.
     *
     * @param set the set
     * @param layer the texture
     * @param image the flat look, changed in place
     */
    static void glow(final HyperionArmorSet set, final ArmorLayer layer, final BufferedImage image) {
        OptionalInt glow = set.textures().flatGlow(layer);
        if (glow.isEmpty()) {
            return;
        }

        BufferedImage glowing = ArmorImages.scale(
                ArmorImages.firstFrame(set.model().textures().get(glow.getAsInt())),
                image.getWidth(),
                image.getHeight()
        );
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = glowing.getRGB(x, y);
                if (pixel >>> 24 >= OPAQUE) {
                    image.setRGB(x, y, (pixel & 0xFFFFFF) | ArmorShaderData.FLAT_GLOW_ALPHA << 24);
                }
            }
        }
    }

    /**
     * Read a flat texture at its own resolution, scaled to whole multiples of 64 by 32.
     *
     * @param set the set
     * @param index the texture's index
     * @return the image
     */
    private static BufferedImage layout(final HyperionArmorSet set, final int index) {
        Texture texture = set.model().textures().get(index);
        BufferedImage frame = ArmorImages.firstFrame(texture);
        int scale = Math.max(1, Math.round(frame.getWidth() / (float) LAYOUT_WIDTH));

        return ArmorImages.scale(frame, LAYOUT_WIDTH * scale, LAYOUT_HEIGHT * scale);
    }

    /**
     * Paint a flat look by projecting each limb's faces onto the sides of its box.
     *
     * @param set the set
     * @param quads the faces by limb
     * @return the image
     */
    private static BufferedImage paint(final HyperionArmorSet set, final Map<ArmorPart, List<ArmorQuad>> quads) {
        BufferedImage image = ArmorImages.blank(LAYOUT_WIDTH * PAINT_SCALE, LAYOUT_HEIGHT * PAINT_SCALE);
        List<BufferedImage> textures = new ArrayList<>();
        for (Texture texture : set.model().textures()) {
            textures.add(ArmorImages.firstFrame(texture));
        }

        // Left limbs share the right limbs' pixels, mirrored, so a right limb paints them when it can.
        for (ArmorPart part : ArmorPart.values()) {
            List<ArmorQuad> faces = quads.get(part);
            if (faces == null || faces.isEmpty()) {
                continue;
            }

            if (part.mirror() && quads.containsKey(right(part))) {
                continue;
            }

            for (int side = 0; side < SIDES; side++) {
                paintSide(image, part, side, faces, set, textures);
            }
        }

        return image;
    }

    /**
     * Paint one side of a limb's box.
     *
     * @param image the image
     * @param part the limb
     * @param side 0 to 5: down, up, west, north, east, south, vanilla's order
     * @param faces the limb's faces
     * @param set the set
     * @param textures the set's texture images
     */
    private static void paintSide(
            final BufferedImage image,
            final ArmorPart part,
            final int side,
            final List<ArmorQuad> faces,
            final HyperionArmorSet set,
            final List<BufferedImage> textures
    ) {
        float[] rect = VanillaBoxes.rect(part, side);
        int u1 = Math.round(Math.min(rect[0], rect[2]) * PAINT_SCALE);
        int u2 = Math.round(Math.max(rect[0], rect[2]) * PAINT_SCALE);
        int v1 = Math.round(Math.min(rect[1], rect[3]) * PAINT_SCALE);
        int v2 = Math.round(Math.max(rect[1], rect[3]) * PAINT_SCALE);
        for (int y = v1; y < v2; y++) {
            for (int x = u1; x < u2; x++) {
                float u = (x + 0.5f) / PAINT_SCALE;
                float v = (y + 0.5f) / PAINT_SCALE;
                Vector3f point = VanillaBoxes.point(part, side, u, v, 0f);
                int color = trace(point, VanillaBoxes.inward(part, side), faces, set, textures);
                if (color != 0) {
                    image.setRGB(x, y, color);
                }
            }
        }
    }

    /**
     * Find the colour of the first opaque face a ray meets, starting outside the limb and going
     * in.
     *
     * @param point a point on the ray, on the box's side
     * @param inward the ray's direction, one of the axes
     * @param faces the faces
     * @param set the set
     * @param textures the set's texture images
     * @return the colour as ARGB, or 0 when the ray meets nothing
     */
    private static int trace(
            final Vector3fc point,
            final Vector3fc inward,
            final List<ArmorQuad> faces,
            final HyperionArmorSet set,
            final List<BufferedImage> textures
    ) {
        int axis = inward.x() != 0 ? 0 : inward.y() != 0 ? 1 : 2;
        int a = (axis + 1) % 3;
        int b = (axis + 2) % 3;
        float sign = inward.get(axis);
        float best = Float.MAX_VALUE;
        int color = 0;
        for (ArmorQuad quad : faces) {
            for (int[] triangle : new int[][] {{0, 1, 2}, {0, 2, 3}}) {
                Vector3fc p0 = quad.corners().get(triangle[0]);
                Vector3fc p1 = quad.corners().get(triangle[1]);
                Vector3fc p2 = quad.corners().get(triangle[2]);
                float[] weights = barycentric(point.get(a), point.get(b), p0.get(a), p0.get(b), p1.get(a), p1.get(b), p2.get(a), p2.get(b));
                if (weights == null) {
                    continue;
                }

                float depth = sign * (weights[0] * p0.get(axis) + weights[1] * p1.get(axis) + weights[2] * p2.get(axis));
                if (depth >= best) {
                    continue;
                }

                int sample = sample(quad, triangle, weights, set, textures);
                if (sample >>> 24 >= OPAQUE) {
                    best = depth;
                    color = sample;
                }
            }
        }

        return color;
    }

    /**
     * Sample a face's texture at a point inside one of its triangles.
     *
     * @param quad the face
     * @param triangle the triangle's corners
     * @param weights the point's weights on those corners
     * @param set the set
     * @param textures the set's texture images
     * @return the colour as ARGB
     */
    private static int sample(
            final ArmorQuad quad,
            final int[] triangle,
            final float[] weights,
            final HyperionArmorSet set,
            final List<BufferedImage> textures
    ) {
        if (quad.texture() < 0 || quad.texture() >= textures.size()) {
            return 0;
        }

        Texture texture = set.model().textures().get(quad.texture());
        BufferedImage image = textures.get(quad.texture());
        float u = 0f;
        float v = 0f;
        for (int corner = 0; corner < 3; corner++) {
            Vector2fc uv = quad.uvs().get(triangle[corner]);
            u += weights[corner] * uv.x();
            v += weights[corner] * uv.y();
        }

        int x = (int) Math.floor(u * image.getWidth() / Math.max(1, texture.uvWidth()));
        int y = (int) Math.floor(v * image.getHeight() / Math.max(1, texture.uvHeight()));
        x = Math.max(0, Math.min(image.getWidth() - 1, x));
        y = Math.max(0, Math.min(image.getHeight() - 1, y));

        return image.getRGB(x, y);
    }

    /**
     * Work out a 2D point's weights on a triangle's corners.
     *
     * @param px the point's first coordinate
     * @param py the point's second coordinate
     * @param ax the first corner's first coordinate
     * @param ay the first corner's second coordinate
     * @param bx the second corner's first coordinate
     * @param by the second corner's second coordinate
     * @param cx the third corner's first coordinate
     * @param cy the third corner's second coordinate
     * @return the three weights, or null when the point is outside or the triangle is flat
     */
    private static float @Nullable [] barycentric(
            final float px,
            final float py,
            final float ax,
            final float ay,
            final float bx,
            final float by,
            final float cx,
            final float cy
    ) {
        float area = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay);
        if (Math.abs(area) < 1e-6f) {
            return null;
        }

        float w1 = ((bx - px) * (cy - py) - (cx - px) * (by - py)) / area;
        float w2 = ((cx - px) * (ay - py) - (ax - px) * (cy - py)) / area;
        float w3 = 1f - w1 - w2;
        if (w1 < 0f || w2 < 0f || w3 < 0f) {
            return null;
        }

        return new float[] {w1, w2, w3};
    }

    /**
     * Return the right limb a left limb mirrors.
     *
     * @param part the left limb
     * @return the right limb
     */
    private static ArmorPart right(final ArmorPart part) {
        return part == ArmorPart.LEFT_ARM ? ArmorPart.RIGHT_ARM : ArmorPart.RIGHT_LEG;
    }
}
