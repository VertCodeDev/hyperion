package dev.vertcode.hyperion.config;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

/**
 * Hyperion's settings from {@code config.yml}.
 *
 * <p>Immutable: a reload reads a new one. A value that cannot be used falls back to its default
 * with a warning that names the key, so one typo never stops the plugin.
 *
 * @param pack the resource pack settings
 * @param models the settings live models start with
 */
public record HyperionConfig(PackSettings pack, ModelSettings models) {

    /**
     * The file name, in the data folder and in the jar.
     */
    public static final String FILE = "config.yml";

    private static final String BAD_VALUE = "config.yml: '%s' is not a valid %s, using %s";
    private static final int DEFAULT_PORT = 8163;
    private static final int MAX_PORT = 65_535;
    private static final int DEFAULT_CUSTOM_MODEL_DATA = 10_000;
    private static final String DEFAULT_BASE_ITEM = "paper";
    private static final String MINECRAFT = "minecraft:";
    private static final int DEFAULT_VIEW_DISTANCE = 48;
    private static final int DEFAULT_INTERPOLATION = 3;

    /**
     * Read {@code config.yml} from the data folder, writing or completing it from the jar first.
     *
     * @param plugin the plugin whose data folder and jar to read
     * @return the settings
     */
    public static HyperionConfig load(final Plugin plugin) {
        return of(BundledYaml.load(plugin, FILE), plugin.getLogger(), HyperionConfig::isItem);
    }

    /**
     * Read the settings from a loaded file.
     *
     * @param config the contents of {@code config.yml}
     * @param logger where unusable values are reported
     * @param isItem tells whether a name, such as {@code paper}, is an item on this server
     * @return the settings
     */
    public static HyperionConfig of(
            final ConfigurationSection config,
            final Logger logger,
            final Predicate<String> isItem
    ) {
        String hostingKey = config.getString("pack.hosting", PackHosting.SELF_HOST.key());
        PackHosting hosting = PackHosting.fromKey(hostingKey).orElseGet(() -> {
            logger.warning(String.format(BAD_VALUE, hostingKey, "pack.hosting", PackHosting.SELF_HOST.key()));
            return PackHosting.SELF_HOST;
        });

        int port = config.getInt("pack.self-host.port", DEFAULT_PORT);
        if (port < 1 || port > MAX_PORT) {
            logger.warning(String.format(BAD_VALUE, port, "pack.self-host.port", DEFAULT_PORT));
            port = DEFAULT_PORT;
        }

        int viewDistance = config.getInt("models.view-distance", DEFAULT_VIEW_DISTANCE);
        if (viewDistance < 1) {
            logger.warning(String.format(BAD_VALUE, viewDistance, "models.view-distance", DEFAULT_VIEW_DISTANCE));
            viewDistance = DEFAULT_VIEW_DISTANCE;
        }

        int interpolation = config.getInt("models.interpolation", DEFAULT_INTERPOLATION);
        if (interpolation < 0) {
            logger.warning(String.format(BAD_VALUE, interpolation, "models.interpolation", DEFAULT_INTERPOLATION));
            interpolation = DEFAULT_INTERPOLATION;
        }

        PackSettings pack = new PackSettings(
                hosting,
                port,
                config.getString("pack.self-host.address", "").trim(),
                config.getString("pack.url", "").trim(),
                config.getBoolean("pack.send-on-join", true),
                config.getBoolean("pack.required", false),
                config.getBoolean("pack.resend-on-change", true),
                baseItem(config.getString("pack.base-item", DEFAULT_BASE_ITEM), logger, isItem),
                config.getInt("pack.custom-model-data-start", DEFAULT_CUSTOM_MODEL_DATA)
        );

        return new HyperionConfig(pack, new ModelSettings(viewDistance, interpolation));
    }

    /**
     * Read the base item, which must be an item rather than a block-only material.
     *
     * @param name the item's name, such as {@code paper} or {@code minecraft:paper}
     * @param logger where an unusable name is reported
     * @param isItem tells whether a name is an item on this server
     * @return the item's name without the namespace, {@code paper} when the name is not an item
     */
    private static String baseItem(final String name, final Logger logger, final Predicate<String> isItem) {
        String item = name.trim().toLowerCase(Locale.ROOT);
        if (item.startsWith(MINECRAFT)) {
            item = item.substring(MINECRAFT.length());
        }

        if (!isItem.test(item)) {
            logger.warning(String.format(BAD_VALUE, name, "pack.base-item", DEFAULT_BASE_ITEM));
            return DEFAULT_BASE_ITEM;
        }

        return item;
    }

    /**
     * Return whether a name is an item on the running server.
     *
     * @param name the item's name, such as {@code paper}
     * @return true for an item, false for air, a block-only material or an unknown name
     */
    private static boolean isItem(final String name) {
        Material material = Material.matchMaterial(name);

        return material != null && material.isItem() && !material.isAir();
    }
}
