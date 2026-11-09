package dev.vertcode.hyperion.config;

/**
 * The {@code bedrock} section of {@code config.yml}.
 *
 * @param enabled whether Bedrock players see models through Hyperion's Geyser extension
 * @param linkPort the port the Geyser extension connects to
 */
public record BedrockSettings(boolean enabled, int linkPort) {
}
