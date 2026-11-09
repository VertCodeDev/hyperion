package dev.vertcode.hyperion.config;

/**
 * The {@code effects} section of {@code config.yml}.
 *
 * @param viewDistance how far away, in blocks, players see an effect that is not on a model
 * @param particleMultiplier what every particle count is scaled by
 */
public record EffectSettings(int viewDistance, float particleMultiplier) {
}
