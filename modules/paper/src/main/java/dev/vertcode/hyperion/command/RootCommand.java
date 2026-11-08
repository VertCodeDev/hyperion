package dev.vertcode.hyperion.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.util.sender.Source;

import dev.vertcode.hyperion.HyperionPlugin;

/**
 * {@code /hyperion}: open to everyone. Staff get the help menu; everyone else sees what Hyperion
 * is and where to find it.
 *
 * <p>The about card is the one text not in {@code messages.yml}, on purpose: it is how players
 * learn which plugin draws the models, so a server cannot remove it by accident. Its colors are
 * Hyperion's own for the same reason.
 */
public final class RootCommand {

    private static final String REPOSITORY = "https://github.com/VertCodeDev/hyperion";
    private static final TextColor VIOLET = TextColor.color(0x9B6BFF);
    private static final TextColor LIGHT_VIOLET = TextColor.color(0xC9B2FF);

    private final MinecraftHelp<Source> help;
    private final Component about;

    /**
     * Create the command.
     *
     * @param plugin the plugin, for its version
     * @param help the help menu staff get
     */
    public RootCommand(final HyperionPlugin plugin, final MinecraftHelp<Source> help) {
        this.help = help;
        this.about = about(plugin.getPluginMeta().getVersion());
    }

    /**
     * Show the help menu to staff and the about card to everyone else.
     *
     * @param source who ran the command
     */
    @Command(HyperionCommands.ROOT)
    @CommandDescription("Show what Hyperion is, or the help menu for staff")
    public void root(final Source source) {
        CommandSender sender = source.source();
        if (!sender.hasPermission(HyperionPermissions.HELP)) {
            sender.sendMessage(this.about);
            return;
        }

        this.help.queryCommands("", source);
    }

    /**
     * Build the about card.
     *
     * @param version the running version
     * @return the card
     */
    private static Component about(final String version) {
        Component link = Component.text("github.com/VertCodeDev/hyperion", VIOLET)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(REPOSITORY))
                .hoverEvent(HoverEvent.showText(Component.text("Open on GitHub", NamedTextColor.GRAY)));

        return Component.text()
                .append(Component.text("Hyperion", VIOLET, TextDecoration.BOLD))
                .append(Component.text(" v" + version, NamedTextColor.DARK_GRAY))
                .appendNewline()
                .append(Component.text("The models here are powered by Hyperion,", NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("a free and open source Blockbench model plugin.", NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("Made by ", NamedTextColor.GRAY))
                .append(Component.text("VertCode Development", LIGHT_VIOLET))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(link)
                .build();
    }
}
