package dev.vertcode.hyperion.command;

import java.util.Map;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
import dev.vertcode.hyperion.config.HyperionMessages;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

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
            HyperionMessages.MODEL_NOT_LOADED.send(sender, value("model", key.asString()));
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
        Map<Key, LoadReport> reports = this.registry.reports();
        TextComponent.Builder lines = Component.text();
        int withIssues = 0;
        for (Map.Entry<Key, LoadReport> entry : reports.entrySet()) {
            LoadReport report = entry.getValue();
            if (report.issues().isEmpty()) {
                continue;
            }

            withIssues++;
            String key = entry.getKey().asString();
            int errors = report.count(IssueSeverity.ERROR);
            Component line = errors > 0
                    ? HyperionMessages.VALIDATE__ENTRY_ERRORS.component(value("model", key), value("count", errors))
                    : HyperionMessages.VALIDATE__ENTRY_WARNINGS.component(
                            value("model", key),
                            value("count", report.count(IssueSeverity.WARNING))
                    );
            lines.appendNewline().append(CommandText.clickable(
                    line,
                    "/hyperion validate " + key,
                    HyperionMessages.VALIDATE__ENTRY_HOVER.component()
            ));
        }

        if (withIssues == 0) {
            return HyperionMessages.VALIDATE__ALL_CLEAN.component(value("count", reports.size()));
        }

        return HyperionMessages.VALIDATE__HEADING.component(value("count", withIssues)).append(lines.build());
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
            return HyperionMessages.VALIDATE__MODEL_CLEAN.component(value("model", key));
        }

        TextComponent.Builder message = Component.text().append(HyperionMessages.VALIDATE__MODEL_HEADING.component(
                value("model", key),
                value("file", report.source())
        ));
        for (int i = 0; i < report.issues().size(); i++) {
            if (i == MAX_ISSUES) {
                int more = report.issues().size() - i;
                message.appendNewline().append(HyperionMessages.VALIDATE__MORE.component(value("count", more)));
                break;
            }

            LoadIssue issue = report.issues().get(i);
            HyperionMessages line = issue.severity() == IssueSeverity.ERROR
                    ? HyperionMessages.VALIDATE__ERROR
                    : HyperionMessages.VALIDATE__WARNING;
            message.appendNewline().append(line.component(value("issue", issue.describe())));
        }

        return message.build();
    }
}
