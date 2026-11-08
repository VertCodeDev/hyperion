package dev.vertcode.hyperion.command;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * The shared look of command output, and a way to reply from any thread.
 */
public final class CommandText {

    private CommandText() {
    }

    /**
     * Start a line with the Hyperion prefix.
     *
     * @param text the text after the prefix
     * @return the line
     */
    public static Component prefixed(final Component text) {
        return Component.text()
                .append(Component.text("Hyperion", HyperionCommands.PRIMARY))
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(text)
                .build();
    }

    /**
     * Write a heading such as {@code Bones (12)}.
     *
     * @param title the heading
     * @return the heading line
     */
    public static Component heading(final String title) {
        return Component.text(title, HyperionCommands.PRIMARY);
    }

    /**
     * Write a gray label and a white value, such as {@code Format: free}.
     *
     * @param label the label, without the colon
     * @param value the value
     * @return the row
     */
    public static Component row(final String label, final String value) {
        return Component.text()
                .append(Component.text(" " + label + ": ", NamedTextColor.GRAY))
                .append(Component.text(value, NamedTextColor.WHITE))
                .build();
    }

    /**
     * Make text run a command when clicked.
     *
     * @param text the text
     * @param command the command to run, with the slash
     * @param hover what hovering says the click does
     * @return the clickable text
     */
    public static Component clickable(final Component text, final String command, final String hover) {
        return text.clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY)));
    }

    /**
     * Say that no model is loaded under a key.
     *
     * @param key the key that was asked for
     * @return the message
     */
    public static Component notLoaded(final Key key) {
        return prefixed(Component.text("No model is loaded as " + key.asString() + ".", NamedTextColor.RED));
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
