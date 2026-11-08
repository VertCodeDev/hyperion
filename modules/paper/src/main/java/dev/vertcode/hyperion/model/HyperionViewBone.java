package dev.vertcode.hyperion.model;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.bone.BoneTransform;
import dev.vertcode.hyperion.api.model.bone.ItemSource;
import dev.vertcode.hyperion.api.model.view.ViewBone;

/**
 * One bone as one player sees it: overrides on top of the shared bone, each unset until given.
 * Setters are safe from any thread and take effect on the next frame.
 */
final class HyperionViewBone implements ViewBone {

    private final HyperionBone bone;
    private final HyperionPlayerView view;
    private final ViewLook look;
    private volatile @Nullable BoneTransform transform;
    private volatile @Nullable ItemSource itemSource;
    private volatile @Nullable ItemStack item;

    /**
     * Create a bone's overrides with nothing set.
     *
     * @param bone the shared bone
     * @param view the view it belongs to
     */
    HyperionViewBone(final HyperionBone bone, final HyperionPlayerView view) {
        this.bone = bone;
        this.view = view;
        this.look = new ViewLook(view::lookChanged);
    }

    HyperionBone bone() {
        return this.bone;
    }

    ViewLook look() {
        return this.look;
    }

    /**
     * Return whether anything is set on this bone.
     *
     * @return true when at least one override is set
     */
    boolean any() {
        return this.look.any() || this.transform != null || this.itemSource != null;
    }

    /**
     * Unset everything.
     */
    void clear() {
        this.look.clear();
        this.transform = null;
        this.itemSource = null;
        this.item = null;
        this.view.moved();
    }

    /**
     * Return the transform set for this player.
     *
     * @return the transform, or null when unset
     */
    @Nullable BoneTransform transformOverride() {
        return this.transform;
    }

    /**
     * Note the item this player was last sent for the bone.
     *
     * @param item the item, or null
     */
    void shownItem(final @Nullable ItemStack item) {
        this.item = item;
    }

    @Override
    public String name() {
        return this.bone.name();
    }

    @Override
    public BoneTransform transform() {
        BoneTransform own = this.transform;

        return own != null ? own : this.bone.transform();
    }

    @Override
    public ViewBone transform(final BoneTransform transform) {
        this.transform = transform;
        this.view.moved();

        return this;
    }

    @Override
    public @Nullable ItemStack item() {
        ItemStack shown = this.item;
        if (shown != null) {
            return shown.clone();
        }

        return this.itemSource == null ? this.bone.item() : null;
    }

    @Override
    public ViewBone item(final @Nullable ItemStack item) {
        return this.itemSource(item == null ? null : ItemSource.fixed(item));
    }

    @Override
    public @Nullable ItemSource itemSource() {
        ItemSource own = this.itemSource;

        return own != null ? own : this.bone.itemSource();
    }

    /**
     * Return the item source set for this player.
     *
     * @return the source, or null when unset
     */
    @Nullable ItemSource itemSourceOverride() {
        return this.itemSource;
    }

    @Override
    public ViewBone itemSource(final @Nullable ItemSource source) {
        this.itemSource = source;
        this.item = null;
        this.view.lookChanged();

        return this;
    }

    @Override
    public boolean visible() {
        Boolean own = this.look.visible();

        return own != null ? own : this.bone.visible();
    }

    @Override
    public ViewBone visible(final boolean visible) {
        this.look.visible(visible);

        return this;
    }

    @Override
    public @Nullable Color tint() {
        Color own = this.look.tint();

        return own != null ? own : this.bone.tint();
    }

    @Override
    public ViewBone tint(final @Nullable Color tint) {
        this.look.tint(tint);

        return this;
    }

    @Override
    public boolean glowing() {
        Boolean own = this.look.glowing();

        return own != null ? own : this.bone.glowing();
    }

    @Override
    public ViewBone glowing(final boolean glowing) {
        this.look.glowing(glowing);

        return this;
    }

    @Override
    public @Nullable Color glowColor() {
        Color own = this.look.glowColor();

        return own != null ? own : this.bone.glowColor();
    }

    @Override
    public ViewBone glowColor(final @Nullable Color color) {
        this.look.glowColor(color);

        return this;
    }

    @Override
    public boolean glint() {
        Boolean own = this.look.glint();

        return own != null ? own : this.bone.glint();
    }

    @Override
    public ViewBone glint(final boolean glint) {
        this.look.glint(glint);

        return this;
    }

    @Override
    public Display.@Nullable Billboard billboard() {
        Display.Billboard own = this.look.billboard();

        return own != null ? own : this.bone.billboard();
    }

    @Override
    public ViewBone billboard(final Display.@Nullable Billboard billboard) {
        this.look.billboard(billboard);

        return this;
    }

    @Override
    public Display.@Nullable Brightness brightness() {
        Display.Brightness own = this.look.brightness();

        return own != null ? own : this.bone.brightness();
    }

    @Override
    public ViewBone brightness(final Display.@Nullable Brightness brightness) {
        this.look.brightness(brightness);

        return this;
    }
}
