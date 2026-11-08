package dev.vertcode.hyperion.command;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;

import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.pack.PackManager;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion pack export}: write the pack into the data folder, to upload or merge by hand.
 */
public final class PackExportCommand {

    private final Plugin plugin;
    private final PackManager packs;

    /**
     * Create the command.
     *
     * @param plugin the plugin, for scheduling the reply
     * @param packs the pack to write
     */
    public PackExportCommand(final Plugin plugin, final PackManager packs) {
        this.plugin = plugin;
        this.packs = packs;
    }

    /**
     * Write the current pack to {@code plugins/Hyperion/hyperion-pack.zip}.
     *
     * @param source who ran the command
     */
    @Command(HyperionCommands.ROOT + " pack export")
    @CommandDescription("Write the resource pack into the data folder")
    @Permission(HyperionPermissions.PACK_EXPORT)
    public void export(final Source source) {
        CommandSender sender = source.source();
        if (this.packs.current().isEmpty()) {
            HyperionMessages.PACK__NOT_BUILT.send(sender);
            return;
        }

        this.packs.export().whenComplete((file, error) -> CommandText.reply(this.plugin, sender, error == null
                ? HyperionMessages.PACK__EXPORTED.component(value("file", file.toString()))
                : HyperionMessages.PACK__EXPORT_FAILED.component(value("reason", String.valueOf(error.getMessage())))
        ));
    }
}
