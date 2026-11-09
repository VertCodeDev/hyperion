package dev.vertcode.hyperion.config;

/**
 * The {@code models} section of {@code config.yml}: what a live model starts with when the
 * plugin that spawns it does not say.
 *
 * @param viewDistance how far away, in blocks, players see a model
 * @param interpolation over how many ticks a client smooths each move of a bone
 * @param autoAnimations whether models on entities play their idle and walk animations
 * @param spawnEntity the mob {@code /hy spawn} puts a model on, such as {@code pig}
 */
public record ModelSettings(int viewDistance, int interpolation, AutoAnimations autoAnimations, String spawnEntity) {
}
