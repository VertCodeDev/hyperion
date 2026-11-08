package dev.vertcode.hyperion.config;

import net.kyori.adventure.text.format.TextColor;

/**
 * The colors every message uses, from the {@code palette} section of {@code messages.yml}.
 *
 * @param primary names and headings
 * @param highlight values that stand out next to the primary color
 * @param text plain text
 * @param muted labels and details
 * @param dim the quietest text, such as counts in brackets
 * @param success something worked
 * @param warning something needs a look
 * @param error something failed
 */
public record MessagePalette(
        TextColor primary,
        TextColor highlight,
        TextColor text,
        TextColor muted,
        TextColor dim,
        TextColor success,
        TextColor warning,
        TextColor error
) {
}
