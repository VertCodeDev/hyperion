package dev.vertcode.hyperion.command;

import java.util.Map;
import java.util.logging.Level;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.exception.InvalidCommandSenderException;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.exception.NoPermissionException;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.Source;

import dev.vertcode.hyperion.HyperionPlugin;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.config.MessagePalette;
import dev.vertcode.hyperion.pack.PackManager;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * Build the Cloud command manager and register every {@code /hyperion} command.
 *
 * <p>Commands go through Paper's Brigadier api, so the client completes and colors them like
 * vanilla commands. Cloud runs a command on the thread that sent it: a player's command runs on
 * that player's region and the console's on the global region, so a command that touches
 * something elsewhere hops to its owner first. Every text, Cloud's own errors and help menu
 * included, comes from {@code messages.yml}.
 */
public final class HyperionCommands {

    /**
     * The root literal and its aliases, shared by every command class.
     */
    public static final String ROOT = "hyperion|hyp|hy";

    private static final int HELP_PAGE_SIZE = 8;
    private static final String COMMAND_FAILED = "A /hyperion command failed";

    private HyperionCommands() {
    }

    /**
     * Build the command manager and register every command. Call once, from
     * {@code onEnable}, after the messages are installed.
     *
     * @param plugin the plugin that owns the commands
     * @param registry the loaded blueprints the commands show and reload
     * @param packs the resource pack the pack commands build and send
     */
    public static void register(
            final HyperionPlugin plugin,
            final BlueprintRegistry registry,
            final PackManager packs
    ) {
        PaperCommandManager<Source> manager = PaperCommandManager
                .builder(PaperSimpleSenderMapper.simpleSenderMapper())
                .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
                .buildOnEnable(plugin);

        MinecraftExceptionHandler.<Source>create(Source::source)
                .handler(InvalidSyntaxException.class, (formatter, context) -> usage(context.exception()))
                .handler(ArgumentParseException.class, (formatter, context) -> invalidArgument(context.exception()))
                .handler(NoPermissionException.class, (formatter, context) ->
                        HyperionMessages.COMMAND__NO_PERMISSION.component())
                .handler(InvalidCommandSenderException.class, (formatter, context) ->
                        HyperionMessages.COMMAND__PLAYERS_ONLY.component())
                .handler(CommandExecutionException.class, (formatter, context) -> {
                    plugin.getLogger().log(Level.SEVERE, COMMAND_FAILED, context.exception().getCause());
                    return HyperionMessages.COMMAND__FAILED.component();
                })
                .registerTo(manager);

        MinecraftHelp<Source> help = help(manager);
        AnnotationParser<Source> parser = new AnnotationParser<>(manager, Source.class);
        parser.parse(
                new RootCommand(plugin, help),
                new HelpCommand(help),
                new BlueprintSuggestions(registry),
                new ListCommand(registry),
                new InfoCommand(registry),
                new ReloadCommand(plugin, registry, packs),
                new ValidateCommand(registry),
                new PackBuildCommand(plugin, packs),
                new PackSendCommand(packs),
                new PackStatusCommand(packs),
                new PackExportCommand(plugin, packs)
        );
    }

    /**
     * Tell the sender how the command they mistyped is used.
     *
     * @param exception what Cloud found wrong
     * @return the message
     */
    private static Component usage(final InvalidSyntaxException exception) {
        return HyperionMessages.COMMAND__USAGE.component(value("syntax", "/" + exception.correctSyntax()));
    }

    /**
     * Tell the sender why an argument could not be read, in Cloud's words.
     *
     * @param exception what Cloud found wrong
     * @return the message
     */
    private static Component invalidArgument(final ArgumentParseException exception) {
        Throwable cause = exception.getCause();
        String reason = cause == null ? exception.getMessage() : cause.getMessage();

        return HyperionMessages.COMMAND__INVALID_ARGUMENT.component(value("reason", String.valueOf(reason)));
    }

    /**
     * Build the paged help menu, in the palette's colors and with its texts from
     * {@code messages.yml}. It leaves out {@code /hyperion} itself, which only opens this menu,
     * and every command the reader has no permission for.
     *
     * <p>The colors are read once here, so a palette change reaches the help menu on the next
     * restart; its texts follow {@code /hy reload}.
     *
     * @param manager the command manager to read the commands from
     * @return the help menu
     */
    private static MinecraftHelp<Source> help(final PaperCommandManager<Source> manager) {
        MessagePalette palette = HyperionMessages.palette();

        return MinecraftHelp.<Source>builder()
                .commandManager(manager)
                .audienceProvider(Source::source)
                .commandPrefix("/hyperion help")
                .commandFilter(command -> command.components().size() > 1)
                .colors(MinecraftHelp.helpColors(
                        palette.primary(),
                        palette.highlight(),
                        palette.text(),
                        palette.muted(),
                        palette.dim()
                ))
                .messageProvider((sender, key, arguments) -> helpText(key, arguments))
                .maxResultsPerPage(HELP_PAGE_SIZE)
                .build();
    }

    /**
     * Render one of the help menu's texts.
     *
     * @param key Cloud's name for the text
     * @param arguments the values the text fills in, such as {@code page}
     * @return the text
     */
    private static Component helpText(final String key, final Map<String, String> arguments) {
        HyperionMessages message = switch (key) {
            case MinecraftHelp.MESSAGE_HELP_TITLE -> HyperionMessages.HELP__TITLE;
            case MinecraftHelp.MESSAGE_COMMAND -> HyperionMessages.HELP__COMMAND;
            case MinecraftHelp.MESSAGE_DESCRIPTION -> HyperionMessages.HELP__DESCRIPTION;
            case MinecraftHelp.MESSAGE_NO_DESCRIPTION -> HyperionMessages.HELP__NO_DESCRIPTION;
            case MinecraftHelp.MESSAGE_ARGUMENTS -> HyperionMessages.HELP__ARGUMENTS;
            case MinecraftHelp.MESSAGE_OPTIONAL -> HyperionMessages.HELP__OPTIONAL;
            case MinecraftHelp.MESSAGE_SHOWING_RESULTS_FOR_QUERY -> HyperionMessages.HELP__SHOWING_RESULTS;
            case MinecraftHelp.MESSAGE_NO_RESULTS_FOR_QUERY -> HyperionMessages.HELP__NO_RESULTS;
            case MinecraftHelp.MESSAGE_AVAILABLE_COMMANDS -> HyperionMessages.HELP__AVAILABLE_COMMANDS;
            case MinecraftHelp.MESSAGE_CLICK_TO_SHOW_HELP -> HyperionMessages.HELP__CLICK_TO_SHOW_HELP;
            case MinecraftHelp.MESSAGE_PAGE_OUT_OF_RANGE -> HyperionMessages.HELP__PAGE_OUT_OF_RANGE;
            case MinecraftHelp.MESSAGE_CLICK_FOR_NEXT_PAGE -> HyperionMessages.HELP__NEXT_PAGE;
            case MinecraftHelp.MESSAGE_CLICK_FOR_PREVIOUS_PAGE -> HyperionMessages.HELP__PREVIOUS_PAGE;
            default -> null;
        };

        if (message == null) {
            return Component.text(key);
        }

        TagResolver[] values = arguments.entrySet().stream()
                .map(entry -> value(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);

        return message.component(values);
    }
}
