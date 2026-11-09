package dev.vertcode.hyperion.command;

/**
 * The permission nodes of the {@code /hyperion} commands, one per command.
 *
 * <p>Every node is also declared in {@code paper-plugin.yml}, where {@code hyperion.admin} grants
 * them all.
 */
public final class HyperionPermissions {

    /**
     * See the help menu, through {@code /hyperion} or {@code /hyperion help}.
     */
    public static final String HELP = "hyperion.command.help";

    /**
     * List the loaded models, through {@code /hyperion list}.
     */
    public static final String LIST = "hyperion.command.list";

    /**
     * See everything about one model, through {@code /hyperion info}.
     */
    public static final String INFO = "hyperion.command.info";

    /**
     * Load the models again, through {@code /hyperion reload}.
     */
    public static final String RELOAD = "hyperion.command.reload";

    /**
     * See the warnings and errors from loading, through {@code /hyperion validate}.
     */
    public static final String VALIDATE = "hyperion.command.validate";

    /**
     * Build the resource pack again, through {@code /hyperion pack build}.
     */
    public static final String PACK_BUILD = "hyperion.command.pack.build";

    /**
     * Send the resource pack to players, through {@code /hyperion pack send}.
     */
    public static final String PACK_SEND = "hyperion.command.pack.send";

    /**
     * See how the resource pack is doing, through {@code /hyperion pack status}.
     */
    public static final String PACK_STATUS = "hyperion.command.pack.status";

    /**
     * Write the resource pack to the data folder, through {@code /hyperion pack export}.
     */
    public static final String PACK_EXPORT = "hyperion.command.pack.export";

    /**
     * Spawn a model or put one on an entity, through {@code /hyperion spawn} and
     * {@code /hyperion attach}.
     */
    public static final String SPAWN = "hyperion.command.spawn";

    /**
     * List the models nearby and remove them, through {@code /hyperion near} and
     * {@code /hyperion remove}.
     */
    public static final String REMOVE = "hyperion.command.remove";

    /**
     * Play and stop animations on the nearest model, through {@code /hyperion animate}.
     */
    public static final String ANIMATE = "hyperion.command.animate";

    /**
     * Play, stop and list effects, through {@code /hyperion effect}.
     */
    public static final String EFFECT = "hyperion.command.effect";

    /**
     * Show bones, hitboxes, locators, seats and effects, through {@code /hyperion debug}.
     */
    public static final String DEBUG = "hyperion.command.debug";

    /**
     * Browse, preview and spawn every model and effect, through {@code /hyperion gallery}.
     */
    public static final String GALLERY = "hyperion.command.gallery";

    /**
     * Open a model's menu by sneaking and right-clicking it.
     */
    public static final String MENU = "hyperion.menu";

    private HyperionPermissions() {
    }
}
