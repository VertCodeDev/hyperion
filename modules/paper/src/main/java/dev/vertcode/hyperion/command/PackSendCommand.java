package dev.vertcode.hyperion.command;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.bukkit.data.MultiplePlayerSelector;
import org.incendo.cloud.paper.util.sender.Source;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.pack.PackManager;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion pack send [players]}: offer the resource pack again.
 */
public final class PackSendCommand {

    private final PackManager packs;

    /**
     * Create the command.
     *
     * @param packs the pack to send
     */
    public PackSendCommand(final PackManager packs) {
        this.packs = packs;
    }

    /**
     * Offer the pack to some players, or to everyone online.
     *
     * @param source who ran the command
     * @param players the players, or null for everyone
     */
    @Command(HyperionCommands.ROOT + " pack send [players]")
    @CommandDescription("Offer the resource pack again, to some players or everyone")
    @Permission(HyperionPermissions.PACK_SEND)
    public void send(final Source source, @Argument("players") final @Nullable MultiplePlayerSelector players) {
        CommandSender sender = source.source();
        if (!this.packs.sends()) {
            HyperionMessages.PACK__SENDING_OFF.send(sender);
            return;
        }

        if (this.packs.current().isEmpty()) {
            HyperionMessages.PACK__NOT_BUILT.send(sender);
            return;
        }

        Collection<? extends Player> targets = players == null ? Bukkit.getOnlinePlayers() : players.values();
        HyperionMessages.PACK__SENT.send(sender, value("count", this.packs.sendTo(targets)));
    }
}
