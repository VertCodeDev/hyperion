package dev.vertcode.hyperion.effect.part;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Fill in PlaceholderAPI's placeholders when the server has it, and leave text alone when it does
 * not.
 *
 * <p>PlaceholderAPI is an optional dependency: its classes are only touched after the check that
 * it is enabled, so Hyperion runs without it.
 */
final class Placeholders {

    private static final String PLUGIN = "PlaceholderAPI";

    private Placeholders() {
    }

    /**
     * Fill in the placeholders of a text.
     *
     * @param player the player they are for, or null for the server's placeholders only
     * @param text the text
     * @return the text with every placeholder PlaceholderAPI knows filled in
     */
    static String apply(final @Nullable Player player, final String text) {
        if (text.indexOf('%') < 0 || !Bukkit.getPluginManager().isPluginEnabled(PLUGIN)) {
            return text;
        }

        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
