package dev.vertcode.hyperion.api.model.bone;

import java.util.function.Function;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The source behind {@link ItemSource#perViewer(Function)}.
 *
 * @param picker picks the item for one viewer
 */
record PerViewerItemSource(Function<Player, @Nullable ItemStack> picker) implements ItemSource {

    @Override
    public @Nullable ItemStack item(final ItemContext context) {
        Player viewer = context.viewer();
        if (viewer == null) {
            return null;
        }

        return this.picker.apply(viewer);
    }

    @Override
    public boolean perViewer() {
        return true;
    }
}
