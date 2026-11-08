package dev.vertcode.hyperion.blueprint;

import java.util.Locale;
import java.util.Optional;

import net.kyori.adventure.key.Key;

/**
 * Turn a file's path inside the {@code models/} folder into the key it is registered under.
 *
 * <p>{@code bosses/Dragon Boss.bbmodel} becomes {@code hyperion:bosses/dragon_boss}: the
 * extension is dropped, letters are lowercased and spaces become underscores. Anything a key
 * still cannot hold after that makes the file unloadable, with an error that names it.
 */
public final class BlueprintKeys {

    /**
     * The namespace of every model in Hyperion's own folder.
     */
    public static final String NAMESPACE = "hyperion";

    private static final String CHANGED = "the file name is not a valid key, so it loads as '%s'; "
            + "rename the file to keep this name";
    private static final String INVALID = "'%s' cannot be a key: use only a to z, 0 to 9, '_', '-', '.' and '/'";

    private BlueprintKeys() {
    }

    /**
     * Work out the key for a model file.
     *
     * @param relativePath the path inside the {@code models/} folder, with {@code /} between
     *     folders
     * @param report where a renamed or invalid path is noted
     * @return the key, or empty when the path cannot become one
     */
    public static Optional<Key> fromPath(final String relativePath, final LoadReport report) {
        int dot = relativePath.lastIndexOf('.');
        String withoutExtension = dot > relativePath.lastIndexOf('/') ? relativePath.substring(0, dot) : relativePath;
        String value = withoutExtension.toLowerCase(Locale.ROOT).replace(' ', '_');
        if (!Key.parseableValue(value)) {
            report.error(String.format(INVALID, withoutExtension));
            return Optional.empty();
        }

        if (!value.equals(withoutExtension)) {
            report.warn("file", String.format(CHANGED, NAMESPACE + ":" + value));
        }

        return Optional.of(Key.key(NAMESPACE, value));
    }
}
