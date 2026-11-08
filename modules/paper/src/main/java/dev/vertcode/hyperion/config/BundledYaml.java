package dev.vertcode.hyperion.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Read a YAML file Hyperion ships in its jar and copies to the data folder.
 *
 * <p>The server's copy wins: a missing file is written from the jar, and a key a newer jar has
 * is added to the server's copy, but a value the server changed is never touched. When the data
 * folder cannot be written, the jar's values fill the gaps in memory and one line in the log
 * says why the file did not change.
 */
final class BundledYaml {

    private static final String WRITE_FAILED = "Could not write %s (%s); using the defaults from the jar for what it lacks."
            + " Give the server's user write access to the plugins/Hyperion folder.";

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
        if (Files.notExists(file) && !copy(plugin, name, file)) {
            return bundled(plugin, name);
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
                plugin.getLogger().warning(String.format(WRITE_FAILED, file, exception.getMessage()));
            }
        }

        return config;
    }

    /**
     * Copy a file from the jar into the data folder.
     *
     * @param plugin the plugin whose jar to read
     * @param name the file name in the jar
     * @param file where to write it
     * @return false when it could not be written, which is logged
     */
    private static boolean copy(final Plugin plugin, final String name, final Path file) {
        try (InputStream stream = plugin.getResource(name)) {
            if (stream == null) {
                return false;
            }

            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.copy(stream, file);

            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning(String.format(WRITE_FAILED, file, exception.getMessage()));
            return false;
        }
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
