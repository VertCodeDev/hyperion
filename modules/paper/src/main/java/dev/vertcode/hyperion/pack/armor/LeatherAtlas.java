package dev.vertcode.hyperion.pack.armor;

import java.awt.image.BufferedImage;
import java.util.List;

import dev.vertcode.hyperion.armor.ArmorLayer;
import dev.vertcode.hyperion.armor.HyperionArmorSet;

/**
 * The leather texture of 1.21 to 1.21.1, widened into cells: vanilla's leather first, then one
 * cell per set, in set number order.
 *
 * <p>Those versions draw custom armor only as leather, so the patched shader picks a cell by the
 * leather's color. Dye does not show on custom sets there; the color is the set's name.
 */
final class LeatherAtlas {

    private LeatherAtlas() {
    }

    /**
     * Build one leather texture.
     *
     * @param vanilla vanilla's leather texture for this layer
     * @param sets the sets, the first being number 1
     * @param layer the layer
     * @return the widened texture
     */
    static BufferedImage build(final BufferedImage vanilla, final List<HyperionArmorSet> sets, final ArmorLayer layer) {
        BufferedImage[] cells = new BufferedImage[sets.size() + 1];
        cells[0] = vanilla;
        int scale = Math.max(1, vanilla.getWidth() / FlatTextures.LAYOUT_WIDTH);
        for (int index = 0; index < sets.size(); index++) {
            HyperionArmorSet set = sets.get(index);
            BufferedImage base = FlatTextures.base(set, layer);
            BufferedImage tint = FlatTextures.tint(set, layer);
            BufferedImage cell = base == null ? ArmorImages.blank(FlatTextures.LAYOUT_WIDTH, FlatTextures.LAYOUT_HEIGHT) : base;
            if (tint != null) {
                cell = over(cell, tint);
            }

            cells[index + 1] = cell;
            scale = Math.max(scale, cell.getWidth() / FlatTextures.LAYOUT_WIDTH);
        }

        int width = FlatTextures.LAYOUT_WIDTH * scale;
        int height = FlatTextures.LAYOUT_HEIGHT * scale;
        BufferedImage atlas = ArmorImages.blank(width * cells.length, height);
        for (int index = 0; index < cells.length; index++) {
            BufferedImage cell = ArmorImages.scale(cells[index], width, height);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    atlas.setRGB(index * width + x, y, cell.getRGB(x, y));
                }
            }
        }

        return atlas;
    }

    /**
     * Lay one image over another of the same layout, opaque pixels winning.
     *
     * @param base the image below
     * @param top the image above
     * @return a new image at the larger resolution
     */
    private static BufferedImage over(final BufferedImage base, final BufferedImage top) {
        int width = Math.max(base.getWidth(), top.getWidth());
        int height = Math.max(base.getHeight(), top.getHeight());
        BufferedImage below = ArmorImages.scale(base, width, height);
        BufferedImage above = ArmorImages.scale(top, width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = above.getRGB(x, y);
                if (pixel >>> 24 >= 128) {
                    below.setRGB(x, y, pixel);
                }
            }
        }

        return below;
    }
}
