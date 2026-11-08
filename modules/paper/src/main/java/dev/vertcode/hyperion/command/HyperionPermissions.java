package dev.vertcode.hyperion.command;

/**
 * The permission nodes of the {@code /hyperion} commands, one per command.
 *
 * <p>Every node is also declared in {@code plugin.yml}, where {@code hyperion.admin} grants
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

    private HyperionPermissions() {
    }
}
