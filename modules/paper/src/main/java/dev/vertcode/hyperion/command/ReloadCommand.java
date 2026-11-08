package dev.vertcode.hyperion.command;

import java.util.Map;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.bukkit.annotation.specifier.DefaultNamespace;
import org.incendo.cloud.paper.util.sender.Source;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintChanges;
import dev.vertcode.hyperion.blueprint.BlueprintKeys;
import dev.vertcode.hyperion.blueprint.BlueprintLoadException;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.blueprint.LoadIssue;
import dev.vertcode.hyperion.blueprint.LoadReport;
import dev.vertcode.hyperion.blueprint.ReloadResult;

/**
 * {@code /hyperion reload [model]}: load every model again, or one, and say what changed.
 *
 * <p>Loading runs on the async scheduler; the result comes back to whoever ran the command on
 * the thread that owns them.
 */
public final class ReloadCommand {

    private final Plugin plugin;
    private final BlueprintRegistry registry;

    /**
     * Create the command.
     *
     * @param plugin the plugin, for scheduling the reply
     * @param registry the blueprints to reload
     */
    public ReloadCommand(final Plugin plugin, final BlueprintRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    /**
     * Reload one model, or every model and the {@code models/} folder.
     *
     * @param source who ran the command
     * @param key the model to reload, or null for everything
     */
    @Command(HyperionCommands.ROOT + " reload [model]")
    @CommandDescription("Load every model again, or one, and show what changed")
    @Permission(HyperionPermissions.RELOAD)
    public void reload(
            final Source source,
            @Argument(value = "model", suggestions = BlueprintSuggestions.NAME)
            @DefaultNamespace(BlueprintKeys.NAMESPACE) final @Nullable NamespacedKey key
    ) {
        CommandSender sender = source.source();
        if (key == null) {
            sender.sendMessage(CommandText.prefixed(Component.text("Reloading every model...", NamedTextColor.GRAY)));
            this.registry.reloadAll().whenComplete((result, error) -> {
                Component reply = error == null ? summary(result) : failure(error);
                CommandText.reply(this.plugin, sender, reply);
            });
            return;
        }

        Key adventureKey = Key.key(key.namespace(), key.value());
        Blueprint before = this.registry.find(adventureKey).orElse(null);
        this.registry.reload(adventureKey).whenComplete((after, error) -> {
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
        TextComponent.Builder message = Component.text().append(CommandText.prefixed(Component.text(
                "Loaded " + result.loaded().size() + " model(s) in " + result.millis() + " ms, "
                        + result.failed().size() + " failed, " + result.warnings() + " warning(s).",
                NamedTextColor.WHITE
        )));

        for (Map.Entry<Key, BlueprintChanges> entry : result.changes().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                message.appendNewline().append(CommandText.row(entry.getKey().asString(), entry.getValue().describe()));
            }
        }

        for (Key removed : result.removed()) {
            message.appendNewline().append(CommandText.row(removed.asString(), "removed, the file is gone"));
        }

        for (LoadReport failed : result.failed()) {
            String line = " " + failed.source() + ": " + firstIssue(failed);
            message.appendNewline().append(Component.text(line, NamedTextColor.RED));
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

        return CommandText.prefixed(Component.text()
                .append(Component.text("Reloaded " + after.key().asString(), NamedTextColor.WHITE))
                .append(Component.text(" (revision " + after.revision() + "): " + changes, NamedTextColor.GRAY))
                .build());
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

        return CommandText.prefixed(Component.text("Reload failed: " + reason, NamedTextColor.RED));
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
