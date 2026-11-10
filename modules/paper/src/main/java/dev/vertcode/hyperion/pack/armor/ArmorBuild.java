package dev.vertcode.hyperion.pack.armor;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import dev.vertcode.hyperion.armor.HyperionArmorSet;
import dev.vertcode.hyperion.config.ArmorSettings;

/**
 * Everything the pack builder needs to write armor.
 *
 * @param sets the sets, sorted by key
 * @param settings the armor settings
 * @param minecraftVersion the server's version as Mojang names it, such as {@code 1.21.4}, to
 *     find its client jar
 * @param cache the folder the client jar is kept in
 * @param logger where problems are logged
 */
public record ArmorBuild(
        List<HyperionArmorSet> sets,
        ArmorSettings settings,
        String minecraftVersion,
        Path cache,
        Logger logger
) {

    /**
     * Copy the sets, so the build never changes.
     */
    public ArmorBuild {
        sets = List.copyOf(sets);
    }
}
