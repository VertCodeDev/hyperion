package dev.vertcode.hyperion.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;

import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.pack.PackManager;
import dev.vertcode.hyperion.pack.PackStatus;
import dev.vertcode.hyperion.pack.ResourcePack;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion pack status}: how the pack is hosted, and how players are doing with it.
 */
public final class PackStatusCommand {

    private final PackManager packs;

    /**
     * Create the command.
     *
     * @param packs the pack to report on
     */
    public PackStatusCommand(final PackManager packs) {
        this.packs = packs;
    }

    /**
     * Show the hosting, the link, the pack's size and hash, and the players' progress.
     *
     * @param source who ran the command
     */
    @Command(HyperionCommands.ROOT + " pack status")
    @CommandDescription("Show how the resource pack is hosted and how players are doing with it")
    @Permission(HyperionPermissions.PACK_STATUS)
    public void status(final Source source) {
        CommandSender sender = source.source();
        ResourcePack pack = this.packs.current().orElse(null);
        if (pack == null) {
            HyperionMessages.PACK__NOT_BUILT.send(sender);
            return;
        }

        PackStatus status = this.packs.status();
        String hosting = this.packs.settings().hosting().key();
        TextComponent.Builder message = Component.text()
                .append(HyperionMessages.PACK__STATUS_HEADING.component())
                .appendNewline()
                .append(HyperionMessages.PACK__STATUS_HOSTING.component(value("hosting", hosting)));
        if (sender instanceof Player player) {
            this.packs.link(player).ifPresent(link -> message.appendNewline()
                    .append(HyperionMessages.PACK__STATUS_URL.component(value("url", link))));
        }

        message.appendNewline().append(HyperionMessages.PACK__STATUS_PACK.component(
                value("size", pack.describeSize()),
                value("files", pack.files()),
                value("hash", pack.sha1())
        ));
        message.appendNewline().append(HyperionMessages.PACK__STATUS_PLAYERS.component(
                value("loaded", status.loaded()),
                value("pending", status.pending()),
                value("declined", status.declined()),
                value("failed", status.failed())
        ));
        sender.sendMessage(message.build());
    }
}
