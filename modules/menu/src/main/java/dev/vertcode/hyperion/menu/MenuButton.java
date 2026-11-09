package dev.vertcode.hyperion.menu;

import java.util.function.Supplier;

import org.bukkit.inventory.ItemStack;

/**
 * One button: the item it shows, worked out again after every click, and what clicking it does.
 *
 * @param icon makes the item the button shows
 * @param action what clicking it does
 */
public record MenuButton(Supplier<ItemStack> icon, MenuAction action) {

    /**
     * Make a button that only shows an item.
     *
     * @param icon the item
     * @return the button
     */
    public static MenuButton display(final ItemStack icon) {
        ItemStack copy = icon.clone();

        return new MenuButton(copy::clone, MenuAction.NONE);
    }
}
