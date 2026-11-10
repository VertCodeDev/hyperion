package dev.vertcode.hyperion.config;

/**
 * How custom armor is drawn.
 *
 * @param shader whether chestplates, leggings and boots are drawn in 3D by the core shader, from
 *     1.21.2; false draws them flat
 * @param renderDistance how far from the camera armor is drawn in 3D, in blocks; beyond it, flat
 * @param maxLayers the most equipment layers one set may use for 3D: each draws two faces per
 *     limb
 */
public record ArmorSettings(boolean shader, int renderDistance, int maxLayers) {
}
