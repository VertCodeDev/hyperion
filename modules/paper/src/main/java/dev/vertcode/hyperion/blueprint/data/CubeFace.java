package dev.vertcode.hyperion.blueprint.data;

/**
 * One face of a cube: where on the texture it samples and which texture.
 *
 * @param u1 the left edge of the UV area, in UV pixels
 * @param v1 the top edge of the UV area, in UV pixels
 * @param u2 the right edge of the UV area, in UV pixels
 * @param v2 the bottom edge of the UV area, in UV pixels
 * @param texture the index into the model's textures, or {@link #NO_TEXTURE}
 * @param rotation the UV rotation in degrees: 0, 90, 180 or 270
 */
public record CubeFace(float u1, float v1, float u2, float v2, int texture, int rotation) {

    /**
     * The texture index of a face that shows nothing.
     */
    public static final int NO_TEXTURE = -1;

    /**
     * Return whether this face shows a texture.
     *
     * @return false for a face Blockbench left empty
     */
    public boolean textured() {
        return this.texture != NO_TEXTURE;
    }
}
