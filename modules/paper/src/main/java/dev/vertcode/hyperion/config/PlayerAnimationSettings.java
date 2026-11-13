package dev.vertcode.hyperion.config;

/**
 * The {@code player-animations} section of {@code config.yml}.
 *
 * @param showToSelf whether a player sees the model that stands in for them
 * @param stopOnSneak whether sneaking ends an animation on a player, unless the caller chose
 * @param stopOnDamage whether taking damage ends an animation on a player, unless the caller
 *     chose
 * @param fadeTicks how many ticks an animation takes to blend in and out
 */
public record PlayerAnimationSettings(boolean showToSelf, boolean stopOnSneak, boolean stopOnDamage, int fadeTicks) {
}
