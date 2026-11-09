package dev.vertcode.hyperion.menu.invui1;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.InvUI;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import dev.vertcode.hyperion.menu.MenuButton;
import dev.vertcode.hyperion.menu.MenuPage;
import dev.vertcode.hyperion.menu.MenuRenderer;

/**
 * Draws menus with InvUI 1. The plugin loads this class only on a 1.21.x server.
 */
public final class InvUi1Renderer implements MenuRenderer {

    private final Plugin plugin;

    /**
     * Set InvUI up for the plugin.
     *
     * @param plugin the plugin, which owns InvUI's listeners
     */
    public InvUi1Renderer(final Plugin plugin) {
        this.plugin = plugin;
        InvUI.getInstance().setPlugin(plugin);
    }

    @Override
    public void open(final Player player, final MenuPage page) {
        List<ButtonItem> buttons = new ArrayList<>();
        List<Item> content = new ArrayList<>();
        for (MenuButton button : page.content()) {
            ButtonItem item = new ButtonItem(this.plugin, button, buttons);
            buttons.add(item);
            content.add(item);
        }

        SimpleItem filler = new SimpleItem(new ItemWrapper(page.filler()));
        PagedGui.Builder<Item> gui = PagedGui.items()
                .setStructure(MenuPage.layout())
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', filler)
                .addIngredient('<', new PageArrow(false, page.previous()))
                .addIngredient('>', new PageArrow(true, page.next()));
        for (int i = 0; i < MenuPage.MAX_CONTROLS; i++) {
            char slot = (char) ('0' + i);
            if (i < page.controls().size()) {
                ButtonItem item = new ButtonItem(this.plugin, page.controls().get(i), buttons);
                buttons.add(item);
                gui.addIngredient(slot, item);
            } else {
                gui.addIngredient(slot, filler);
            }
        }

        gui.setContent(content);
        Window.single()
                .setViewer(player)
                .setTitle(new AdventureComponentWrapper(page.title()))
                .setGui(gui.build())
                .build()
                .open();
    }
}
