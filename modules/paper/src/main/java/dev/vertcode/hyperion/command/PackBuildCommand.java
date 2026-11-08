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
 * {@code /hyperion pack build}: build the resource pack again from the loaded models.
 */
public final class PackBuildCommand {

    private final Plugin plugin;
    private final PackManager packs;

    /**
     * Create the command.
     *
     * @param plugin the plugin, for scheduling the reply
     * @param packs the pack to build
     */
    public PackBuildCommand(final Plugin plugin, final PackManager packs) {
        this.plugin = plugin;
        this.packs = packs;
    }

    /**
     * Build the pack, and send it to everyone when it changed and resending is on.
     *
     * @param source who ran the command
     */
    @Command(HyperionCommands.ROOT + " pack build")
    @CommandDescription("Build the resource pack again from the loaded models")
    @Permission(HyperionPermissions.PACK_BUILD)
    public void build(final Source source) {
        CommandSender sender = source.source();
        HyperionMessages.PACK__BUILDING.send(sender);
        this.packs.rebuild().whenComplete((pack, error) -> CommandText.reply(this.plugin, sender, error == null
                ? HyperionMessages.PACK__BUILT.component(
                        value("size", pack.describeSize()),
                        value("files", pack.files()),
                        value("time", pack.millis()),
                        value("hash", pack.sha1().substring(0, 8))
                )
                : HyperionMessages.PACK__BUILD_FAILED.component(value("reason", String.valueOf(error.getMessage())))
        ));
    }
}
