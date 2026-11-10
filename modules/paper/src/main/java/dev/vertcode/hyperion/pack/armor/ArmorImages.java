package dev.vertcode.hyperion.pack.armor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import dev.vertcode.hyperion.blueprint.data.Texture;

/**
 * Read, scale and write the images armor textures are made of.
 */
final class ArmorImages {

    private ArmorImages() {
    }

    /**
     * Read a texture's image, only its first frame when it is animated.
     *
     * @param texture the texture
     * @return the image, 1 by 1 transparent when it has none
     */
    static BufferedImage firstFrame(final Texture texture) {
        BufferedImage image = read(texture.png());
        int frames = Math.max(1, texture.frames());
        int height = Math.max(1, image.getHeight() / frames);
        if (height == image.getHeight()) {
            return image;
        }

        return copy(image.getSubimage(0, 0, image.getWidth(), height));
    }

    /**
     * Read PNG bytes into an image with alpha.
     *
     * @param png the bytes
     * @return the image, 1 by 1 transparent when they are not a readable PNG
     */
    static BufferedImage read(final byte[] png) {
        try {
            BufferedImage image = png.length == 0 ? null : ImageIO.read(new ByteArrayInputStream(png));
            return image == null ? blank(1, 1) : copy(image);
        } catch (IOException exception) {
            return blank(1, 1);
        }
    }

    /**
     * Write an image as PNG bytes.
     *
     * @param image the image
     * @return the bytes
     */
    static byte[] png(final BufferedImage image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", bytes);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        return bytes.toByteArray();
    }

    /**
     * Make a transparent image.
     *
     * @param width the width
     * @param height the height
     * @return the image
     */
    static BufferedImage blank(final int width, final int height) {
        return new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Scale an image to a size, nearest pixel, so pixel art stays sharp.
     *
     * @param image the image
     * @param width the new width
     * @param height the new height
     * @return the scaled image
     */
    static BufferedImage scale(final BufferedImage image, final int width, final int height) {
        BufferedImage scaled = blank(width, height);
        for (int y = 0; y < height; y++) {
            int sourceY = Math.min(image.getHeight() - 1, y * image.getHeight() / height);
            for (int x = 0; x < width; x++) {
                int sourceX = Math.min(image.getWidth() - 1, x * image.getWidth() / width);
                scaled.setRGB(x, y, image.getRGB(sourceX, sourceY));
            }
        }

        return scaled;
    }

    /**
     * Copy an image into a plain ARGB one, whatever kind it was read as.
     *
     * @param image the image
     * @return the copy
     */
    static BufferedImage copy(final BufferedImage image) {
        BufferedImage copy = blank(image.getWidth(), image.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                copy.setRGB(x, y, image.getRGB(x, y));
            }
        }

        return copy;
    }

    /**
     * Pack four bytes into one ARGB pixel.
     *
     * @param red the red byte
     * @param green the green byte
     * @param blue the blue byte
     * @param alpha the alpha byte
     * @return the pixel
     */
    static int argb(final int red, final int green, final int blue, final int alpha) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
    }
}
