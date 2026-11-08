package dev.vertcode.hyperion.api.model.source;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.bukkit.plugin.Plugin;

/**
 * A model read from a file inside a plugin's jar.
 *
 * @param plugin the plugin whose jar holds the file
 * @param path the path inside the jar
 */
public record ResourceSource(Plugin plugin, String path) implements ModelSource {

    private static final String MISSING = "%s has no resource %s";

    @Override
    public String name() {
        int slash = this.path.lastIndexOf('/');

        return this.path.substring(slash + 1);
    }

    @Override
    public InputStream open() throws IOException {
        InputStream stream = this.plugin.getResource(this.path);
        if (stream == null) {
            throw new FileNotFoundException(String.format(MISSING, this.plugin.getName(), this.path));
        }

        return stream;
    }
}
