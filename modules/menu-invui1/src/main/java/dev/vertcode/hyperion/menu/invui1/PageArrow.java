package dev.vertcode.hyperion.menu.invui1;

import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

/**
 * An arrow to the page before or after. InvUI turns the page when it is clicked.
 */
final class PageArrow extends PageItem {

    private final ItemStack icon;

    /**
     * Create an arrow.
     *
     * @param forward true for the page after, false for the page before
     * @param icon the item it shows
     */
    PageArrow(final boolean forward, final ItemStack icon) {
        super(forward);
        this.icon = icon.clone();
    }

    @Override
    public ItemProvider getItemProvider(final PagedGui<?> gui) {
        return new ItemWrapper(this.icon);
    }
}
