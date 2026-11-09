package dev.vertcode.hyperion.menu.invui1;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import dev.vertcode.hyperion.menu.MenuButton;
import dev.vertcode.hyperion.menu.MenuClick;

/**
 * One menu button as an InvUI 1 item. After a click every button of the menu is drawn again,
 * so a toggle and anything it affects show their new state.
 */
final class ButtonItem extends AbstractItem {

    private final Plugin plugin;
    private final MenuButton button;
    private final List<ButtonItem> menu;

    /**
     * Wrap a button.
     *
     * @param plugin the plugin, to close the menu on the player's thread
     * @param button the button
     * @param menu every button of the menu, redrawn after a click
     */
    ButtonItem(final Plugin plugin, final MenuButton button, final List<ButtonItem> menu) {
        this.plugin = plugin;
        this.button = button;
        this.menu = menu;
    }

    @Override
    public ItemProvider getItemProvider() {
        return new ItemWrapper(this.button.icon().get());
    }

    @Override
    public void handleClick(final ClickType type, final Player player, final InventoryClickEvent event) {
        if (!this.button.action().click(player, MenuClick.of(type))) {
            player.getScheduler().run(this.plugin, task -> player.closeInventory(), null);
            return;
        }

        for (ButtonItem item : this.menu) {
            item.notifyWindows();
        }
    }
}
