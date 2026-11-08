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

import dev.vertcode.hyperion.HyperionPlugin;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintChanges;
import dev.vertcode.hyperion.blueprint.BlueprintKeys;
import dev.vertcode.hyperion.blueprint.BlueprintLoadException;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.blueprint.LoadIssue;
import dev.vertcode.hyperion.blueprint.LoadReport;
import dev.vertcode.hyperion.blueprint.ReloadResult;
import dev.vertcode.hyperion.config.HyperionMessages;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion reload [model]}: load every model and {@code messages.yml} again, or one
 * model, and say what changed.
 *
 * <p>Loading runs on the async scheduler; the result comes back to whoever ran the command on
 * the thread that owns them.
 */
public final class ReloadCommand {

    private final HyperionPlugin plugin;
    private final BlueprintRegistry registry;

    /**
     * Create the command.
     *
     * @param plugin the plugin, for reloading messages and scheduling the reply
     * @param registry the blueprints to reload
     */
    public ReloadCommand(final HyperionPlugin plugin, final BlueprintRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    /**
     * Reload one model, or every model, the {@code models/} folder and the messages.
     *
     * @param source who ran the command
     * @param key the model to reload, or null for everything
     */
    @Command(HyperionCommands.ROOT + " reload [model]")
    @CommandDescription("Load every model and the messages again, or one model")
    @Permission(HyperionPermissions.RELOAD)
    public void reload(
            final Source source,
            @Argument(value = "model", suggestions = BlueprintSuggestions.NAME)
            @DefaultNamespace(BlueprintKeys.NAMESPACE) final @Nullable NamespacedKey key
    ) {
        CommandSender sender = source.source();
        if (key == null) {
            HyperionMessages.RELOAD__STARTED.send(sender);
            this.registry.reloadAll()
                    .thenApply(result -> {
                        this.plugin.reloadMessages();
                        return result;
                    })
                    .whenComplete((result, error) -> {
                        Component reply = error == null ? summary(result) : failure(error);
                        CommandText.reply(this.plugin, sender, reply);
                    });
            return;
        }

        Key model = Key.key(key.namespace(), key.value());
        Blueprint before = this.registry.find(model).orElse(null);
        this.registry.reload(model).whenComplete((after, error) -> {
            Component reply = error == null ? reloaded(before, after) : failure(error);
            CommandText.reply(this.plugin, sender, reply);
        });
    }

    /**
     * Describe a full reload.
     *
     * @param result what the reload did
     * @return the message
     */
    private static Component summary(final ReloadResult result) {
        TextComponent.Builder message = Component.text().append(HyperionMessages.RELOAD__DONE.component(
                value("loaded", result.loaded().size()),
                value("time", result.millis()),
                value("failed", result.failed().size()),
                value("warnings", result.warnings())
        ));

        for (Map.Entry<Key, BlueprintChanges> entry : result.changes().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                message.appendNewline().append(HyperionMessages.RELOAD__CHANGED.component(
                        value("model", entry.getKey().asString()),
                        value("changes", entry.getValue().describe())
                ));
            }
        }

        for (Key removed : result.removed()) {
            Component line = HyperionMessages.RELOAD__REMOVED.component(value("model", removed.asString()));
            message.appendNewline().append(line);
        }

        for (LoadReport failed : result.failed()) {
            message.appendNewline().append(HyperionMessages.RELOAD__FILE_FAILED.component(
                    value("file", failed.source()),
                    value("reason", firstIssue(failed))
            ));
        }

        return message.build();
    }

    /**
     * Describe a single reload.
     *
     * @param before the blueprint before the reload, or null when it was not loaded
     * @param after the blueprint after the reload
     * @return the message
     */
    private static Component reloaded(final @Nullable Blueprint before, final Blueprint after) {
        String changes = before == null ? "loaded" : BlueprintChanges.between(before, after).describe();

        return HyperionMessages.RELOAD__MODEL_DONE.component(
                value("model", after.key().asString()),
                value("revision", after.revision()),
                value("changes", changes)
        );
    }

    /**
     * Describe a reload that failed.
     *
     * @param error what went wrong
     * @return the message
     */
    private static Component failure(final Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String reason = cause instanceof BlueprintLoadException load
                ? firstIssue(load.report())
                : String.valueOf(cause.getMessage());

        return HyperionMessages.RELOAD__FAILED.component(value("reason", reason));
    }

    /**
     * Return the first issue of a report, the reason it failed.
     *
     * @param report the report
     * @return the first issue, described
     */
    private static String firstIssue(final LoadReport report) {
        for (LoadIssue issue : report.issues()) {
            return issue.describe();
        }

        return "unknown error";
    }
}
