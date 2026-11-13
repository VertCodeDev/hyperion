package dev.vertcode.hyperion.skin;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import dev.vertcode.hyperion.api.skin.Skin;
import dev.vertcode.hyperion.blueprint.BlueprintBone;
import dev.vertcode.hyperion.blueprint.data.Cube;
import dev.vertcode.hyperion.blueprint.data.CubeFace;
import dev.vertcode.hyperion.blueprint.data.Texture;

/**
 * Finds the skin texture of a Blockbench file: the texture named {@code player_skin}, whose faces are
 * drawn with a player's skin in place of the image in the file.
 */
public final class SkinTextures {

    /**
     * The index for a file without a skin texture.
     */
    public static final int NONE = -1;

    private static final String NAME = "player_skin";
    private static final String EXTENSION = ".png";
    private static final int LEGACY_HEIGHT = 32;

    private SkinTextures() {
    }

    /**
     * Find the skin texture of a file, when a face uses it.
     *
     * @param textures the file's textures
     * @param bones the file's bones, with their cubes
     * @return the texture's index, or {@link #NONE}
     */
    public static int find(final List<Texture> textures, final List<BlueprintBone> bones) {
        for (int index = 0; index < textures.size(); index++) {
            if (isSkin(textures.get(index).name()) && used(index, bones)) {
                return index;
            }
        }

        return NONE;
    }

    /**
     * Read the image of a file's skin texture as a skin, for a model nobody gave another one.
     *
     * @param texture the skin texture
     * @return its image as a skin, or the plain skin when the image is not a skin's size
     */
    public static HyperionSkin fileSkin(final Texture texture) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(texture.png()));
            if (image != null && image.getWidth() == Skin.SIZE
                    && (image.getHeight() == Skin.SIZE || image.getHeight() == LEGACY_HEIGHT)) {
                return HyperionSkin.of(image, false);
            }
        } catch (IOException exception) {
            // Falls through to the plain skin: an unreadable image shows as nothing in the pack too.
        }

        return HyperionSkin.plain();
    }

    /**
     * Return whether a texture's name marks it as the skin: {@code player_skin}, with or without
     * {@code .png}, in any case.
     *
     * @param name the texture's name
     * @return true for the skin texture
     */
    private static boolean isSkin(final String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(EXTENSION)) {
            lower = lower.substring(0, lower.length() - EXTENSION.length());
        }

        return lower.equals(NAME);
    }

    /**
     * Return whether any face of any cube uses a texture.
     *
     * @param texture the texture's index
     * @param bones the bones to look through
     * @return true when a face uses it
     */
    private static boolean used(final int texture, final List<BlueprintBone> bones) {
        for (BlueprintBone bone : bones) {
            for (Cube cube : bone.cubes()) {
                for (CubeFace face : cube.faces().values()) {
                    if (face.texture() == texture) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
