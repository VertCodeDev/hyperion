package dev.vertcode.hyperion.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.animation.BonePose;
import dev.vertcode.hyperion.animation.ModelAnimator;
import dev.vertcode.hyperion.api.animation.Animator;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.view.PlayerView;
import dev.vertcode.hyperion.api.model.view.ViewBone;
import dev.vertcode.hyperion.render.DisplayLook;
import dev.vertcode.hyperion.render.DisplayTransform;

/**
 * How one player sees a model, on top of what everyone sees: look overrides for the model and
 * its bones, and animations only this player sees.
 *
 * <p>The api methods are safe from any thread. While the view sets anything, the model renders
 * this player on their own, and this object holds what they were last sent; that render state
 * belongs to the thread that owns the model.
 */
final class HyperionPlayerView implements PlayerView {

    private final HyperionModel model;
    private final Player player;
    private final ViewLook look;
    private final ModelAnimator animator;
    private final Map<String, HyperionViewBone> bones = new ConcurrentHashMap<>();
    private final Map<PartDisplay, DisplayTransform> transforms = new HashMap<>();
    private final Map<PartDisplay, DisplayTransform> sentTransforms = new HashMap<>();
    private final Map<PartDisplay, DisplayLook> sentLooks = new HashMap<>();
    private final Map<PartDisplay, ItemStack> sentItems = new HashMap<>();
    private volatile boolean lookDirty = true;
    private volatile boolean moveDirty = true;
    private BonePose[] poses = new BonePose[0];
    private boolean rendered;
    private boolean animatedBefore;

    /**
     * Create a view with nothing set.
     *
     * @param model the model
     * @param player the player
     */
    HyperionPlayerView(final HyperionModel model, final Player player) {
        this.model = model;
        this.player = player;
        this.look = new ViewLook(this::lookChanged);
        this.animator = model.newAnimator(player);
    }

    // Render state

