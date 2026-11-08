package dev.vertcode.hyperion.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Small helpers for command output: clickable lines, and replies from any thread.
 */
public final class CommandText {

    private CommandText() {
    }

    /**
     * Make a line run a command when clicked.
     *
     * @param line the line
     * @param command the command to run, with the slash
     * @param hover what hovering shows
     * @return the clickable line
     */
    public static Component clickable(final Component line, final String command, final Component hover) {
        return line.clickEvent(ClickEvent.runCommand(command)).hoverEvent(HoverEvent.showText(hover));
    }

    /**
     * Send a message from any thread: to a player or entity on the region that owns it, to the
     * console on the global region. A player who left by then gets nothing.
     *
     * @param plugin the plugin scheduling the reply
     * @param sender who to reply to
     * @param message the message
     */
    public static void reply(final Plugin plugin, final CommandSender sender, final Component message) {
        if (sender instanceof Entity entity) {
            entity.getScheduler().run(plugin, scheduled -> sender.sendMessage(message), null);
            return;
        }

        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> sender.sendMessage(message));
    }
}
