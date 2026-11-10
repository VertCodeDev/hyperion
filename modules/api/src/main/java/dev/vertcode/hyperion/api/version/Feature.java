package dev.vertcode.hyperion.api.version;

/**
 * Something a newer Minecraft version added, named once with the version it arrived in.
 *
 * <p>Code asks the feature, not the version number, so the reason for a branch is in its name:
 *
 * <pre>{@code
 * String key = Feature.PLAIN_ATTRIBUTE_KEYS.available() ? "scale" : "generic.scale";
 * }</pre>
 *
 * <p>When a newer version adds something Hyperion uses, it gets a constant here, and every place
 * that cares asks it.
 */
public enum Feature {

    /**
     * Attribute keys without the {@code generic.} prefix, such as {@code scale}.
     */
    PLAIN_ATTRIBUTE_KEYS(1, 21, 2),

    /**
     * The client sends all its movement keys in one input packet, rather than a steering packet
     * with sideways and forward values.
     */
    PLAYER_INPUT_PACKET(1, 21, 2),

    /**
     * Any item can be worn through the {@code equippable} component, and armor draws from
     * equipment assets named by it, rather than from the armor material's fixed textures.
     */
    EQUIPPABLE(1, 21, 2),

    /**
     * Items render through item definitions in {@code assets/<namespace>/items}, chosen with the
     * {@code item_model} component, rather than custom model data overrides on a base item.
     */
    ITEM_MODEL_DEFINITIONS(1, 21, 4),

    /**
     * Packs declare the formats they support with {@code min_format} and {@code max_format}.
     */
    PACK_FORMAT_RANGE(1, 21, 9);

    // Plain numbers rather than a MinecraftVersion field, so the enum is plainly immutable.
    private final int major;
    private final int minor;
    private final int patch;

    Feature(final int major, final int minor, final int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Return the version this feature arrived in.
     *
     * @return the first version with it
     */
    public MinecraftVersion since() {
        return new MinecraftVersion(this.major, this.minor, this.patch);
    }

    /**
     * Return whether the server has this feature.
     *
     * @return true when the running version is at least {@link #since()}
     */
    public boolean available() {
        return this.availableOn(MinecraftVersion.current());
    }

    /**
     * Return whether a version has this feature.
     *
     * @param version the version to check
     * @return true when the version is at least {@link #since()}
     */
    public boolean availableOn(final MinecraftVersion version) {
        return version.atLeast(this.since());
    }
}
