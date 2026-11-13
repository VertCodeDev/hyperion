package dev.vertcode.hyperion.skin;

/**
 * One pixel of a skin that a face of a model part shows.
 *
 * @param x the pixel's column in the skin
 * @param y the pixel's row in the skin
 * @param variant which skins the pixel's cube is drawn for
 */
public record SkinPixel(int x, int y, SkinVariant variant) {
}
