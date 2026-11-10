package dev.vertcode.hyperion.pack.armor;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector2fc;

import dev.vertcode.hyperion.armor.HyperionArmorSet;
import dev.vertcode.hyperion.blueprint.data.Texture;

/**
 * A set's model textures stacked in one column, with the alpha codes the shader reads.
 *
 * <p>Only the first frame of an animated texture is used. Each texture keeps its own pixels;
 * the alpha says whether a pixel is a hole, plain, tinted or glowing, so it never shows in
 * vanilla.
 */
final class ArmorAtlas {

    private static final int OPAQUE = 128;

    private final BufferedImage image;
    private final int[] offsets;
    private final float[] uScales;
    private final float[] vScales;

    /**
     * Stack a set's textures.
     *
     * @param set the set
     */
    ArmorAtlas(final HyperionArmorSet set) {
        List<Texture> textures = set.model().textures();
        List<BufferedImage> frames = new ArrayList<>(textures.size());
        int width = 1;
        int height = 0;
        for (Texture texture : textures) {
            BufferedImage frame = ArmorImages.firstFrame(texture);
            frames.add(frame);
            width = Math.max(width, frame.getWidth());
            height += frame.getHeight();
        }

        this.image = ArmorImages.blank(width, Math.max(1, height));
        this.offsets = new int[textures.size()];
        this.uScales = new float[textures.size()];
        this.vScales = new float[textures.size()];
        int y = 0;
        for (int index = 0; index < frames.size(); index++) {
            BufferedImage frame = frames.get(index);
            Texture texture = textures.get(index);
            int code = ArmorShaderData.PLAIN
                    + (set.dyeable() && set.textures().tinted(index) ? ArmorShaderData.TINT : 0)
                    + (set.textures().glowing(index) ? ArmorShaderData.GLOW : 0);
            for (int row = 0; row < frame.getHeight(); row++) {
                for (int column = 0; column < frame.getWidth(); column++) {
                    int pixel = frame.getRGB(column, row);
                    int alpha = pixel >>> 24 >= OPAQUE ? code : 0;
                    this.image.setRGB(column, y + row, (pixel & 0xFFFFFF) | alpha << 24);
                }
            }

            this.offsets[index] = y;
            this.uScales[index] = frame.getWidth() / (float) Math.max(1, texture.uvWidth());
            this.vScales[index] = frame.getHeight() / (float) Math.max(1, texture.uvHeight());
            y += frame.getHeight();
        }
    }

    /**
     * Return the stacked image.
     *
     * @return the image
     */
    BufferedImage image() {
        return this.image;
    }

    /**
     * Move a UV of one texture to its pixel in the stacked image.
     *
     * @param texture the texture's index
     * @param uv the UV in the texture's UV units
     * @return the pixel position in the stacked image
     */
    Vector2fc pixel(final int texture, final Vector2fc uv) {
        if (texture < 0 || texture >= this.offsets.length) {
            return new Vector2f();
        }

        return new Vector2f(uv.x() * this.uScales[texture], this.offsets[texture] + uv.y() * this.vScales[texture]);
    }
}
