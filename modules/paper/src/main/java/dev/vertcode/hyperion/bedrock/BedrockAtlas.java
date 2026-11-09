package dev.vertcode.hyperion.bedrock;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.blueprint.data.CubeFace;
import dev.vertcode.hyperion.blueprint.data.Texture;

/**
 * A model's textures stacked into one image, since a Bedrock entity draws from one texture.
 *
 * <p>Each texture keeps its own resolution; a face's UV is moved and scaled from its texture's UV
 * space into the atlas's pixels. An animated texture contributes its first frame.
 */
final class BedrockAtlas {

    private static final int EMPTY_SIZE = 16;

    private final byte[] png;
    private final int width;
    private final int height;
    private final int[] offsets;
    private final float[] scaleX;
    private final float[] scaleY;

    private BedrockAtlas(
            final byte[] png,
            final int width,
            final int height,
            final int[] offsets,
            final float[] scaleX,
            final float[] scaleY
    ) {
        this.png = png;
        this.width = width;
        this.height = height;
        this.offsets = offsets;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /**
     * Stack a model's textures.
     *
     * @param textures the textures, in the order faces point at them
     * @return the atlas
     */
    static BedrockAtlas of(final List<Texture> textures) {
        int count = textures.size();
        @Nullable BufferedImage[] frames = new BufferedImage[count];
        int[] offsets = new int[count];
        float[] scaleX = new float[count];
        float[] scaleY = new float[count];
        int width = 0;
        int height = 0;
        for (int i = 0; i < count; i++) {
            Texture texture = textures.get(i);
            BufferedImage image = read(texture.png());
            if (image == null) {
                offsets[i] = -1;
                continue;
            }

            int frameHeight = Math.max(1, image.getHeight() / Math.max(1, texture.frames()));
            frames[i] = image.getSubimage(0, 0, image.getWidth(), frameHeight);
            offsets[i] = height;
            scaleX[i] = (float) image.getWidth() / Math.max(1, texture.uvWidth());
            scaleY[i] = (float) frameHeight / Math.max(1, texture.uvHeight());
            width = Math.max(width, image.getWidth());
            height += frameHeight;
        }

        width = Math.max(width, EMPTY_SIZE);
        height = Math.max(height, EMPTY_SIZE);
        BufferedImage atlas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < count; i++) {
            BufferedImage frame = frames[i];
            if (frame != null) {
                atlas.getGraphics().drawImage(frame, 0, offsets[i], null);
            }
        }

        return new BedrockAtlas(write(atlas), width, height, offsets, scaleX, scaleY);
    }

    /**
     * Move a face's UV into the atlas, as Bedrock's per-face {@code uv} and {@code uv_size}.
     *
     * @param face the face
     * @return the start and size in atlas pixels, four floats, or null when its texture is missing
     */
    float @Nullable [] uv(final CubeFace face) {
        int texture = face.texture();
        if (texture < 0 || texture >= this.offsets.length || this.offsets[texture] < 0) {
            return null;
        }

        float sx = this.scaleX[texture];
        float sy = this.scaleY[texture];
        float u = face.u1() * sx;
        float v = this.offsets[texture] + face.v1() * sy;

        return new float[] {u, v, (face.u2() - face.u1()) * sx, (face.v2() - face.v1()) * sy};
    }

    byte[] png() {
        return this.png.clone();
    }

    int width() {
        return this.width;
    }

    int height() {
        return this.height;
    }

    /**
     * Read a PNG.
     *
     * @param png the bytes
     * @return the image, or null when there is none or it cannot be read
     */
    private static @Nullable BufferedImage read(final byte[] png) {
        if (png.length == 0) {
            return null;
        }

        try {
            return ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException exception) {
            return null;
        }
    }

    /**
     * Write an image as PNG.
     *
     * @param image the image
     * @return the bytes
     */
    private static byte[] write(final BufferedImage image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", bytes);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        return bytes.toByteArray();
    }
}