    /**
     * Return whether the view changes anything, so the player is rendered on their own.
     *
     * @return true while an override is set or an animation plays
     */
    boolean active() {
        if (this.look.any() || this.animator.active()) {
            return true;
        }

        for (HyperionViewBone bone : this.bones.values()) {
            if (bone.any()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return whether a bone has a transform set for this player.
     *
     * @return true when at least one bone does
     */
    boolean movesBones() {
        for (HyperionViewBone bone : this.bones.values()) {
            if (bone.transformOverride() != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return the bone's overrides, if any were made.
     *
     * @param name the bone's name
     * @return the overrides, or null
     */
    @Nullable HyperionViewBone existing(final String name) {
        return this.bones.get(name);
    }

    ViewLook look() {
        return this.look;
    }

    ModelAnimator modelAnimator() {
        return this.animator;
    }

    /**
     * Return the poses this player's bones are worked out in, sized to the model.
     *
     * @param count how many bones the model has
     * @return the poses
     */
    BonePose[] poses(final int count) {
        if (this.poses.length != count) {
            this.poses = new BonePose[count];
            for (int i = 0; i < count; i++) {
                this.poses[i] = new BonePose();
            }
        }

        return this.poses;
    }

    Map<PartDisplay, DisplayTransform> transforms() {
        return this.transforms;
    }

    Map<PartDisplay, DisplayTransform> sentTransforms() {
        return this.sentTransforms;
    }

    Map<PartDisplay, DisplayLook> sentLooks() {
        return this.sentLooks;
    }

    Map<PartDisplay, ItemStack> sentItems() {
        return this.sentItems;
    }

    /**
     * Return whether the player was rendered on their own on the last frame.
     *
     * @return true when they were
     */
    boolean rendered() {
        return this.rendered;
    }

    /**
     * Note whether the player is rendered on their own, dropping the render state when not.
     *
     * @param rendered true when they are
     */
    void rendered(final boolean rendered) {
        this.rendered = rendered;
        if (!rendered) {
            this.transforms.clear();
            this.sentTransforms.clear();
            this.sentLooks.clear();
            this.sentItems.clear();
        }
    }

    /**
     * Start this player's sent state from what everyone was last sent, so going on their own
     * only sends what differs.
     *
     * @param display the display
     * @param item the item the player was last sent on it
     */
    void seed(final PartDisplay display, final ItemStack item) {
        DisplayTransform transform = display.sentTransform();
        DisplayLook look = display.sentLook();
        if (transform != null) {
            this.sentTransforms.put(display, transform);
        }

        if (look != null) {
            this.sentLooks.put(display, look);
        }

        this.sentItems.put(display, item);
    }

    boolean animatedBefore() {
        return this.animatedBefore;
    }

    void animatedBefore(final boolean animated) {
        this.animatedBefore = animated;
    }

    /**
     * Return whether the look changed since the last frame, and clear the mark.
     *
     * @return true once after every change
     */
    boolean takeLookDirty() {
        boolean was = this.lookDirty;
        this.lookDirty = false;

        return was;
    }

    /**
     * Return whether a bone's transform changed since the last frame, and clear the mark.
     *
     * @return true once after every change
     */
    boolean takeMoveDirty() {
        boolean was = this.moveDirty;
        this.moveDirty = false;

        return was;
    }

    void lookChanged() {
        this.lookDirty = true;
        this.model.lookChanged();
    }

    void moved() {
        this.moveDirty = true;
        this.model.moved();
    }

    // Api

    @Override
    public Player player() {
        return this.player;
    }

    @Override
    public Model model() {
        return this.model;
    }

    @Override
    public Optional<ViewBone> bone(final String name) {
        HyperionBone bone = this.model.boneNamed(name);
        if (bone == null) {
            return Optional.empty();
        }

        return Optional.of(this.bones.computeIfAbsent(name, key -> new HyperionViewBone(bone, this)));
    }

    @Override
    public Animator animator() {
        return this.animator;
    }

    @Override
    public void reset() {
        this.look.clear();
        for (HyperionViewBone bone : this.bones.values()) {
            bone.clear();
        }

        this.bones.clear();
        this.animator.stopAll();
    }

    @Override
    public boolean visible() {
        Boolean own = this.look.visible();

        return own != null ? own : this.model.visible();
    }

    @Override
    public PlayerView visible(final boolean visible) {
        this.look.visible(visible);

        return this;
    }

    @Override
    public @Nullable Color tint() {
        Color own = this.look.tint();

        return own != null ? own : this.model.tint();
    }

    @Override
    public PlayerView tint(final @Nullable Color tint) {
        this.look.tint(tint);

        return this;
    }

    @Override
    public boolean glowing() {
        Boolean own = this.look.glowing();

        return own != null ? own : this.model.glowing();
    }

    @Override
    public PlayerView glowing(final boolean glowing) {
        this.look.glowing(glowing);

        return this;
    }

    @Override
    public @Nullable Color glowColor() {
        Color own = this.look.glowColor();

        return own != null ? own : this.model.glowColor();
    }

    @Override
    public PlayerView glowColor(final @Nullable Color color) {
        this.look.glowColor(color);

        return this;
    }

    @Override
    public boolean glint() {
        Boolean own = this.look.glint();

        return own != null ? own : this.model.glint();
    }

    @Override
    public PlayerView glint(final boolean glint) {
        this.look.glint(glint);

        return this;
    }

    @Override
    public Display.@Nullable Billboard billboard() {
        Display.Billboard own = this.look.billboard();

        return own != null ? own : this.model.billboard();
    }

    @Override
    public PlayerView billboard(final Display.@Nullable Billboard billboard) {
        this.look.billboard(billboard);

        return this;
    }

    @Override
    public Display.@Nullable Brightness brightness() {
        Display.Brightness own = this.look.brightness();

        return own != null ? own : this.model.brightness();
    }

    @Override
    public PlayerView brightness(final Display.@Nullable Brightness brightness) {
        this.look.brightness(brightness);

        return this;
    }
}
