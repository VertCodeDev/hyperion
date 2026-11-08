package dev.vertcode.hyperion.config;

import java.util.Optional;

/**
 * How players get the resource pack, the {@code pack.hosting} setting.
 */
public enum PackHosting {

    /**
     * Hyperion serves the pack from a small web server on this machine.
     */
    SELF_HOST("self-host"),

    /**
     * The server owner uploads the zip and sets the link.
     */
    URL("url"),

    /**
     * Hyperion sends nothing; the owner merges the zip into their own pack.
     */
    OFF("off");

    private final String key;

    PackHosting(final String key) {
        this.key = key;
    }

    /**
     * Return the name written in {@code config.yml}.
     *
     * @return the name, such as {@code self-host}
     */
    public String key() {
        return this.key;
    }

    /**
     * Find the hosting written under a name.
     *
     * @param key the name in {@code config.yml}
     * @return the hosting, or empty for an unknown name
     */
    public static Optional<PackHosting> fromKey(final String key) {
        for (PackHosting hosting : values()) {
            if (hosting.key.equalsIgnoreCase(key.trim())) {
                return Optional.of(hosting);
            }
        }

        return Optional.empty();
    }
}
