package dev.vertcode.hyperion.api.model.hitbox;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * The size and place of a hitbox. Minecraft hitboxes are square from above, so there is one
 * width.
 *
 * @param width the width on x and z in blocks, above 0
 * @param height the height in blocks, above 0
 * @param offset the offset of the bottom center from the bone's pivot
 */
public record Hitbox(float width, float height, Vector3fc offset) {

    private static final String NOT_POSITIVE = "A hitbox needs a width and height above 0, got %s by %s";

    /**
     * Create a hitbox with a copy of the offset.
     *
     * @param width the width on x and z in blocks, above 0
     * @param height the height in blocks, above 0
     * @param offset the offset of the bottom center from the bone's pivot
     * @throws IllegalArgumentException if the width or height is not above 0
     */
    public Hitbox {
        if (width <= 0f || height <= 0f) {
            throw new IllegalArgumentException(String.format(NOT_POSITIVE, width, height));
        }

        offset = new Vector3f(offset);
    }

    /**
     * Create a hitbox standing on the bone's pivot.
     *
     * @param width the width in blocks
     * @param height the height in blocks
     * @return the hitbox
     * @throws IllegalArgumentException if the width or height is not above 0
     */
    public static Hitbox of(final float width, final float height) {
        return new Hitbox(width, height, new Vector3f());
    }
}
