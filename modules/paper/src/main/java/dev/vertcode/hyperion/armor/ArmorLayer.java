package dev.vertcode.hyperion.armor;

/**
 * The two armor textures vanilla draws: the outer one for the chestplate and boots, and the inner
 * one for the leggings.
 */
public enum ArmorLayer {

    /**
     * The outer texture, {@code humanoid} in an equipment asset and {@code _layer_1} before.
     */
    OUTER("humanoid", 1f, "flat_outer"),

    /**
     * The inner texture, {@code humanoid_leggings} in an equipment asset and {@code _layer_2}
     * before.
     */
    INNER("humanoid_leggings", 0.5f, "flat_inner");

    private final String asset;
    private final float inflate;
    private final String texture;

    ArmorLayer(final String asset, final float inflate, final String texture) {
        this.asset = asset;
        this.inflate = inflate;
        this.texture = texture;
    }

    /**
     * Return the layer's name in an equipment asset, which is also its texture folder.
     *
     * @return the name, such as {@code humanoid}
     */
    public String asset() {
        return this.asset;
    }

    /**
     * Return how far vanilla grows the boxes this layer is drawn on, in pixels.
     *
     * @return the growth
     */
    public float inflate() {
        return this.inflate;
    }

    /**
     * Return the name of the flat texture for this layer in a set's {@code .bbmodel}.
     *
     * @return the texture name, such as {@code flat_outer}
     */
    public String texture() {
        return this.texture;
    }
}
