package dev.vertcode.hyperion.pack;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.version.MinecraftVersion;

/**
 * The resource pack format a Minecraft version reads, such as 46 for 1.21.4.
 *
 * <p>The running server knows its own format: its jar carries a {@code version.json} with it, so
 * a version Hyperion has never heard of still gets the right number. The table here is only the
 * fallback for a server whose jar lacks that file.
 *
 * @param major the format number
 * @param minor the minor format, 0 before 1.21.9 introduced it
 */
public record PackFormat(int major, int minor) {

    private static final String VERSION_FILE = "version.json";

    /**
     * Work out the format of the running server.
     *
     * @param version the server's version, for the fallback table
     * @param loader the class loader that loaded the server, which holds its {@code version.json}
     * @return the format
     */
    public static PackFormat ofServer(final MinecraftVersion version, final ClassLoader loader) {
        try (InputStream stream = loader.getResourceAsStream(VERSION_FILE)) {
            if (stream != null) {
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    Optional<PackFormat> format = fromVersionJson(JsonParser.parseReader(reader));
                    if (format.isPresent()) {
                        return format.get();
                    }
                }
            }
        } catch (IOException | JsonParseException exception) {
            // An unreadable file falls through to the table.
        }

        return known(version);
    }

    /**
     * Read the resource pack format from a server's {@code version.json}. Versions before 1.21.9
     * write {@code "resource": 46}; later ones write {@code "resource_major"} and
     * {@code "resource_minor"}.
     *
     * @param json the parsed file
     * @return the format, or empty when the file does not say
     */
    public static Optional<PackFormat> fromVersionJson(final JsonElement json) {
        if (!(json instanceof JsonObject root) || !(root.get("pack_version") instanceof JsonObject pack)) {
            return Optional.empty();
        }

        Integer major = number(pack.get("resource_major"));
        if (major != null) {
            Integer minor = number(pack.get("resource_minor"));
            return Optional.of(new PackFormat(major, minor == null ? 0 : minor));
        }

        Integer resource = number(pack.get("resource"));

        return resource == null ? Optional.empty() : Optional.of(new PackFormat(resource, 0));
    }

    /**
     * Return the format of a version from the table, the newest known one for a newer version.
     *
     * @param version the version
     * @return the format
     */
    public static PackFormat known(final MinecraftVersion version) {
        if (version.atLeast(new MinecraftVersion(26, 1, 0))) {
            return new PackFormat(84, 0);
        }

        if (version.atLeast(new MinecraftVersion(1, 21, 11))) {
            return new PackFormat(75, 0);
        }

        if (version.atLeast(MinecraftVersion.V1_21_9)) {
            return new PackFormat(69, 0);
        }

        if (version.atLeast(new MinecraftVersion(1, 21, 7))) {
            return new PackFormat(64, 0);
        }

        if (version.atLeast(new MinecraftVersion(1, 21, 6))) {
            return new PackFormat(63, 0);
        }

        if (version.atLeast(new MinecraftVersion(1, 21, 5))) {
            return new PackFormat(55, 0);
        }

        if (version.atLeast(MinecraftVersion.V1_21_4)) {
            return new PackFormat(46, 0);
        }

        if (version.atLeast(MinecraftVersion.V1_21_2)) {
            return new PackFormat(42, 0);
        }

        return new PackFormat(34, 0);
    }

    /**
     * Read a whole number from a JSON element.
     *
     * @param element the element, or null when missing
     * @return the number, or null when the element is not a number
     */
    private static @Nullable Integer number(final @Nullable JsonElement element) {
        return element instanceof JsonPrimitive primitive && primitive.isNumber() ? primitive.getAsInt() : null;
    }
}
