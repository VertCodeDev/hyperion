package dev.vertcode.hyperion.ui;

import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.menu.MenuPage;
import dev.vertcode.hyperion.menu.MenuRenderer;

/**
 * Open menus with the InvUI module that fits the server: InvUI 1 on 1.21.x, InvUI 2 on 26.x.
 *
 * <p>The module is found by name, so the plugin never links against the one that does not fit.
 * When InvUI could not be downloaded or set up, menus say they are not available and the rest of
 * Hyperion works as before.
 */
public final class Menus {

    private static final MinecraftVersion V26 = new MinecraftVersion(26, 0, 0);
    private static final String INVUI_1 = "dev.vertcode.hyperion.menu.invui1.InvUi1Renderer";
    private static final String INVUI_2 = "dev.vertcode.hyperion.menu.invui2.InvUi2Renderer";
    private static final String LOAD_FAILED = "Menus are not available: InvUI could not be set up";

    private final @Nullable MenuRenderer renderer;

    private Menus(final @Nullable MenuRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Load the module for the server's version. Call once, on enable.
     *
     * @param plugin the plugin, which InvUI is set up with
     * @param version the server's version
     * @return the menus
     */
    public static Menus load(final Plugin plugin, final MinecraftVersion version) {
        String name = version.atLeast(V26) ? INVUI_2 : INVUI_1;
        try {
            MenuRenderer renderer = Class.forName(name)
                    .asSubclass(MenuRenderer.class)
                    .getConstructor(Plugin.class)
                    .newInstance(plugin);
            return new Menus(renderer);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, LOAD_FAILED, exception);
            return new Menus(null);
        }
    }

    /**
     * Open a menu for a player, or tell them menus are not available. Call on the thread that
     * owns the player.
     *
     * @param player the player
     * @param page the menu
     */
    public void open(final Player player, final MenuPage page) {
        if (this.renderer == null) {
            HyperionMessages.MENU__UNAVAILABLE.send(player);
            return;
        }

        this.renderer.open(player, page);
    }
}
