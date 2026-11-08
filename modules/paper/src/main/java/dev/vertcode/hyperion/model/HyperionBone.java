package dev.vertcode.hyperion.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import net.kyori.adventure.key.Key;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.behavior.BoneBehavior;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.model.bone.BoneModifier;
import dev.vertcode.hyperion.api.model.bone.BoneTransform;
import dev.vertcode.hyperion.api.model.bone.ItemSource;
import dev.vertcode.hyperion.blueprint.BlueprintBone;
import dev.vertcode.hyperion.render.DisplayTransform;

/**
 * One bone of a live model.
 *
 * <p>The look, transform, item source and modifiers are written from any thread and read by the
 * next frame. The tree, the displays and the matrices belong to the thread that owns the model.
 * A reload keeps a bone whose name is still in the model, with everything it was given, and
 * points it at the new blueprint's bone.
 */
final class HyperionBone implements Bone {

    private final HyperionModel model;
    private final LookState look;
    private final Map<Key, BoneModifier> modifiers = new LinkedHashMap<>();
    private final Matrix4f matrix = new Matrix4f();
    private final List<PartDisplay> displays = new ArrayList<>();
    private volatile BlueprintBone blueprint;
    private volatile @Nullable HyperionBone parent;
    private volatile List<HyperionBone> children = List.of();
    private volatile List<BoneBehavior> behaviors = List.of();
    private volatile List<Map.Entry<Key, BoneModifier>> modifierList = List.of();
    private volatile BoneTransform transform = BoneTransform.IDENTITY;
    private volatile @Nullable ItemSource itemSource;
    private volatile @Nullable ItemStack item;
    private volatile Matrix4fc world = new Matrix4f();
    private volatile boolean itemDirty;
    private @Nullable PartDisplay itemDisplay;

    /**
     * Create a bone, visible when its blueprint says so.
     *
     * @param model the model it belongs to
     * @param blueprint the blueprint's bone
     */
    HyperionBone(final HyperionModel model, final BlueprintBone blueprint) {
        this.model = model;
        this.blueprint = blueprint;
        this.look = new LookState(blueprint.visibleByDefault(), model::lookChanged);
    }

    @Override
    public BoneBlueprint blueprint() {
        return this.blueprint;
    }

    /**
     * Return the blueprint's bone with its cubes.
     *
     * @return the bone
     */
    BlueprintBone blueprintBone() {
        return this.blueprint;
    }

    /**
     * Point this bone at a reloaded blueprint's bone of the same name.
     *
     * @param blueprint the new bone
     */
    void blueprint(final BlueprintBone blueprint) {
        this.blueprint = blueprint;
    }

    @Override
    public Model model() {
        return this.model;
    }

    @Override
    public String name() {
        return this.blueprint.name();
    }

    @Override
    public Optional<Bone> parent() {
        return Optional.ofNullable(this.parent);
    }

    /**
     * Return the parent as this class.
     *
     * @return the parent, or null for a root bone
     */
    @Nullable HyperionBone parentBone() {
        return this.parent;
    }

    /**
     * Hang this bone in the tree.
     *
     * @param parent the parent, or null for a root bone
     * @param children the bones that hang from this one
     */
    void link(final @Nullable HyperionBone parent, final List<HyperionBone> children) {
        this.parent = parent;
        this.children = List.copyOf(children);
    }

    @Override
    public List<Bone> children() {
        return List.copyOf(this.children);
    }

    @Override
    public List<Bone> descendants() {
        List<Bone> descendants = new ArrayList<>();
        this.collect(descendants);

        return List.copyOf(descendants);
    }

    @Override
    public Location location() {
        Vector3f pivot = this.world.transformPosition(new Vector3f(this.blueprint.pivot()));

        return this.model.location().add(pivot.x, pivot.y, pivot.z);
    }

    @Override
    public Quaternionfc rotation() {
        return DisplayTransform.of(this.world).rotation();
    }

    /**
     * Return where the bone is in model space, with the model's root, as of the last frame.
     *
     * @return the matrix, never changed once published
     */
    Matrix4fc world() {
        return this.world;
    }

    /**
     * Publish this frame's matrix with the root, for {@link #location()} and the locators.
     *
     * @param world the matrix, which this bone keeps
     */
    void world(final Matrix4fc world) {
        this.world = world;
    }

    /**
     * Return the bone's own matrix, without the root, which only the owning thread writes.
     *
     * @return the matrix
     */
    Matrix4f matrix() {
        return this.matrix;
    }

    @Override
    public Bone modifier(final Key key, final BoneModifier modifier) {
        synchronized (this.modifiers) {
            this.modifiers.put(key, modifier);
            this.modifierList = List.copyOf(this.modifiers.entrySet());
        }

        return this;
    }

    @Override
    public boolean removeModifier(final Key key) {
        synchronized (this.modifiers) {
            boolean removed = this.modifiers.remove(key) != null;
            this.modifierList = List.copyOf(this.modifiers.entrySet());

            return removed;
        }
    }

    @Override
    public List<Key> modifiers() {
        List<Key> keys = new ArrayList<>();
        for (Map.Entry<Key, BoneModifier> entry : this.modifierList) {
            keys.add(entry.getKey());
        }

        return List.copyOf(keys);
    }

