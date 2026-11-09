package dev.vertcode.hyperion.config;

/**
 * The {@code gallery} section of {@code config.yml}.
 *
 * @param previewSeconds how long a private preview from the gallery stays before it goes
 */
public record GallerySettings(int previewSeconds) {
}
