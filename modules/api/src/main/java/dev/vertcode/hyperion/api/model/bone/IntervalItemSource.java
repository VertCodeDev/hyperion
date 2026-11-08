package dev.vertcode.hyperion.api.model.bone;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The source behind {@link ItemSource#every(int)}: another source, asked on an interval.
 *
 * @param source the source to ask
 * @param interval the interval in ticks
 */
record IntervalItemSource(ItemSource source, int interval) implements ItemSource {

    private static final String TOO_SHORT = "An item source interval must be 1 tick or more, got %d";

    /**
     * Create the source.
     *
     * @param source the source to ask
     * @param interval the interval in ticks, 1 or more
     * @throws IllegalArgumentException if the interval is below 1
     */
    IntervalItemSource {
        if (interval < 1) {
            throw new IllegalArgumentException(String.format(TOO_SHORT, interval));
        }
    }

    @Override
    public @Nullable ItemStack item(final ItemContext context) {
        return this.source.item(context);
    }

    @Override
    public boolean perViewer() {
        return this.source.perViewer();
    }
}
