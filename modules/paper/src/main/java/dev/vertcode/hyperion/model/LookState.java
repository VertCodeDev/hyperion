package dev.vertcode.hyperion.model;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.jspecify.annotations.Nullable;

/**
 * The look a model or a bone was given: what the {@code Appearance} setters write and a frame
 * reads.
 *
 * <p>Every field is volatile, so a setter on any thread is seen by the next frame, and every
 * setter tells the model a frame has looks to send.
 */
final class LookState {

    private final Runnable changed;
    private volatile boolean visible;
    private volatile @Nullable Color tint;
    private volatile boolean glowing;
    private volatile @Nullable Color glowColor;
    private volatile boolean glint;
    private volatile Display.@Nullable Billboard billboard;
    private volatile Display.@Nullable Brightness brightness;

    /**
     * Create a look with nothing set.
     *
     * @param visible whether it starts visible
     * @param changed what to run after every change
     */
    LookState(final boolean visible, final Runnable changed) {
        this.visible = visible;
        this.changed = changed;
    }

    boolean visible() {
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

    boolean glowing() {
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

    boolean glint() {
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
