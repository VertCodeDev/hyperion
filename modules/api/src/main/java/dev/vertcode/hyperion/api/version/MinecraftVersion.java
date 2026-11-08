package dev.vertcode.hyperion.api.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Minecraft release such as {@code 1.21.4} or {@code 26.3}, compared like numbers.
 *
 * <p>Mojang moved from {@code 1.21.x} to year-based versions with 26.1; both compare correctly,
 * so 26.1 is newer than 1.21.11. Ask what the server runs with {@link #current()}:
 *
 * <pre>{@code
 * if (MinecraftVersion.current().atLeast(MinecraftVersion.V1_21_4)) {
 *     // item definitions
 * }
 * }</pre>
 *
 * <p>Most code asks a {@link Feature} instead, which names why the version matters.
 *
 * @param major the first number: 1, or the year for 26.1 and later
 * @param minor the second number
 * @param patch the third number, 0 when the version has none
 */
public record MinecraftVersion(int major, int minor, int patch) implements Comparable<MinecraftVersion> {

    /**
     * 1.21, the oldest version Hyperion supports.
     */
    public static final MinecraftVersion V1_21 = new MinecraftVersion(1, 21, 0);

    /**
     * 1.21.2: attribute keys lost their {@code generic.} prefix, and the client started sending
     * all its movement keys in one packet.
     */
    public static final MinecraftVersion V1_21_2 = new MinecraftVersion(1, 21, 2);

    /**
     * 1.21.4: items render through item definitions in {@code assets/<namespace>/items}.
     */
    public static final MinecraftVersion V1_21_4 = new MinecraftVersion(1, 21, 4);

    /**
     * 1.21.9: packs declare the formats they support as a range.
     */
    public static final MinecraftVersion V1_21_9 = new MinecraftVersion(1, 21, 9);

    private static final Pattern NUMBERS = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final String UNREADABLE = "'%s' is not a Minecraft version";

    /**
     * Read a version such as {@code 1.21}, {@code 1.21.4} or {@code 26.3}. Anything after the
     * numbers, such as {@code -rc1}, is ignored.
     *
     * @param text the version text
     * @return the version
     * @throws IllegalArgumentException if the text does not start with a version
     */
    public static MinecraftVersion parse(final String text) {
        Matcher matcher = NUMBERS.matcher(text.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException(String.format(UNREADABLE, text));
        }

        String patch = matcher.group(3);

        return new MinecraftVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                patch == null ? 0 : Integer.parseInt(patch)
        );
    }

    /**
     * Return the version the server runs. Read once, then cached.
     *
     * @return the running version
     */
    public static MinecraftVersion current() {
        return RunningVersion.VERSION;
    }

    /**
     * Return whether this version is the same as or newer than another.
     *
     * @param other the version to compare with
     * @return true when this version is at least the other
     */
    public boolean atLeast(final MinecraftVersion other) {
        return this.compareTo(other) >= 0;
    }

    @Override
    public int compareTo(final MinecraftVersion other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }

        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }

        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public String toString() {
        return this.patch == 0 ? this.major + "." + this.minor : this.major + "." + this.minor + "." + this.patch;
    }
}
