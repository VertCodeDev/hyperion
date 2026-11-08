package dev.vertcode.hyperion.command;

import java.util.Locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
import dev.vertcode.hyperion.config.HyperionMessages;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

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
            if (!blueprint.key().asString().contains(needle)) {
                continue;
            }

            matches++;
            if (shown < MAX_LINES) {
                lines.appendNewline().append(entry(blueprint));
                shown++;
            }
        }

        if (matches == 0) {
            HyperionMessages empty = filter == null ? HyperionMessages.LIST__EMPTY : HyperionMessages.LIST__NO_MATCH;
            empty.send(sender, value("filter", String.valueOf(filter)));
            return;
        }

        if (matches > shown) {
            lines.appendNewline().append(HyperionMessages.LIST__MORE.component(value("count", matches - shown)));
        }

        sender.sendMessage(HyperionMessages.LIST__HEADING.component(value("count", matches)).append(lines.build()));
    }

    /**
     * Write one model's line, a click away from its info.
     *
     * @param blueprint the blueprint
     * @return the line
     */
    private static Component entry(final Blueprint blueprint) {
        String key = blueprint.key().asString();
        Component line = HyperionMessages.LIST__ENTRY.component(
                value("model", key),
                value("bones", blueprint.bones().size()),
                value("animations", blueprint.animations().size()),
                value("locators", blueprint.locators().size())
        );

        return CommandText.clickable(line, "/hyperion info " + key, HyperionMessages.LIST__ENTRY_HOVER.component());
    }
}
