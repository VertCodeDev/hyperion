package dev.vertcode.hyperion.pack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/**
 * Collect the files of a resource pack and zip them, the same bytes for the same files.
 *
 * <p>Files are written in path order with a fixed timestamp, so building the same models twice
 * gives the same zip and the same hash, and players who already have the pack do not download
 * it again.
 */
final class PackWriter {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final Map<String, byte[]> files = new TreeMap<>();

    /**
     * Add a JSON file.
     *
     * @param path the path inside the pack, such as {@code pack.mcmeta}
     * @param json the contents
     */
    void json(final String path, final JsonElement json) {
        this.files.put(path, GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Add a file as it is, such as a texture.
     *
     * @param path the path inside the pack
     * @param bytes the contents, copied
     */
    void bytes(final String path, final byte[] bytes) {
        this.files.put(path, bytes.clone());
    }

    /**
     * Return how many files the pack holds.
     *
     * @return the file count
     */
    int size() {
        return this.files.size();
    }

    /**
     * Zip every file.
     *
     * @return the zip's bytes
     */
    byte[] zip() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> file : this.files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setTimeLocal(TIMESTAMP);
                zip.putNextEntry(entry);
                zip.write(file.getValue());
                zip.closeEntry();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        return bytes.toByteArray();
    }
}
