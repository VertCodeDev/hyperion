package dev.vertcode.hyperion.command;

import java.util.Map;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.bukkit.annotation.specifier.DefaultNamespace;
import org.incendo.cloud.paper.util.sender.Source;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.blueprint.BlueprintKeys;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.blueprint.IssueSeverity;
import dev.vertcode.hyperion.blueprint.LoadIssue;
import dev.vertcode.hyperion.blueprint.LoadReport;

/**
 * {@code /hyperion validate [model]}: the warnings and errors from the last load.
 */
public final class ValidateCommand {

    private static final int MAX_ISSUES = 30;

    private final BlueprintRegistry registry;

    /**
     * Create the command.
     *
     * @param registry where the load reports come from
     */
    public ValidateCommand(final BlueprintRegistry registry) {
        this.registry = registry;
    }

    /**
     * Show one model's issues, or a line for every model that has any.
     *
     * @param source who ran the command
     * @param key the model to check, or null for every model
     */
    @Command(HyperionCommands.ROOT + " validate [model]")
    @CommandDescription("Show the warnings and errors from loading the models")
    @Permission(HyperionPermissions.VALIDATE)
    public void validate(
            final Source source,
            @Argument(value = "model", suggestions = BlueprintSuggestions.NAME)
            @DefaultNamespace(BlueprintKeys.NAMESPACE) final @Nullable NamespacedKey key
    ) {
        CommandSender sender = source.source();
        if (key == null) {
            sender.sendMessage(this.overview());
            return;
        }

        LoadReport report = this.registry.report(Key.key(key.namespace(), key.value())).orElse(null);
        if (report == null) {
            sender.sendMessage(CommandText.notLoaded(key));
            return;
        }

        sender.sendMessage(details(key.asString(), report));
    }

    /**
     * Write a line for every model with issues, each a click away from its details.
     *
     * @return the message
     */
    private Component overview() {
        TextComponent.Builder lines = Component.text();
        int withIssues = 0;
        for (Map.Entry<Key, LoadReport> entry : this.registry.reports().entrySet()) {
            LoadReport report = entry.getValue();
            if (report.issues().isEmpty()) {
                continue;
            }

            withIssues++;
            String key = entry.getKey().asString();
            int errors = report.count(IssueSeverity.ERROR);
            int warnings = report.count(IssueSeverity.WARNING);
            Component counts = errors > 0
                    ? Component.text("  " + errors + " error(s), does not load", NamedTextColor.RED)
                    : Component.text("  " + warnings + " warning(s)", NamedTextColor.YELLOW);
            Component line = CommandText.clickable(
                    Component.text(" " + key, NamedTextColor.WHITE),
                    "/hyperion validate " + key,
                    "Show every issue"
            );
            lines.appendNewline().append(line.append(counts));
        }

        if (withIssues == 0) {
            int total = this.registry.reports().size();
            String text = "All " + total + " model(s) loaded without problems.";
            return CommandText.prefixed(Component.text(text, NamedTextColor.GREEN));
        }

        return CommandText.heading("Models with issues (" + withIssues + ")").append(lines.build());
    }

    /**
     * Write every issue of one model.
     *
     * @param key the model's key
     * @param report the report of its last load
     * @return the message
     */
    private static Component details(final String key, final LoadReport report) {
        if (report.issues().isEmpty()) {
            return CommandText.prefixed(Component.text(key + " loaded without problems.", NamedTextColor.GREEN));
        }

        TextComponent.Builder message = Component.text()
                .append(CommandText.heading(key + " (" + report.source() + ")"));
        int shown = 0;
        for (LoadIssue issue : report.issues()) {
            if (shown == MAX_ISSUES) {
                String more = " ...and " + (report.issues().size() - shown) + " more in the server log";
                message.appendNewline().append(Component.text(more, NamedTextColor.DARK_GRAY));
                break;
            }

            NamedTextColor color = issue.severity() == IssueSeverity.ERROR ? NamedTextColor.RED : NamedTextColor.YELLOW;
            message.appendNewline().append(Component.text(" " + issue.describe(), color));
            shown++;
        }

        return message.build();
    }
}
