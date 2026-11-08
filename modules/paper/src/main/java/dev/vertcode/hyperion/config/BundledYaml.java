package dev.vertcode.hyperion.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Read a YAML file Hyperion ships in its jar and copies to the data folder.
 *
 * <p>The server's copy wins: a missing file is written from the jar, and a key a newer jar has
 * is added to the server's copy, but a value the server changed is never touched.
 */
final class BundledYaml {

    private static final String SAVE_FAILED = "Could not add the new keys to %s";

    private BundledYaml() {
    }

    /**
     * Read a file from the data folder, writing or completing it from the jar first.
     *
     * @param plugin the plugin whose data folder and jar to read
     * @param name the file name, the same in the data folder and the jar
     * @return the file's contents
     */
    static YamlConfiguration load(final Plugin plugin, final String name) {
        Path file = plugin.getDataPath().resolve(name);
        if (Files.notExists(file)) {
            plugin.saveResource(name, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
        YamlConfiguration defaults = bundled(plugin, name);
        boolean added = false;
        for (String key : defaults.getKeys(true)) {
            if (!defaults.isConfigurationSection(key) && !config.contains(key, true)) {
                config.set(key, defaults.get(key));
                added = true;
            }
        }

        if (added) {
            try {
                config.save(file.toFile());
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, String.format(SAVE_FAILED, name), exception);
            }
        }

        return config;
    }

    /**
     * Read a file bundled in the jar.
     *
     * @param plugin the plugin whose jar to read
     * @param name the file name
     * @return the bundled file, empty if the jar lacks it
     */
    private static YamlConfiguration bundled(final Plugin plugin, final String name) {
        InputStream stream = plugin.getResource(name);
        if (stream == null) {
            return new YamlConfiguration();
        }

        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            return new YamlConfiguration();
        }
    }
}
