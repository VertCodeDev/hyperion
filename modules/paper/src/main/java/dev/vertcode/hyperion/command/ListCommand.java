package dev.vertcode.hyperion.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotation.specifier.FlagYielding;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.config.HyperionMessages;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion list [filter] [--page <page>]}: every loaded model, a page at a time, each a
 * click away from its info, with buttons to the pages before and after.
 */
public final class ListCommand {

    private static final int PAGE_SIZE = 10;

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
     * List one page of the loaded models whose key contains the filter.
     *
     * @param source who ran the command
     * @param filter the text a key must contain, or null for every model
     * @param page the page to show, 1 when left out
     */
    @Command(HyperionCommands.ROOT + " list [filter]")
    @CommandDescription("List every loaded model, optionally filtered")
    @Permission(HyperionPermissions.LIST)
    public void list(
            final Source source,
            @Argument("filter") @Greedy @FlagYielding final @Nullable String filter,
            @Flag("page") @Range(min = "1") final @Nullable Integer page
    ) {
        CommandSender sender = source.source();
        String needle = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        List<Blueprint> matches = new ArrayList<>();
        for (Blueprint blueprint : this.registry.all()) {
            if (blueprint.key().asString().contains(needle)) {
                matches.add(blueprint);
            }
        }

        if (matches.isEmpty()) {
            HyperionMessages empty = filter == null ? HyperionMessages.LIST__EMPTY : HyperionMessages.LIST__NO_MATCH;
            empty.send(sender, value("filter", String.valueOf(filter)));
            return;
        }

        int pages = (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int shown = page == null ? 1 : page;
        if (shown > pages) {
            HyperionMessages.LIST__NO_PAGE.send(sender, value("page", shown), value("pages", pages));
            return;
        }

        TextComponent.Builder message = Component.text()
                .append(HyperionMessages.LIST__HEADING.component(value("count", matches.size())));
        int first = (shown - 1) * PAGE_SIZE;
        for (Blueprint blueprint : matches.subList(first, Math.min(first + PAGE_SIZE, matches.size()))) {
            message.appendNewline().append(entry(blueprint));
        }

        if (pages > 1) {
            message.appendNewline().append(footer(filter, shown, pages));
        }

        sender.sendMessage(message.build());
    }

    /**
     * Write the line under a page: the page number between buttons to the pages around it.
     *
     * @param filter the filter in use, kept by the buttons, or null
     * @param page the page shown
     * @param pages how many pages there are
     * @return the line
     */
    private static Component footer(final @Nullable String filter, final int page, final int pages) {
        Component previous = page > 1
                ? button(HyperionMessages.LIST__PREVIOUS, filter, page - 1)
                : HyperionMessages.LIST__PREVIOUS_NONE.component();
        Component next = page < pages
                ? button(HyperionMessages.LIST__NEXT, filter, page + 1)
                : HyperionMessages.LIST__NEXT_NONE.component();

        return HyperionMessages.LIST__FOOTER.component(
                value("previous", previous),
                value("next", next),
                value("page", page),
                value("pages", pages)
        );
    }

    /**
     * Write a button that opens another page.
     *
     * @param label the button's text
     * @param filter the filter in use, or null
     * @param page the page it opens
     * @return the button
     */
    private static Component button(final HyperionMessages label, final @Nullable String filter, final int page) {
        String command = "/hyperion list " + (filter == null ? "" : filter + " ") + "--page " + page;
        Component hover = HyperionMessages.LIST__PAGE_HOVER.component(value("page", page));

        return CommandText.clickable(label.component(), command, hover);
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
