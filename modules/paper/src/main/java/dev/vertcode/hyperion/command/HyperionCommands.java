package dev.vertcode.hyperion.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.Source;

import dev.vertcode.hyperion.HyperionPlugin;

/**
 * Build the Cloud command manager and register every {@code /hyperion} command.
 *
 * <p>Commands go through Paper's Brigadier api, so the client completes and colors them like
 * vanilla commands. Cloud runs a command on the thread that sent it: a player's command runs on
 * that player's region and the console's on the global region, so a command that touches
 * something elsewhere hops to its owner first.
 */
public final class HyperionCommands {

    /**
     * The root literal and its aliases, shared by every command class.
     */
    public static final String ROOT = "hyperion|hyp|hy";

    /**
     * The brand violet: names, headers and highlights.
     */
    public static final TextColor PRIMARY = TextColor.color(0x9B6BFF);

    /**
     * The lighter brand violet: values next to the primary color.
     */
    public static final TextColor HIGHLIGHT = TextColor.color(0xC9B2FF);

    private static final MinecraftHelp.HelpColors HELP_COLORS = MinecraftHelp.helpColors(
            PRIMARY,
            HIGHLIGHT,
            NamedTextColor.WHITE,
            NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY
    );

    private static final int HELP_PAGE_SIZE = 8;

    private HyperionCommands() {
    }

    /**
     * Build the command manager and register every command. Call once, from
     * {@code onEnable}.
     *
     * @param plugin the plugin that owns the commands
     */
    public static void register(final HyperionPlugin plugin) {
        PaperCommandManager<Source> manager = PaperCommandManager
                .builder(PaperSimpleSenderMapper.simpleSenderMapper())
                .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
                .buildOnEnable(plugin);

        MinecraftExceptionHandler.<Source>create(Source::source)
                .defaultHandlers()
                .decorator(message -> prefix().append(message))
                .registerTo(manager);

        MinecraftHelp<Source> help = help(manager);
        AnnotationParser<Source> parser = new AnnotationParser<>(manager, Source.class);
        parser.parse(new RootCommand(plugin, help), new HelpCommand(help));
    }

    /**
     * Build the paged help menu. It leaves out {@code /hyperion} itself, which only opens this
     * menu, and every command the reader has no permission for.
     *
     * @param manager the command manager to read the commands from
     * @return the help menu
     */
    private static MinecraftHelp<Source> help(final PaperCommandManager<Source> manager) {
        return MinecraftHelp.<Source>builder()
                .commandManager(manager)
                .audienceProvider(Source::source)
                .commandPrefix("/hyperion help")
                .commandFilter(command -> command.components().size() > 1)
                .colors(HELP_COLORS)
                .maxResultsPerPage(HELP_PAGE_SIZE)
                .build();
    }

    /**
     * Return the prefix put before every error message.
     *
     * @return the prefix
     */
    private static Component prefix() {
        return Component.text()
                .append(Component.text("Hyperion", PRIMARY))
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .build();
    }
}
