package dev.vertcode.hyperion.bedrock;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.regex.Pattern;

import net.kyori.adventure.key.Key;

/**
 * The names a model goes by on Bedrock, and the UUID that tells Geyser which model an entity is.
 *
 * <p>Geyser sees only what the Java server sends, so Hyperion puts the model in the UUID of the
 * entity's spawn packet, for Bedrock players only: the top half is a fixed marker, the bottom
 * half the model's id and the entity's id. The Geyser extension reads the same layout.
 */
public final class BedrockIds {

    /**
     * The top half of every model UUID: "Hyperion" in ASCII.
     */
    public static final long MARKER = 0x4879706572696F6EL;

    private static final Pattern UNSAFE = Pattern.compile("[^a-z0-9_]");
    private static final int FNV_OFFSET = 0x811C9DC5;
    private static final int FNV_PRIME = 0x01000193;

    private BedrockIds() {
    }

    /**
     * Turn a model's key into a name Bedrock accepts: lowercase letters, digits and {@code _}.
     *
     * @param key the model's key
     * @return the name, such as {@code bosses_dragon}
     */
    public static String slug(final Key key) {
        String name = "hyperion".equals(key.namespace()) ? key.value() : key.namespace() + "_" + key.value();

        return UNSAFE.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("_");
    }

    /**
     * Return the Bedrock entity identifier of a model.
     *
     * @param key the model's key
     * @return the identifier, such as {@code hyperion:bosses_dragon}
     */
    public static String identifier(final Key key) {
        return "hyperion:" + slug(key);
    }

    /**
     * Work out a model's id, the same on every server for the same key.
     *
     * @param key the model's key
     * @return the id, a 32-bit FNV-1a hash of the key
     */
    public static int id(final Key key) {
        int hash = FNV_OFFSET;
        for (byte part : key.asString().getBytes(StandardCharsets.UTF_8)) {
            hash ^= part & 0xFF;
            hash *= FNV_PRIME;
        }

        return hash;
    }

    /**
     * Build the UUID that marks an entity as a model.
     *
     * @param model the model's id
     * @param entityId the entity's id, so every entity keeps its own UUID
     * @return the UUID
     */
    public static UUID uuid(final int model, final int entityId) {
        return new UUID(MARKER, ((long) model << 32) | (entityId & 0xFFFFFFFFL));
    }

    /**
     * Read the model from a UUID.
     *
     * @param uuid the UUID
     * @return the model's id, or empty when the UUID does not mark a model
     */
    public static OptionalInt model(final UUID uuid) {
        if (uuid.getMostSignificantBits() != MARKER) {
            return OptionalInt.empty();
        }

        return OptionalInt.of((int) (uuid.getLeastSignificantBits() >>> 32));
    }
}
