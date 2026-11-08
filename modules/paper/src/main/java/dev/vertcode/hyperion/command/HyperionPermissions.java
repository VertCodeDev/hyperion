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

    private HyperionPermissions() {
    }
}
