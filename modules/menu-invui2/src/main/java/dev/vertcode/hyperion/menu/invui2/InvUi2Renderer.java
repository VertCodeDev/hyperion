package dev.vertcode.hyperion.menu.invui2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import xyz.xenondevs.invui.InvUI;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.window.Window;

import dev.vertcode.hyperion.menu.MenuButton;
import dev.vertcode.hyperion.menu.MenuClick;
import dev.vertcode.hyperion.menu.MenuPage;
import dev.vertcode.hyperion.menu.MenuRenderer;

/**
 * Draws menus with InvUI 2. The plugin loads this class only on a 26.x server. After a click every
 * button of the menu is drawn again, so a toggle and anything it affects show their new state.
 */
public final class InvUi2Renderer implements MenuRenderer {

    private final Plugin plugin;

    /**
     * Set InvUI up for the plugin.
     *
     * @param plugin the plugin, which owns InvUI's listeners
     */
    public InvUi2Renderer(final Plugin plugin) {
        this.plugin = plugin;
        InvUI.getInstance().setPlugin(plugin);
    }

    @Override
    public void open(final Player player, final MenuPage page) {
        List<Item> buttons = new ArrayList<>();
        List<Item> content = new ArrayList<>();
        for (MenuButton button : page.content()) {
            content.add(this.item(button, buttons));
        }

        AtomicReference<PagedGui<Item>> paged = new AtomicReference<>();
        PagedGui.Builder<Item> gui = PagedGui.itemsBuilder()
                .setStructure(MenuPage.layout())
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', page.filler())
                .addIngredient('<', arrow(page.previous(), paged, -1))
                .addIngredient('>', arrow(page.next(), paged, 1));
        for (int i = 0; i < MenuPage.MAX_CONTROLS; i++) {
            char slot = (char) ('0' + i);
            if (i < page.controls().size()) {
                gui.addIngredient(slot, this.item(page.controls().get(i), buttons));
            } else {
                gui.addIngredient(slot, page.filler());
            }
        }

        gui.setContent(content);
        PagedGui<Item> built = gui.build();
        paged.set(built);
        Window.builder()
                .setUpperGui(built)
                .setTitle(page.title())
                .setViewer(player)
                .build()
                .open();
    }

    /**
     * Turn a button into an item that redraws the whole menu after a click.
     *
     * @param button the button
     * @param buttons every button of the menu, which the item joins
     * @return the item
     */
    private Item item(final MenuButton button, final List<Item> buttons) {
        Item item = Item.builder()
                .setItemProvider(viewer -> new ItemBuilder(button.icon().get()))
                .addClickHandler(click -> {
                    Player player = click.player();
                    if (!button.action().click(player, MenuClick.of(click.clickType()))) {
                        player.getScheduler().run(this.plugin, task -> player.closeInventory(), null);
                        return;
                    }

                    for (Item each : buttons) {
                        each.notifyWindows();
                    }
                })
                .build();
        buttons.add(item);

        return item;
    }

    /**
     * Make an arrow that turns the page, staying within the pages there are.
     *
     * @param icon the item it shows
     * @param paged the paged gui, set once it is built
     * @param step -1 for the page before, 1 for the page after
     * @return the arrow
     */
    private static Item arrow(final ItemStack icon, final AtomicReference<PagedGui<Item>> paged, final int step) {
        ItemStack copy = icon.clone();

        return Item.builder()
                .setItemProvider(copy)
                .addClickHandler(click -> {
                    PagedGui<Item> gui = paged.get();
                    if (gui == null) {
                        return;
                    }

                    int target = gui.getPage() + step;
                    if (target >= 0 && target < gui.getPageCount()) {
                        gui.setPage(target);
                    }
                })
                .build();
    }
}
