package dev.vertcode.hyperion.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.pack.ModelPart;
import dev.vertcode.hyperion.render.DisplayLook;
import dev.vertcode.hyperion.render.DisplayTransform;

/**
 * One item display of a model: a part of a bone, or the item a bone was given.
 *
 * <p>Holds what the display shows now and what its viewers were last sent, so a frame sends
 * only the difference. Only the thread that owns the model touches it.
 */
final class PartDisplay {

    private final int entityId = SpigotReflectionUtil.generateEntityId();
    private final UUID uniqueId = UUID.randomUUID();
    private final @Nullable ModelPart part;
    private final Map<UUID, ItemStack> viewerItems = new ConcurrentHashMap<>();
    private @Nullable DisplayTransform transform;
    private @Nullable DisplayTransform sentTransform;
    private @Nullable DisplayLook look;
    private @Nullable DisplayLook sentLook;
    private ItemStack item = ItemStack.EMPTY;
    private boolean itemChanged;
    private boolean viewerItemsStale;

    /**
     * Create a display with a fresh entity id.
     *
     * @param part the part it shows, or null for the item display of a bone
     */
    PartDisplay(final @Nullable ModelPart part) {
        this.part = part;
    }

    int entityId() {
        return this.entityId;
    }

    UUID uniqueId() {
        return this.uniqueId;
    }

    /**
     * Return the part this display shows.
     *
     * @return the part, or null for a bone's item display
     */
    @Nullable ModelPart part() {
        return this.part;
    }

    /**
     * Return where the display is this frame.
     *
     * @return the transform, or null before the first frame worked it out
     */
    @Nullable DisplayTransform transform() {
        return this.transform;
    }

    void transform(final DisplayTransform transform) {
        this.transform = transform;
    }

    /**
     * Return how the display looks this frame.
     *
     * @return the look, or null before the first frame worked it out
     */
    @Nullable DisplayLook look() {
        return this.look;
    }

    void look(final DisplayLook look) {
        this.look = look;
    }

    ItemStack item() {
        return this.item;
    }

    /**
     * Change the item every viewer sees, noting that it has to be sent.
     *
     * @param item the new item
     */
    void item(final ItemStack item) {
        this.item = item;
        this.itemChanged = true;
    }

    /**
     * Return whether the transform moved since viewers were last sent it.
     *
     * @param epsilon the largest difference that does not count
     * @return true when it has to be sent
     */
    boolean moved(final float epsilon) {
        return this.transform != null
                && (this.sentTransform == null || !this.transform.closeTo(this.sentTransform, epsilon));
    }

    /**
     * Return whether the outline, billboard or light changed since viewers were last sent them.
     *
     * @return true when they have to be sent
     */
    boolean restyled() {
        return this.look != null && (this.sentLook == null || this.look.styleDiffers(this.sentLook));
    }

    boolean itemChanged() {
        return this.itemChanged;
    }

    /**
     * Note that the items of a per-viewer source must be picked and sent again this frame.
     */
    void markViewerItemsStale() {
        this.viewerItemsStale = true;
    }

    boolean viewerItemsStale() {
        return this.viewerItemsStale;
    }

    /**
     * Note that every viewer has what this frame worked out.
     */
    void sent() {
        this.sentTransform = this.transform;
        this.sentLook = this.look;
        this.itemChanged = false;
        this.viewerItemsStale = false;
    }

    /**
     * Return the items of a per-viewer source, by viewer.
     *
     * @return the items each viewer was last sent
     */
    Map<UUID, ItemStack> viewerItems() {
        return this.viewerItems;
    }
}
