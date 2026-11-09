package dev.vertcode.hyperion.command;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dev.vertcode.hyperion.effect.HyperionEffects;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion validate [model]}: the warnings and errors from the last load.
 */
public final class ValidateCommand {

    private static final int MAX_ISSUES = 30;

    private final BlueprintRegistry registry;
    private final HyperionEffects effects;

    /**
     * Create the command.
     *
     * @param registry where the models' load reports come from
     * @param effects where the effects' load reports come from
     */
    public ValidateCommand(final BlueprintRegistry registry, final HyperionEffects effects) {
        this.registry = registry;
        this.effects = effects;
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

        Component effectIssues = this.effectIssues();
        if (withIssues == 0 && effectIssues == null) {
            return HyperionMessages.VALIDATE__ALL_CLEAN.component(value("count", reports.size()));
        }

        TextComponent.Builder message = Component.text();
        if (withIssues > 0) {
            message.append(HyperionMessages.VALIDATE__HEADING.component(value("count", withIssues))).append(lines.build());
        }

        if (effectIssues != null) {
            if (withIssues > 0) {
                message.appendNewline();
            }

            message.append(effectIssues);
        }

        return message.build();
    }

    /**
     * Write every issue of the effect files and models' effects that have any.
     *
     * @return the message, or null when every effect loaded without problems
     */
    private @Nullable Component effectIssues() {
        TextComponent.Builder lines = Component.text();
        int files = 0;
        int shown = 0;
        for (Map.Entry<String, LoadReport> entry : new TreeMap<>(this.effects.reports()).entrySet()) {
            List<LoadIssue> issues = entry.getValue().issues();
            if (issues.isEmpty()) {
                continue;
            }

            files++;
            lines.appendNewline().append(HyperionMessages.VALIDATE__EFFECT_FILE.component(value("file", entry.getKey())));
            for (int i = 0; i < issues.size(); i++) {
                if (shown++ == MAX_ISSUES) {
                    lines.appendNewline().append(HyperionMessages.VALIDATE__MORE.component(value("count", issues.size() - i)));
                    break;
                }

                LoadIssue issue = issues.get(i);
                HyperionMessages line = issue.severity() == IssueSeverity.ERROR
                        ? HyperionMessages.VALIDATE__ERROR
                        : HyperionMessages.VALIDATE__WARNING;
                lines.appendNewline().append(line.component(value("issue", issue.describe())));
            }
        }

        if (files == 0) {
            return null;
        }

        return HyperionMessages.VALIDATE__EFFECTS_HEADING.component(value("count", files)).append(lines.build());
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
