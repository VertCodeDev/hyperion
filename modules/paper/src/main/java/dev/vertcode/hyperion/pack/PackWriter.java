package dev.vertcode.hyperion.pack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
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
 * it again. The Bedrock pack is written the same way.
 */
public final class PackWriter {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final Map<String, byte[]> files = new TreeMap<>();

    /**
     * Add a JSON file.
     *
     * @param path the path inside the pack, such as {@code pack.mcmeta}
     * @param json the contents
     */
    public void json(final String path, final JsonElement json) {
        this.files.put(path, GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Add a file as it is, such as a texture.
     *
     * @param path the path inside the pack
     * @param bytes the contents, copied
     */
    public void bytes(final String path, final byte[] bytes) {
        this.files.put(path, bytes.clone());
    }

    /**
     * Return how many files the pack holds.
     *
     * @return the file count
     */
    public int size() {
        return this.files.size();
    }

    /**
     * Zip every file.
     *
     * @return the zip's bytes
     */
    public byte[] zip() {
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

    /**
     * Work out a zip's SHA-1, the hash clients check a pack by.
     *
     * @param zip the zip's bytes
     * @return the hash in lowercase hex
     */
    public static String sha1(final byte[] zip) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(zip));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Every Java runtime has SHA-1", exception);
        }
    }
}
