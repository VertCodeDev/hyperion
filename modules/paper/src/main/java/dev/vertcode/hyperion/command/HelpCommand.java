package dev.vertcode.hyperion.command;

import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.util.sender.Source;
import org.jspecify.annotations.Nullable;

/**
 * {@code /hyperion help [query]}: the paged help menu, filtered by a search.
 */
public final class HelpCommand {

    private final MinecraftHelp<Source> help;

    /**
     * Create the command.
     *
     * @param help the help menu to show
     */
    public HelpCommand(final MinecraftHelp<Source> help) {
        this.help = help;
    }

    /**
     * Show the commands that match a search, or every command when there is none. A page number
     * as the search opens that page.
     *
     * @param source who ran the command
     * @param query the search, or null for every command
     */
    @Command(HyperionCommands.ROOT + " help [query]")
    @CommandDescription("Browse every Hyperion command, or search for one")
    @Permission(HyperionPermissions.HELP)
    public void help(final Source source, @Argument("query") @Greedy final @Nullable String query) {
        this.help.queryCommands(query == null ? "" : query, source);
    }
}