    /**
     * Work out this frame's transform: the bone's own, then every modifier in order.
     *
     * @return the transform
     */
    BoneTransform frameTransform() {
        BoneTransform result = this.transform;
        for (Map.Entry<Key, BoneModifier> entry : this.modifierList) {
            result = entry.getValue().modify(this, result);
        }

        return result;
    }

    /**
     * Return whether this bone has modifiers, which can change it on every frame.
     *
     * @return true with at least one modifier
     */
    boolean hasModifiers() {
        return !this.modifierList.isEmpty();
    }

    @Override
    public Collection<BoneBehavior> behaviors() {
        return this.behaviors;
    }

    /**
     * Give this bone its behaviors.
     *
     * @param behaviors the behaviors
     */
    void behaviors(final List<BoneBehavior> behaviors) {
        this.behaviors = List.copyOf(behaviors);
    }

    @Override
    public <B extends BoneBehavior> Optional<B> behavior(final Class<B> type) {
        for (BoneBehavior behavior : this.behaviors) {
            if (type.isInstance(behavior)) {
                return Optional.of(type.cast(behavior));
            }
        }

        return Optional.empty();
    }

    @Override
    public BoneTransform transform() {
        return this.transform;
    }

    @Override
    public Bone transform(final BoneTransform transform) {
        this.transform = transform;
        this.model.moved();

        return this;
    }

    @Override
    public @Nullable ItemStack item() {
        ItemStack current = this.item;

        return current == null ? null : current.clone();
    }

    /**
     * Return the item a shared source picked last, without copying it.
     *
     * @return the item, or null
     */
    @Nullable ItemStack currentItem() {
        return this.item;
    }

    /**
     * Note that the item must be picked and sent again on the next frame.
     */
    void markItemDirty() {
        this.itemDirty = true;
    }

    boolean itemDirty() {
        return this.itemDirty;
    }

    /**
     * Return whether the item must be sent again, and clear the mark.
     *
     * @return true once after every change
     */
    boolean takeItemDirty() {
        boolean was = this.itemDirty;
        this.itemDirty = false;

        return was;
    }

    /**
     * Note the item a non per-viewer source picked this frame.
     *
     * @param item the item, or null
     */
    void currentItem(final @Nullable ItemStack item) {
        this.item = item;
    }

    @Override
    public Bone item(final @Nullable ItemStack item) {
        return this.itemSource(item == null ? null : ItemSource.fixed(item));
    }

    @Override
    public @Nullable ItemSource itemSource() {
        return this.itemSource;
    }

    @Override
    public Bone itemSource(final @Nullable ItemSource source) {
        this.itemSource = source;
        this.item = null;
        this.itemDirty = true;
        this.model.lookChanged();

        return this;
    }

    /**
     * Return the displays of the bone's parts.
     *
     * @return the displays, which only the owning thread changes
     */
    List<PartDisplay> displays() {
        return this.displays;
    }

    /**
     * Return the display of the item this bone was given.
     *
     * @return the display, or null while the bone shows its own cubes
     */
    @Nullable PartDisplay itemDisplay() {
        return this.itemDisplay;
    }

    void itemDisplay(final @Nullable PartDisplay display) {
        this.itemDisplay = display;
    }

    /**
     * Return the bones in this bone's subtree that pass a test, this one first.
     *
     * @param filter the test
     * @param into where they go
     */
    void select(final Predicate<Bone> filter, final List<Bone> into) {
        if (filter.test(this)) {
            into.add(this);
        }

        for (HyperionBone child : this.children) {
            child.select(filter, into);
        }
    }

    /**
     * Add every bone below this one, parents first.
     *
     * @param into where they go
     */
    private void collect(final List<Bone> into) {
        for (HyperionBone child : this.children) {
            into.add(child);
            child.collect(into);
        }
    }

    LookState look() {
        return this.look;
    }

    @Override
    public boolean visible() {
        return this.look.visible();
    }

    @Override
    public Bone visible(final boolean visible) {
        this.look.visible(visible);

        return this;
    }

    @Override
    public @Nullable Color tint() {
        return this.look.tint();
    }

    @Override
    public Bone tint(final @Nullable Color tint) {
        this.look.tint(tint);

        return this;
    }

    @Override
    public boolean glowing() {
        return this.look.glowing();
    }

    @Override
    public Bone glowing(final boolean glowing) {
        this.look.glowing(glowing);

        return this;
    }

    @Override
    public @Nullable Color glowColor() {
        return this.look.glowColor();
    }

    @Override
    public Bone glowColor(final @Nullable Color color) {
        this.look.glowColor(color);

        return this;
    }

    @Override
    public boolean glint() {
        return this.look.glint();
    }

    @Override
    public Bone glint(final boolean glint) {
        this.look.glint(glint);

        return this;
    }

    @Override
    public Display.@Nullable Billboard billboard() {
        return this.look.billboard();
    }

    @Override
    public Bone billboard(final Display.@Nullable Billboard billboard) {
        this.look.billboard(billboard);

        return this;
    }

    @Override
    public Display.@Nullable Brightness brightness() {
        return this.look.brightness();
    }

    @Override
    public Bone brightness(final Display.@Nullable Brightness brightness) {
        this.look.brightness(brightness);

        return this;
    }

    @Override
    public String toString() {
        return "HyperionBone[" + this.name() + "]";
    }
}
