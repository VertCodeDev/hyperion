package dev.vertcode.hyperion.api.model.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.bukkit.plugin.Plugin;

/**
 * Where a model's bytes come from. Read again on every reload.
 *
 * <p>Use the factories for files, jar resources and bytes in memory, or implement it for
 * anything else, such as a download.
 */
public interface ModelSource {

    /**
     * Read a model from a file.
     *
     * @param path the file
     * @return the source
     */
    static ModelSource file(final Path path) {
        return new FileSource(path);
    }

    /**
     * Read a model from a file inside a plugin's jar.
     *
     * @param plugin the plugin whose jar holds the file
     * @param path the path inside the jar, such as {@code models/dragon.bbmodel}
     * @return the source
     */
    static ModelSource resource(final Plugin plugin, final String path) {
        return new ResourceSource(plugin, path);
    }

    /**
     * Read a model from bytes already in memory.
     *
     * @param name a file name for the format check and for errors, such as {@code dragon.bbmodel}
     * @param data the bytes, copied
     * @return the source
     */
    static ModelSource bytes(final String name, final byte[] data) {
        return new BytesSource(name, data);
    }

    /**
     * Return a file name for this source, used to pick a format and in error messages.
     *
     * @return the name, with its extension
     */
    String name();

    /**
     * Open the model's bytes. The caller closes the stream. Called off the server thread.
     *
     * @return a fresh stream over the bytes
     * @throws IOException if the bytes cannot be read
     */
    InputStream open() throws IOException;
}
