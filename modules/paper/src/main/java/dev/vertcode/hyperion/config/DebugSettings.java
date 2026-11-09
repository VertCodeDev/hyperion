package dev.vertcode.hyperion.config;

/**
 * The {@code debug} section of {@code config.yml}: how {@code /hy debug} draws.
 *
 * @param interval the ticks between redraws
 * @param labels whether bones, locators, seats and effect parts get their names as floating text
 * @param axisLength how long a bone's axes are, in blocks
 */
public record DebugSettings(int interval, boolean labels, float axisLength) {
}
