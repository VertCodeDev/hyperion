package dev.vertcode.hyperion.menu;

import java.util.List;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

/**
 * One menu: a title, three rows of buttons that page when there are more, a row of filler, and a
 * row of up to seven controls between the page arrows.
 *
 * @param title the title
 * @param content the buttons in the paged rows
 * @param controls the buttons in the bottom row, at most seven
 * @param filler the item in the empty row and slots
 * @param previous the item of the arrow to the page before
 * @param next the item of the arrow to the page after
 */
public record MenuPage(
        Component title,
        List<MenuButton> content,
        List<MenuButton> controls,
        ItemStack filler,
        ItemStack previous,
        ItemStack next
) {

    private static final String[] LAYOUT = {
        "x x x x x x x x x",
        "x x x x x x x x x",
        "x x x x x x x x x",
        "# # # # # # # # #",
        "< 0 1 2 3 4 5 6 >"
    };

    /**
     * The number of controls the bottom row holds.
     */
    public static final int MAX_CONTROLS = 7;

    /**
     * Return the rows of the menu, in the characters a renderer lays them out with: {@code x} for
     * the paged content, {@code #} for filler, {@code <} and {@code >} for the arrows and
     * {@code 0} to {@code 6} for the controls.
     *
     * @return a copy of the rows
     */
    public static String[] layout() {
        return LAYOUT.clone();
    }

    /**
     * Copy the lists and items, so a page never changes.
     */
    public MenuPage {
        if (controls.size() > MAX_CONTROLS) {
            throw new IllegalArgumentException("A menu holds at most " + MAX_CONTROLS + " controls");
        }

        content = List.copyOf(content);
        controls = List.copyOf(controls);
        filler = filler.clone();
        previous = previous.clone();
        next = next.clone();
    }
}
