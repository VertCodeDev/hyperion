package dev.vertcode.hyperion.command;

import java.util.Locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;

/**
 * {@code /hyperion list [filter]}: every loaded model, each a click away from its info.
 */
public final class ListCommand {

    private static final int MAX_LINES = 30;

    private final BlueprintRegistry registry;

    /**
     * Create the command.
     *
     * @param registry where the blueprints come from
     */
    public ListCommand(final BlueprintRegistry registry) {
        this.registry = registry;
    }

    /**
     * List the loaded models whose key contains the filter.
     *
     * @param source who ran the command
     * @param filter the text a key must contain, or null for every model
     */
    @Command(HyperionCommands.ROOT + " list [filter]")
    @CommandDescription("List every loaded model, optionally filtered")
    @Permission(HyperionPermissions.LIST)
    public void list(final Source source, @Argument("filter") @Greedy final @Nullable String filter) {
        CommandSender sender = source.source();
        String needle = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        TextComponent.Builder lines = Component.text();
        int shown = 0;
        int matches = 0;
        for (Blueprint blueprint : this.registry.all()) {
            String key = blueprint.key().asString();
            if (!key.contains(needle)) {
                continue;
            }

            matches++;
            if (shown < MAX_LINES) {
                lines.appendNewline().append(line(blueprint));
                shown++;
            }
        }

        if (matches == 0) {
            sender.sendMessage(CommandText.prefixed(Component.text(
                    filter == null
                            ? "No models are loaded. Put .bbmodel files in plugins/Hyperion/models and run /hy reload."
                            : "No model matches '" + filter + "'.",
                    NamedTextColor.GRAY
            )));
            return;
        }

        if (matches > shown) {
            String more = " ...and " + (matches - shown) + " more; add a filter to narrow the list";
            lines.appendNewline().append(Component.text(more, NamedTextColor.DARK_GRAY));
        }

        sender.sendMessage(CommandText.heading("Models (" + matches + ")").append(lines.build()));
    }

    /**
     * Write one model's line: its key, clickable, and its counts.
     *
     * @param blueprint the blueprint
     * @return the line
     */
    private static Component line(final Blueprint blueprint) {
        String key = blueprint.key().asString();
        String counts = "  " + blueprint.bones().size() + " bones, " + blueprint.animations().size()
                + " animations, " + blueprint.locators().size() + " locators";
        Component name = CommandText.clickable(
                Component.text(" " + key, NamedTextColor.WHITE),
                "/hyperion info " + key,
                "Show this model's info"
        );

        return name.append(Component.text(counts, NamedTextColor.GRAY));
    }
}
