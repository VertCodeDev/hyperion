package dev.vertcode.hyperion.skin;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.vertcode.hyperion.api.skin.Skin;

/**
 * A skin held as its 64 by 64 pixels.
 *
 * <p>It also remembers the colors it gave each model part, so a model with many viewers, or
 * many models wearing the same skin, read the skin once per part.
 */
public final class HyperionSkin implements Skin {

    private static final String BAD_SIZE = "A skin must be 64 pixels wide and 64 or 32 high, got %dx%d";
    private static final int LEGACY_HEIGHT = 32;
    private static final int MAX_REMEMBERED = 256;

    private final int[] pixels;
    private final boolean slim;
    private final Map<SkinMap, SkinColors> colors = new ConcurrentHashMap<>();

    /**
     * Create a skin from its pixels.
     *
     * @param pixels 64 by 64 pixels as ARGB, row by row; kept as given, so hand over a fresh array
     * @param slim whether it is made for the slim arms
     */
    private HyperionSkin(final int[] pixels, final boolean slim) {
        this.pixels = pixels;
        this.slim = slim;
    }

    /**
     * Read a skin from an image.
     *
     * @param image the skin, 64 by 64 pixels, or 64 by 32 in the old layout
     * @param slim whether it is made for the slim arms
     * @return the skin
     * @throws IllegalArgumentException if the image has another size
     */
    public static HyperionSkin of(final BufferedImage image, final boolean slim) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width != SIZE || (height != SIZE && height != LEGACY_HEIGHT)) {
            throw new IllegalArgumentException(String.format(BAD_SIZE, width, height));
        }

        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);

        return new HyperionSkin(SkinLayout.normalize(argb, height), slim);
    }

    /**
     * Return a skin as the plugin's own kind, copying its pixels when it is another.
     *
     * @param skin the skin
     * @return the same skin when it already is one of these, else a copy
     */
    public static HyperionSkin of(final Skin skin) {
        if (skin instanceof HyperionSkin own) {
            return own;
        }

        int[] pixels = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                pixels[y * SIZE + x] = skin.pixel(x, y);
            }
        }

        return new HyperionSkin(pixels, skin.slim());
    }

    /**
     * Make the plain skin used when a player's own cannot be had.
     *
     * @return the skin
     */
    public static HyperionSkin plain() {
        return new HyperionSkin(SkinLayout.plain(), false);
    }

    @Override
    public boolean slim() {
        return this.slim;
    }

    @Override
    public int pixel(final int x, final int y) {
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) {
            return 0;
        }

        return this.pixels[y * SIZE + x];
    }

    /**
     * Return the colors and flags that draw this skin on a model part.
     *
     * @param map the skin pixels the part shows
     * @return the colors and flags for the part's item
     */
    public SkinColors colors(final SkinMap map) {
        SkinColors known = this.colors.get(map);
        if (known != null) {
            return known;
        }

        // Parts go when the pack is rebuilt; start over rather than keep the old ones forever.
        if (this.colors.size() >= MAX_REMEMBERED) {
            this.colors.clear();
        }

        SkinColors made = map.colors(this);
        this.colors.put(map, made);

        return made;
    }
}
