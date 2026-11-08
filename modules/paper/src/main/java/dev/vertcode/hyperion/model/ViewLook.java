package dev.vertcode.hyperion.model;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.jspecify.annotations.Nullable;

/**
 * The look one player's view sets on a model or a bone, each value unset until given.
 *
 * <p>Every field is volatile, so a setter on any thread is seen by the next frame. Null means
 * unset: the shared look shows through.
 */
final class ViewLook {

    private final Runnable changed;
    private volatile @Nullable Boolean visible;
    private volatile @Nullable Color tint;
    private volatile @Nullable Boolean glowing;
    private volatile @Nullable Color glowColor;
    private volatile @Nullable Boolean glint;
    private volatile Display.@Nullable Billboard billboard;
    private volatile Display.@Nullable Brightness brightness;

    /**
     * Create a look with nothing set.
     *
     * @param changed what to run after every change
     */
    ViewLook(final Runnable changed) {
        this.changed = changed;
    }

    /**
     * Return whether anything is set.
     *
     * @return true when at least one value is set
     */
    boolean any() {
        return this.visible != null || this.tint != null || this.glowing != null || this.glowColor != null
                || this.glint != null || this.billboard != null || this.brightness != null;
    }

    /**
     * Unset everything.
     */
    void clear() {
        this.visible = null;
        this.tint = null;
        this.glowing = null;
        this.glowColor = null;
        this.glint = null;
        this.billboard = null;
        this.brightness = null;
        this.changed.run();
    }

    @Nullable Boolean visible() {
        return this.visible;
    }

    void visible(final boolean visible) {
        this.visible = visible;
        this.changed.run();
    }

    @Nullable Color tint() {
        return this.tint;
    }

    void tint(final @Nullable Color tint) {
        this.tint = tint;
        this.changed.run();
    }

    @Nullable Boolean glowing() {
        return this.glowing;
    }

    void glowing(final boolean glowing) {
        this.glowing = glowing;
        this.changed.run();
    }

    @Nullable Color glowColor() {
        return this.glowColor;
    }

    void glowColor(final @Nullable Color glowColor) {
        this.glowColor = glowColor;
        this.changed.run();
    }

    @Nullable Boolean glint() {
        return this.glint;
    }

    void glint(final boolean glint) {
        this.glint = glint;
        this.changed.run();
    }

    Display.@Nullable Billboard billboard() {
        return this.billboard;
    }

    void billboard(final Display.@Nullable Billboard billboard) {
        this.billboard = billboard;
        this.changed.run();
    }

    Display.@Nullable Brightness brightness() {
        return this.brightness;
    }

    void brightness(final Display.@Nullable Brightness brightness) {
        this.brightness = brightness;
        this.changed.run();
    }
}
