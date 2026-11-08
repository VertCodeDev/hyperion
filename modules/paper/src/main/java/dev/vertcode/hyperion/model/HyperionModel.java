package dev.vertcode.hyperion.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.animation.Animator;
import dev.vertcode.hyperion.api.event.ModelRemoveEvent;
import dev.vertcode.hyperion.api.event.ModelViewerEvent;
import dev.vertcode.hyperion.api.model.EntityAnchor;
import dev.vertcode.hyperion.api.model.EntityPart;
import dev.vertcode.hyperion.api.model.LocationAnchor;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.ModelAnchor;
import dev.vertcode.hyperion.api.model.ModelBlueprint;
import dev.vertcode.hyperion.api.model.ModelRotation;
import dev.vertcode.hyperion.api.model.ModelRotator;
import dev.vertcode.hyperion.api.model.ModelScaler;
import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.api.model.behavior.BehaviorType;
import dev.vertcode.hyperion.api.model.behavior.BoneBehavior;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.model.bone.BoneTransform;
import dev.vertcode.hyperion.api.model.bone.ItemContext;
import dev.vertcode.hyperion.api.model.bone.ItemSource;
import dev.vertcode.hyperion.api.model.locator.Locator;
import dev.vertcode.hyperion.api.model.view.PlayerView;
import dev.vertcode.hyperion.api.model.view.Viewers;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintBone;
import dev.vertcode.hyperion.blueprint.BlueprintLocator;
import dev.vertcode.hyperion.pack.ModelPart;
import dev.vertcode.hyperion.pack.ResourcePack;
import dev.vertcode.hyperion.render.BaseEntityData;
import dev.vertcode.hyperion.render.DisplayData;
import dev.vertcode.hyperion.render.DisplayLook;
import dev.vertcode.hyperion.render.DisplayTransform;
import dev.vertcode.hyperion.render.ModelSpace;
import dev.vertcode.hyperion.render.PartItems;
import dev.vertcode.hyperion.render.Packets;

/**
 * A live model: bones shown as item displays to the players who can see it.
 *
 * <p>Every tick a frame runs on the thread that owns the model: the entity's scheduler for a
 * model on an entity, the region of its location for one on its own. The frame works out the
 * scale, the rotation and every bone, checks the viewers every half second, and sends each
 * viewer only what changed. A model nobody sees works out nothing but its behaviors.
 *
 * <p>The displays of a model on an entity ride that entity, so the client moves them with it and
 * no movement is ever sent. They sit on the entity's seat, at the top of its hitbox, so the root
 * is lowered by the entity's height to put the model's origin at its feet.
 */
final class HyperionModel implements Model {

    private static final float EPSILON = 1.0e-4f;
    private static final int VIEWER_CHECK_TICKS = 10;
    private static final Matrix4fc IDENTITY = new Matrix4f();
    private static final String NOT_OWNER = "This is not the thread that owns the model; schedule it on its entity or region";
    private static final String ON_ENTITY = "A model on an entity follows its entity and cannot be teleported";
    private static final String REMOVED = "The model has been removed";
    private static final String NO_WORLD = "The location has no world";
    private static final String SCALE_NOT_POSITIVE = "A model scale must be above 0, got %s";
    private static final String NEGATIVE_TICKS = "Interpolation ticks must be 0 or more, got %d";
    private static final String NO_VIEWS = "Per-player views arrive together with animations";
    private static final String BEHAVIOR_FAILED = "The behavior %s on bone %s of %s failed";
    private static final String CREATE_FAILED = "The behavior type %s could not be created for bone %s of %s";

    private final ModelManager manager;
    private final UUID uniqueId = UUID.randomUUID();
    private final @Nullable Entity entity;
    private final LookState look;
    private final ModelViewers viewers;
    private final Matrix4f root = new Matrix4f();
    private final Matrix4f scratch = new Matrix4f();
    private final List<PartDisplay> displays = new ArrayList<>();
    private final List<Viewer> joined = new ArrayList<>();
    private final List<Viewer> respawn = new ArrayList<>();
    private final List<PartDisplay> added = new ArrayList<>();
    private final List<PartDisplay> dropped = new ArrayList<>();
    private volatile Blueprint blueprint;
    private volatile @Nullable ResourcePack pack;
    private volatile List<HyperionBone> bones = List.of();
    private volatile Map<String, HyperionBone> bonesByName = Map.of();
    private volatile List<HyperionLocator> locators = List.of();
    private volatile BoneTransform transform;
    private volatile ModelScaler scaler;
    private volatile ModelRotator rotator;
    private volatile int interpolation;
    private volatile Set<EntityPart> hiddenParts;
    private volatile float scale = 1f;
    private volatile ModelRotation rotation = ModelRotation.ZERO;
    private volatile Location origin;
    private volatile Location location;
    private volatile Matrix4fc rootWorld = new Matrix4f();
    private volatile int[] displayIds = new int[0];
    private volatile boolean removed;
    private volatile boolean lookDirty = true;
    private volatile boolean moveDirty = true;
    private volatile boolean hiddenDirty;
    private volatile boolean teleported;
    private @Nullable ScheduledTask task;
    private @Nullable ModelRotation lastRotation;
    private float lastScale;
    private float lastLift;
    private long ticks;

    /**
     * Build a model. Nothing is scheduled or sent until {@link #start()}.
     *
     * @param manager the manager that tracks it
     * @param blueprint the blueprint it shows
     * @param anchor what drives its position
     * @param options the caller's settings
     * @param pack the resource pack its parts come from, or null before the first build
     */
    HyperionModel(
            final ModelManager manager,
            final Blueprint blueprint,
            final ModelAnchor anchor,
            final ModelOptions options,
            final @Nullable ResourcePack pack
    ) {
        this.manager = manager;
        this.blueprint = blueprint;
        this.pack = pack;
        this.entity = anchor instanceof EntityAnchor(Entity carrier) ? carrier : null;
        this.origin = anchor instanceof LocationAnchor(Location at) ? at : Objects.requireNonNull(this.entity).getLocation();
        this.location = this.origin.clone();
        this.look = new LookState(true, this::lookChanged);
        this.look.tint(options.tint);
        this.look.glowing(options.glowing);
        this.look.glowColor(options.glowColor);
        this.look.brightness(options.brightness);
        this.look.glint(options.glint);
        this.look.billboard(options.billboard);
        this.viewers = new ModelViewers(options.policy, options.viewDistance);
        this.transform = options.transform;
        this.scaler = options.scaler;
        this.rotator = options.rotator;
        this.interpolation = options.interpolation;
        this.hiddenParts = this.entity == null ? EntityPart.NONE : Set.copyOf(options.hiddenParts);
        this.buildBones(blueprint, false);
        this.buildDisplays();
    }

    /**
     * Start the frames and tell every behavior the model spawned. Call on the owning thread.
     */
    void start() {
        this.schedule();
        for (HyperionBone bone : this.bones) {
            for (BoneBehavior behavior : bone.behaviors()) {
                this.runBehavior(bone, behavior, BoneBehavior::onSpawn);
            }
        }
    }

    // Frames

    /**
     * Run one frame on the owning thread.
     */
    private void frame() {
        if (this.removed) {
            return;
        }

        this.ticks++;
        Location here = this.entity != null ? this.entity.getLocation() : this.origin;
        this.location = here;
        float nextScale = this.scaler.scale(this);
        if (nextScale > 0f && Float.isFinite(nextScale)) {
            this.scale = nextScale;
        }

        this.rotation = this.rotator.rotate(this, this.baseRotation(here));
        this.tickBehaviors();
        if (this.removed) {
            return;
        }

        this.updateViewers(here);
        if (this.viewers.isEmpty()) {
            return;
        }

        this.render(here);
    }

    /**
     * Work out which way the anchor faces: a living entity's body, any other entity's look, or
     * the location's own yaw and pitch.
     *
     * @param here where the anchor is
     * @return the rotation
     */
    private ModelRotation baseRotation(final Location here) {
        if (this.entity instanceof LivingEntity living) {
            return new ModelRotation(0f, living.getBodyYaw());
        }

        return new ModelRotation(here.getPitch(), here.getYaw());
    }

    /**
     * Check who should see the model, every half second or right after the viewers changed:
     * hide it from those who should not, and note those who should for this frame to spawn.
     *
     * @param here where the model is
     */
    private void updateViewers(final Location here) {
        boolean due = this.ticks % VIEWER_CHECK_TICKS == 1;
        if (!this.viewers.takeDirty() && !due) {
            return;
        }

        List<Player> candidates = this.candidates(here);
        for (Viewer viewer : this.viewers.snapshot()) {
            if (!candidates.contains(viewer.player()) || !this.viewers.wants(viewer.player(), here)) {
                this.hideFrom(viewer, true);
            }
        }

        for (Player player : candidates) {
            if (!this.viewers.isViewing(player) && this.viewers.wants(player, here)) {
                this.join(player);
            }
        }
    }

    /**
     * Return the players who could see the model: those tracking its entity, or those near its
     * location.
     *
     * @param here where the model is
     * @return the players
     */
    private List<Player> candidates(final Location here) {
        if (this.entity != null) {
            return List.copyOf(this.entity.getTrackedBy());
        }

        if (here.getWorld() == null) {
            return List.of();
        }

        return List.copyOf(here.getWorld().getNearbyPlayers(here, this.viewers.viewDistance()));
    }

    /**
     * Count a player as a viewer now, so the entity's own packets hide its parts from them at
     * once, and spawn the displays for them on this frame or the next.
     *
     * @param player the player
     */
    private void join(final Player player) {
        User user = this.manager.user(player);
        if (user != null) {
            this.joined.add(this.viewers.add(player, user));
        }
    }

    /**
     * Stop showing the model to a viewer.
     *
     * @param viewer the viewer
     * @param restoreEntity whether the viewer still sees the entity, which then gets its hidden
     *     parts back
     */
    private void hideFrom(final Viewer viewer, final boolean restoreEntity) {
        this.viewers.remove(viewer.player().getUniqueId());
        boolean wasPending = this.joined.remove(viewer);
        this.respawn.remove(viewer);
        for (PartDisplay display : this.displays) {
            display.viewerItems().remove(viewer.player().getUniqueId());
        }

        if (wasPending) {
            return;
        }

        Packets.send(viewer.user(), Packets.destroy(this.displayIds));
        if (restoreEntity && this.entity != null) {
            this.restoreEntity(viewer, this.entity);
        }

        Bukkit.getPluginManager().callEvent(new ModelViewerEvent(this, viewer.player(), false));
        for (HyperionBone bone : this.bones) {
            for (BoneBehavior behavior : bone.behaviors()) {
                this.runBehavior(bone, behavior, each -> each.onHide(viewer.player()));
            }
        }
    }

    /**
     * Give a viewer the entity back as it really is, without this model's displays riding it.
     *
     * @param viewer the viewer
     * @param carrier the entity
     */
    private void restoreEntity(final Viewer viewer, final Entity carrier) {
        if (!carrier.isValid()) {
            return;
        }

        UUID id = viewer.player().getUniqueId();
        for (PacketWrapper<?> packet : BaseEntityData.packets(carrier, this.manager.hiddenFor(carrier.getEntityId(), id))) {
            Packets.send(viewer.user(), packet);
        }

        Packets.send(viewer.user(), Packets.passengers(carrier.getEntityId(), this.manager.passengers(carrier, id)));
    }

    /**
     * Work out every display and send each viewer what changed, and new viewers everything.
     *
     * @param here where the model is
     */
    private void render(final Location here) {
        this.syncItemDisplays();
        this.updateItems();
        boolean everything = !this.joined.isEmpty() || !this.respawn.isEmpty() || !this.added.isEmpty();
        if (everything || this.needsMove()) {
            this.computeTransforms();
        }

        if (everything || this.lookDirty) {
            this.lookDirty = false;
            this.computeLooks();
        }

        List<PacketWrapper<?>> common = this.changes(here);
        boolean hidden = this.hiddenDirty;
        this.hiddenDirty = false;
        boolean restructured = !this.added.isEmpty() || !this.dropped.isEmpty();
        for (Viewer viewer : this.viewers.viewers()) {
            if (this.joined.contains(viewer) || this.respawn.contains(viewer)) {
                continue;
            }

            for (PacketWrapper<?> packet : common) {
                Packets.send(viewer.user(), packet);
            }

            this.sendViewerItems(viewer);
            if (this.entity != null && restructured) {
                int[] riders = this.manager.passengers(this.entity, viewer.player().getUniqueId());
                Packets.send(viewer.user(), Packets.passengers(this.entity.getEntityId(), riders));
            }

            if (this.entity != null && hidden) {
                this.sendEntity(viewer, this.entity);
            }
        }

        for (Viewer viewer : this.respawn) {
            this.spawnFor(viewer, here);
        }

        for (Viewer viewer : this.joined) {
            this.spawnFor(viewer, here);
            this.announce(viewer);
        }

        for (PartDisplay display : this.displays) {
            display.sent();
        }

        this.joined.clear();
        this.respawn.clear();
        this.added.clear();
        this.dropped.clear();
        this.teleported = false;
    }

    /**
     * Return whether anything that places the displays changed since the last frame.
     *
     * @return true when the transforms must be worked out again
     */
    private boolean needsMove() {
        if (this.moveDirty || this.scale != this.lastScale || !this.rotation.equals(this.lastRotation)) {
            return true;
        }

        if (this.entity != null && this.lift() != this.lastLift) {
            return true;
        }

        for (HyperionBone bone : this.bones) {
            if (bone.hasModifiers()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return how far the displays sit above the model's origin: the entity's height, since they
     * ride its seat.
     *
     * @return the height in blocks, 0 for a model on its own
     */
    private float lift() {
        return this.entity == null ? 0f : (float) this.entity.getHeight();
    }

    /**
     * Work out the matrix of every bone and the transform of every display.
     */
    private void computeTransforms() {
        this.moveDirty = false;
        this.lastRotation = this.rotation;
        this.lastScale = this.scale;
        this.lastLift = this.lift();
        ModelSpace.root(this.root, this.rotation, this.scale, this.transform);
        this.rootWorld = new Matrix4f(this.root);
        for (HyperionBone bone : this.bones) {
            HyperionBone parent = bone.parentBone();
            Matrix4fc parentMatrix = parent == null ? IDENTITY : parent.matrix();
            BlueprintBone shape = bone.blueprintBone();
            ModelSpace.bone(bone.matrix(), parentMatrix, shape.pivot(), shape.rotation(), bone.frameTransform());
            bone.world(new Matrix4f(this.root).mul(bone.matrix()));
            for (PartDisplay display : bone.displays()) {
                ModelPart part = Objects.requireNonNull(display.part());
                ModelSpace.part(this.scratch, this.root, bone.matrix(), shape.pivot(), part);
                display.transform(DisplayTransform.of(this.scratch.translateLocal(0f, -this.lastLift, 0f)));
            }

            PartDisplay item = bone.itemDisplay();
            if (item != null) {
                ModelSpace.item(this.scratch, this.root, bone.matrix(), shape.pivot());
                item.transform(DisplayTransform.of(this.scratch.translateLocal(0f, -this.lastLift, 0f)));
            }
        }
    }

    /**
     * Work out how every display looks, and build the items whose look changed.
     */
    private void computeLooks() {
        PartItems items = this.manager.items();
        for (HyperionBone bone : this.bones) {
            DisplayLook look = this.lookOf(bone);
            PartDisplay item = bone.itemDisplay();
            DisplayLook partLook = item == null ? look : hidden(look);
            for (PartDisplay display : bone.displays()) {
                DisplayLook previous = display.look();
                display.look(partLook);
                if (previous == null || partLook.itemDiffers(previous)) {
                    display.item(items.part(Objects.requireNonNull(display.part()), partLook));
                }
            }

            if (item == null) {
                continue;
            }

            DisplayLook previous = item.look();
            item.look(look);
            boolean changed = bone.takeItemDirty() || previous == null || look.itemDiffers(previous);
            if (!changed) {
                continue;
            }

            ItemSource source = bone.itemSource();
            if (source != null && source.perViewer()) {
                item.markViewerItemsStale();
            } else {
                item.item(items.given(bone.currentItem(), look));
            }
        }
    }

    /**
     * Combine a bone's look with the model's: a bone shows when both do, glows or shimmers when
     * either does, and its own tint, glow color, billboard and light win over the model's.
     *
     * @param bone the bone
     * @return the look
     */
    private DisplayLook lookOf(final HyperionBone bone) {
        LookState own = bone.look();
        @Nullable Color tint = own.tint();
        @Nullable Color glowColor = own.glowColor();
        Display.@Nullable Billboard billboard = own.billboard();
        Display.@Nullable Brightness brightness = own.brightness();
        Display.@Nullable Billboard modelBillboard = this.look.billboard();

        return new DisplayLook(
                this.look.visible() && own.visible(),
                tint != null ? tint : this.look.tint(),
                this.look.glint() || own.glint(),
                this.look.glowing() || own.glowing(),
                glowColor != null ? glowColor : this.look.glowColor(),
                billboard != null ? billboard : modelBillboard != null ? modelBillboard : Display.Billboard.FIXED,
                brightness != null ? brightness : this.look.brightness()
        );
    }

    /**
     * Return a look that shows nothing, for the parts of a bone that shows an item instead.
     *
     * @param look the bone's look
     * @return the same look, hidden
     */
    private static DisplayLook hidden(final DisplayLook look) {
        return new DisplayLook(
                false,
                look.tint(),
                look.glint(),
                look.glowing(),
                look.glowColor(),
                look.billboard(),
                look.brightness()
        );
    }

    /**
     * Build the packets every viewer who already sees the model needs this frame.
     *
     * @param here where the model is
     * @return the packets, in order
     */
    private List<PacketWrapper<?>> changes(final Location here) {
        List<PacketWrapper<?>> packets = new ArrayList<>();
        if (!this.dropped.isEmpty()) {
            int[] ids = new int[this.dropped.size()];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = this.dropped.get(i).entityId();
            }

            packets.add(Packets.destroy(ids));
        }

        for (PartDisplay display : this.displays) {
            if (this.added.contains(display)) {
                packets.add(Packets.spawn(display.entityId(), display.uniqueId(), here));
                packets.add(Packets.metadata(display.entityId(), this.spawnData(display, display.item())));
                continue;
            }

            List<EntityData<?>> data = new ArrayList<>();
            DisplayTransform current = display.transform();
            if (current != null && display.moved(EPSILON)) {
                DisplayData.transform(data, current, this.interpolation);
            }

            DisplayLook look = display.look();
            if (look != null && display.restyled()) {
                DisplayData.look(data, look);
            }

            if (display.itemChanged()) {
                DisplayData.item(data, display.item());
            }

            if (!data.isEmpty()) {
                packets.add(Packets.metadata(display.entityId(), data));
            }
        }

        if (this.teleported && this.entity == null) {
            for (PartDisplay display : this.displays) {
                packets.add(Packets.teleport(display.entityId(), here));
            }
        }

        return packets;
    }

    /**
     * Build the entity data a display spawns with.
     *
     * @param display the display
     * @param item the item this viewer sees on it
     * @return the data
     */
    private List<EntityData<?>> spawnData(final PartDisplay display, final ItemStack item) {
        List<EntityData<?>> data = new ArrayList<>();
        DisplayData.spawn(
                data,
                Objects.requireNonNull(display.transform()),
                Objects.requireNonNull(display.look()),
                item,
                this.interpolation,
                this.viewers.viewDistance()
        );

        return data;
    }

    /**
     * Send a viewer every display, and on an entity, mount them and hide the entity's parts.
     *
     * @param viewer the viewer
     * @param here where the model is
     */
    private void spawnFor(final Viewer viewer, final Location here) {
        for (PartDisplay display : this.displays) {
            Packets.send(viewer.user(), Packets.spawn(display.entityId(), display.uniqueId(), here));
            ItemStack item = this.itemFor(display, viewer);
            Packets.send(viewer.user(), Packets.metadata(display.entityId(), this.spawnData(display, item)));
        }

        if (this.entity != null) {
            int[] riders = this.manager.passengers(this.entity, viewer.player().getUniqueId());
            Packets.send(viewer.user(), Packets.passengers(this.entity.getEntityId(), riders));
            this.sendEntity(viewer, this.entity);
        }
    }

    /**
     * Send a viewer the entity with the parts every model they see on it hides.
     *
     * @param viewer the viewer
     * @param carrier the entity
     */
    private void sendEntity(final Viewer viewer, final Entity carrier) {
        Set<EntityPart> parts = this.manager.hiddenFor(carrier.getEntityId(), viewer.player().getUniqueId());
        for (PacketWrapper<?> packet : BaseEntityData.packets(carrier, parts)) {
            Packets.send(viewer.user(), packet);
        }
    }

    /**
     * Tell listeners and behaviors a player started seeing the model.
     *
     * @param viewer the new viewer
     */
    private void announce(final Viewer viewer) {
        Bukkit.getPluginManager().callEvent(new ModelViewerEvent(this, viewer.player(), true));
        for (HyperionBone bone : this.bones) {
            for (BoneBehavior behavior : bone.behaviors()) {
                this.runBehavior(bone, behavior, each -> each.onShow(viewer.player()));
            }
        }
    }

    /**
     * Return the item one viewer sees on a display: the shared one, or theirs for a per-viewer
     * source.
     *
     * @param display the display
     * @param viewer the viewer
     * @return the item
     */
    private ItemStack itemFor(final PartDisplay display, final Viewer viewer) {
        HyperionBone bone = this.boneOf(display);
        @Nullable ItemSource source = bone == null ? null : bone.itemSource();
        if (bone == null || source == null || !source.perViewer()) {
            return display.item();
        }

        ItemStack item = this.manager.items().given(
                source.item(new ItemContext(bone, viewer.player())),
                Objects.requireNonNull(display.look())
        );
        display.viewerItems().put(viewer.player().getUniqueId(), item);

        return item;
    }

    /**
     * Send a viewer the items of per-viewer sources that changed for them, when due.
     *
     * @param viewer the viewer
     */
    private void sendViewerItems(final Viewer viewer) {
        for (HyperionBone bone : this.bones) {
            PartDisplay display = bone.itemDisplay();
            ItemSource source = bone.itemSource();
            if (display == null || source == null || !source.perViewer()) {
                continue;
            }

            if (this.ticks % source.interval() != 0 && !display.viewerItemsStale()) {
                continue;
            }

            ItemStack before = display.viewerItems().get(viewer.player().getUniqueId());
            ItemStack after = this.itemFor(display, viewer);
            if (!Objects.equals(after, before)) {
                List<EntityData<?>> data = new ArrayList<>(1);
                DisplayData.item(data, after);
                Packets.send(viewer.user(), Packets.metadata(display.entityId(), data));
            }
        }
    }

    /**
     * Find the bone a display belongs to.
     *
     * @param display the display
     * @return the bone, or null when the display is no longer part of the model
     */
    private @Nullable HyperionBone boneOf(final PartDisplay display) {
        for (HyperionBone bone : this.bones) {
            if (bone.itemDisplay() == display || bone.displays().contains(display)) {
                return bone;
            }
        }

        return null;
    }

    /**
     * Give each bone with an item source an item display, and take it from a bone that lost its
     * source.
     */
    private void syncItemDisplays() {
        for (HyperionBone bone : this.bones) {
            ItemSource source = bone.itemSource();
            PartDisplay display = bone.itemDisplay();
            if (source != null && display == null) {
                PartDisplay created = new PartDisplay(null);
                bone.itemDisplay(created);
                bone.markItemDirty();
                this.displays.add(created);
                this.added.add(created);
                this.lookDirty = true;
            } else if (source == null && display != null) {
                bone.itemDisplay(null);
                this.displays.remove(display);
                this.dropped.add(display);
                this.lookDirty = true;
            }
        }

        if (!this.added.isEmpty() || !this.dropped.isEmpty()) {
            this.displayIds = ids(this.displays);
        }
    }

    /**
     * Ask every shared item source for its item when its interval is due, and note the bones
     * whose item changed.
     */
    private void updateItems() {
        for (HyperionBone bone : this.bones) {
            ItemSource source = bone.itemSource();
            if (source == null || source.perViewer() || (this.ticks % source.interval() != 0 && !bone.itemDirty())) {
                continue;
            }

            // Bukkit's ItemStack is qualified: this class sends PacketEvents' ItemStack, of the same name.
            org.bukkit.inventory.ItemStack before = bone.currentItem();
            org.bukkit.inventory.ItemStack after = source.item(new ItemContext(bone, null));
            if (!Objects.equals(before, after) || bone.itemDirty()) {
                bone.currentItem(after == null ? null : after.clone());
                bone.markItemDirty();
                this.lookDirty = true;
            }
        }
    }

    /**
     * Run every behavior's tick.
     */
    private void tickBehaviors() {
        for (HyperionBone bone : this.bones) {
            for (BoneBehavior behavior : bone.behaviors()) {
                this.runBehavior(bone, behavior, BoneBehavior::onTick);
            }
        }
    }

    /**
     * Run a behavior hook, logging what it throws so one broken behavior cannot stop the frame.
     *
     * @param bone the bone the behavior is on
     * @param behavior the behavior
     * @param hook the hook to run
     */
    private void runBehavior(
            final HyperionBone bone,
            final BoneBehavior behavior,
            final Consumer<BoneBehavior> hook
    ) {
        try {
            hook.accept(behavior);
        } catch (RuntimeException exception) {
            String message = String.format(
                    BEHAVIOR_FAILED,
                    behavior.getClass().getName(),
                    bone.name(),
                    this.blueprint.key().asString()
            );
            this.manager.plugin().getLogger().log(Level.WARNING, message, exception);
        }
    }

    // Building

    /**
     * Build the bones of a blueprint, keeping a bone whose name was already in the model, and
     * the locators.
     *
     * @param next the blueprint
     * @param spawned whether the model already spawned, so new behaviors are told at once
     */
    private void buildBones(final Blueprint next, final boolean spawned) {
        Map<String, HyperionBone> previous = this.bonesByName;
        Map<String, HyperionBone> byName = new LinkedHashMap<>();
        List<HyperionBone> created = new ArrayList<>();
        for (BlueprintBone shape : next.blueprintBones()) {
            HyperionBone bone = previous.get(shape.name());
            if (bone == null) {
                bone = new HyperionBone(this, shape);
                created.add(bone);
            } else {
                bone.blueprint(shape);
            }

            byName.put(shape.name(), bone);
        }

        for (HyperionBone bone : byName.values()) {
            BlueprintBone shape = bone.blueprintBone();
            HyperionBone parent = named(byName, shape.parent());
            List<HyperionBone> children = new ArrayList<>();
            for (BoneBlueprint child : shape.children()) {
                HyperionBone found = byName.get(child.name());
                if (found != null) {
                    children.add(found);
                }
            }

            bone.link(parent, children);
        }

        for (Map.Entry<String, HyperionBone> entry : previous.entrySet()) {
            if (!byName.containsKey(entry.getKey())) {
                for (BoneBehavior behavior : entry.getValue().behaviors()) {
                    this.runBehavior(entry.getValue(), behavior, each -> each.onRemove(RemovalReason.REMOVED));
                }
            }
        }

        this.bones = List.copyOf(byName.values());
        this.bonesByName = Map.copyOf(byName);
        this.locators = this.buildLocators(next, byName);
        for (HyperionBone bone : created) {
            bone.behaviors(this.createBehaviors(bone));
            if (spawned) {
                for (BoneBehavior behavior : bone.behaviors()) {
                    this.runBehavior(bone, behavior, BoneBehavior::onSpawn);
                }
            }
        }
    }

    /**
     * Build the locators of a blueprint, each on its bone.
     *
     * @param next the blueprint
     * @param byName the bones by name
     * @return the locators
     */
    private List<HyperionLocator> buildLocators(final Blueprint next, final Map<String, HyperionBone> byName) {
        List<HyperionLocator> built = new ArrayList<>();
        for (BlueprintLocator shape : next.blueprintLocators()) {
            HyperionBone bone = named(byName, shape.parent());
            built.add(new HyperionLocator(this, shape, bone));
        }

        return List.copyOf(built);
    }

    /**
     * Find the bone of a blueprint's bone by name.
     *
     * @param byName the bones by name
     * @param shape the blueprint's bone, or empty
     * @return the bone, or null when there is none
     */
    private static @Nullable HyperionBone named(final Map<String, HyperionBone> byName, final Optional<BoneBlueprint> shape) {
        return shape.isPresent() ? byName.get(shape.get().name()) : null;
    }

    /**
     * Create every registered behavior whose matcher takes a bone.
     *
     * @param bone the bone
     * @return the behaviors
     */
    private List<BoneBehavior> createBehaviors(final HyperionBone bone) {
        List<BoneBehavior> created = new ArrayList<>();
        for (BehaviorType<?> type : this.manager.behaviors().all()) {
            try {
                if (type.matcher().matches(bone.blueprint())) {
                    created.add(type.create(bone));
                }
            } catch (RuntimeException exception) {
                String message = String.format(CREATE_FAILED, type.key().asString(), bone.name(), this.blueprint.key().asString());
                this.manager.plugin().getLogger().log(Level.WARNING, message, exception);
            }
        }

        return created;
    }

    /**
     * Make a display for every part the pack has for each bone.
     */
    private void buildDisplays() {
        this.displays.clear();
        ResourcePack current = this.pack;
        for (HyperionBone bone : this.bones) {
            bone.displays().clear();
            bone.itemDisplay(null);
            if (current == null) {
                continue;
            }

            for (ModelPart part : current.parts(this.blueprint.key(), bone.name())) {
                PartDisplay display = new PartDisplay(part);
                bone.displays().add(display);
                this.displays.add(display);
            }
        }

        this.displayIds = ids(this.displays);
    }

    /**
     * Follow a new pack and, when it changed, a reloaded blueprint: remove the old displays from
     * every viewer and send them the new ones on the next frame. Call on the owning thread.
     *
     * @param next the blueprint now registered under this model's key
     * @param nextPack the new pack
     * @param force true to send the displays again even when neither changed, as after the
     *     base item changed
     */
    void refresh(final Blueprint next, final ResourcePack nextPack, final boolean force) {
        if (this.removed || (!force && next == this.blueprint && nextPack == this.pack)) {
            return;
        }

        for (Viewer viewer : this.viewers.viewers()) {
            if (!this.joined.contains(viewer)) {
                Packets.send(viewer.user(), Packets.destroy(this.displayIds));
                this.respawn.add(viewer);
            }
        }

        if (next != this.blueprint) {
            this.blueprint = next;
            this.buildBones(next, true);
        }

        this.pack = nextPack;
        this.buildDisplays();
        this.moveDirty = true;
        this.lookDirty = true;
    }

    // Removal

    /**
     * Remove the model now: from every viewer, its behaviors and the tracker, then tell
     * listeners. Call on the owning thread.
     *
     * @param reason why it is removed
     */
    void removeNow(final RemovalReason reason) {
        if (this.removed) {
            return;
        }

        this.removed = true;
        this.cancelTask();
        for (Viewer viewer : this.viewers.snapshot()) {
            Packets.send(viewer.user(), Packets.destroy(this.displayIds));
            this.viewers.remove(viewer.player().getUniqueId());
            if (this.entity != null) {
                this.restoreEntity(viewer, this.entity);
            }
        }

        for (HyperionBone bone : this.bones) {
            for (BoneBehavior behavior : bone.behaviors()) {
                this.runBehavior(bone, behavior, each -> each.onRemove(reason));
            }
        }

        this.manager.forget(this);
        Bukkit.getPluginManager().callEvent(new ModelRemoveEvent(this, reason));
    }

    /**
     * Remove the model on the thread that owns it, from any thread.
     *
     * @param reason why it is removed
     */
    void removeLater(final RemovalReason reason) {
        this.runOnOwner(() -> this.removeNow(reason));
    }

    /**
     * Take the model down from a thread that may not own it, as when the plugin disables on a
     * Canvas server: the displays go and the tracker forgets it, but no event fires and no
     * behavior runs, since both expect the owning thread.
     */
    void tearDown() {
        if (this.removed) {
            return;
        }

        this.removed = true;
        this.cancelTask();
        for (Viewer viewer : this.viewers.snapshot()) {
            Packets.send(viewer.user(), Packets.destroy(this.displayIds));
        }

        this.manager.forget(this);
    }

    /**
     * Return whether the calling thread owns the model.
     *
     * @return true on the entity's or the location's region
     */
    boolean isOwnedHere() {
        return this.entity != null ? Bukkit.isOwnedByCurrentRegion(this.entity) : Bukkit.isOwnedByCurrentRegion(this.origin);
    }

    /**
     * Run something on the thread that owns the model.
     *
     * @param task what to run
     */
    void runOnOwner(final Runnable task) {
        if (this.entity != null) {
            this.entity.getScheduler().execute(this.manager.plugin(), task, null, 1L);
            return;
        }

        Bukkit.getRegionScheduler().execute(this.manager.plugin(), this.origin, task);
    }

    /**
     * Start the frame task on the owning scheduler.
     */
    private void schedule() {
        if (this.entity != null) {
            this.task = this.entity.getScheduler().runAtFixedRate(
                    this.manager.plugin(),
                    scheduled -> this.frame(),
                    () -> this.removeNow(RemovalReason.ENTITY_REMOVED),
                    1L,
                    1L
            );
            return;
        }

        this.task = Bukkit.getRegionScheduler().runAtFixedRate(
                this.manager.plugin(),
                this.origin,
                scheduled -> this.frame(),
                1L,
                1L
        );
    }

    /**
     * Stop the frame task.
     */
    private void cancelTask() {
        ScheduledTask current = this.task;
        if (current != null) {
            current.cancel();
            this.task = null;
        }
    }

    // Entity hooks

    /**
     * Let a player who just started tracking the entity see the model, when they may. Runs on
     * the entity's thread, before the entity is sent, so its parts are hidden from the start.
     *
     * @param player the player
     */
    void tracked(final Player player) {
        if (!this.removed && !this.viewers.isViewing(player) && this.viewers.wants(player, this.location)) {
            this.join(player);
        }
    }

    /**
     * Stop showing the model to a player who stopped tracking the entity.
     *
     * @param player the player
     */
    void untracked(final Player player) {
        for (Viewer viewer : this.viewers.snapshot()) {
            if (viewer.player().equals(player)) {
                this.hideFrom(viewer, false);
            }
        }
    }

    /**
     * Forget a player whose client dropped every entity: they left, changed worlds or respawned.
     *
     * @param player the player's id
     * @param left true when they left the server, which also drops a show or hide for them
     */
    void lose(final UUID player, final boolean left) {
        if (left) {
            this.viewers.forget(player);
        } else {
            this.viewers.remove(player);
        }

        for (PartDisplay display : List.copyOf(this.displays)) {
            display.viewerItems().remove(player);
        }
    }

    /**
     * Return whether a player sees the model now. Safe from any thread.
     *
     * @param player the player's id
     * @return true when they see it
     */
    boolean seenBy(final UUID player) {
        return this.viewers.isViewing(player);
    }

    /**
     * Return the entity ids of the displays, for mounting them on the entity.
     *
     * @return the ids
     */
    int[] displayIds() {
        return this.displayIds;
    }

    /**
     * Return the entity carrying the model.
     *
     * @return the entity, or null for a model on its own
     */
    @Nullable Entity entity() {
        return this.entity;
    }

    /**
     * Return the root matrix of the last frame, for locators without a bone.
     *
     * @return the matrix, never changed once published
     */
    Matrix4fc rootWorld() {
        return this.rootWorld;
    }

    /**
     * Note that the look changed, for the next frame to send.
     */
    void lookChanged() {
        this.lookDirty = true;
    }

    /**
     * Note that something that places the bones changed, for the next frame to send.
     */
    void moved() {
        this.moveDirty = true;
    }

    /**
     * Return the ids of some displays.
     *
     * @param displays the displays
     * @return their entity ids
     */
    private static int[] ids(final List<PartDisplay> displays) {
        int[] ids = new int[displays.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = displays.get(i).entityId();
        }

        return ids;
    }

    // Api

    @Override
    public UUID uniqueId() {
        return this.uniqueId;
    }

    @Override
    public ModelBlueprint blueprint() {
        return this.blueprint;
    }

    /**
     * Return the blueprint as the plugin's own type.
     *
     * @return the blueprint
     */
    Blueprint currentBlueprint() {
        return this.blueprint;
    }

    @Override
    public ModelAnchor anchor() {
        return this.entity != null ? new EntityAnchor(this.entity) : new LocationAnchor(this.origin);
    }

    @Override
    public Location location() {
        return this.location.clone();
    }

    @Override
    public List<Bone> bones() {
        return List.copyOf(this.bones);
    }

    @Override
    public Optional<Bone> bone(final String name) {
        return Optional.ofNullable(this.bonesByName.get(name));
    }

    @Override
    public List<Bone> bones(final Predicate<Bone> filter) {
        List<Bone> matches = new ArrayList<>();
        for (HyperionBone bone : this.bones) {
            if (filter.test(bone)) {
                matches.add(bone);
            }
        }

        return List.copyOf(matches);
    }

    @Override
    public List<Locator> locators() {
        return List.copyOf(this.locators);
    }

    @Override
    public Optional<Locator> locator(final String name) {
        for (HyperionLocator locator : this.locators) {
            if (locator.name().equals(name)) {
                return Optional.of(locator);
            }
        }

        return Optional.empty();
    }

    @Override
    public BoneTransform transform() {
        return this.transform;
    }

    @Override
    public Model transform(final BoneTransform transform) {
        this.transform = transform;
        this.moveDirty = true;

        return this;
    }

    @Override
    public float scale() {
        return this.scale;
    }

    @Override
    public Model scale(final float scale) {
        if (!(scale > 0f)) {
            throw new IllegalArgumentException(String.format(SCALE_NOT_POSITIVE, scale));
        }

        return this.scaler(ModelScaler.fixed(scale));
    }

    @Override
    public ModelScaler scaler() {
        return this.scaler;
    }

    @Override
    public Model scaler(final ModelScaler scaler) {
        this.scaler = scaler;
        this.moveDirty = true;

        return this;
    }

    @Override
    public ModelRotation rotation() {
        return this.rotation;
    }

    @Override
    public ModelRotator rotator() {
        return this.rotator;
    }

    @Override
    public Model rotator(final ModelRotator rotator) {
        this.rotator = rotator;
        this.moveDirty = true;

        return this;
    }

    @Override
    public int interpolation() {
        return this.interpolation;
    }

    @Override
    public Model interpolation(final int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException(String.format(NEGATIVE_TICKS, ticks));
        }

        this.interpolation = ticks;

        return this;
    }

    @Override
    public Set<EntityPart> hiddenParts() {
        return this.hiddenParts;
    }

    @Override
    public Model hiddenParts(final Set<EntityPart> parts) {
        if (this.entity != null) {
            this.hiddenParts = Set.copyOf(parts);
            this.hiddenDirty = true;
        }

        return this;
    }

    @Override
    public Model hideBaseEntity(final boolean hidden) {
        return this.hiddenParts(hidden ? EntityPart.ALL : EntityPart.NONE);
    }

    @Override
    public void teleport(final Location target) {
        if (this.entity != null) {
            throw new IllegalStateException(ON_ENTITY);
        }

        if (this.removed) {
            throw new IllegalStateException(REMOVED);
        }

        if (target.getWorld() == null) {
            throw new IllegalArgumentException(NO_WORLD);
        }

        if (!Bukkit.isOwnedByCurrentRegion(this.origin)) {
            throw new IllegalStateException(NOT_OWNER);
        }

        Location to = target.clone();
        boolean otherWorld = !to.getWorld().equals(this.origin.getWorld());
        if (otherWorld) {
            for (Viewer viewer : this.viewers.snapshot()) {
                this.hideFrom(viewer, false);
            }
        }

        this.origin = to;
        this.location = to.clone();
        this.teleported = !otherWorld;
        if (Bukkit.isOwnedByCurrentRegion(to)) {
            return;
        }

        // The new region takes the frames over; until it runs them the model only waits.
        this.cancelTask();
        Bukkit.getRegionScheduler().execute(this.manager.plugin(), to, () -> {
            if (!this.removed) {
                this.schedule();
            }
        });
    }

    @Override
    public Viewers viewers() {
        return this.viewers;
    }

    @Override
    public PlayerView view(final Player player) {
        throw new UnsupportedOperationException(NO_VIEWS);
    }

    @Override
    public Animator animator() {
        return PendingAnimator.INSTANCE;
    }

    @Override
    public boolean isRemoved() {
        return this.removed;
    }

    @Override
    public void remove() {
        if (!this.isOwnedHere()) {
            throw new IllegalStateException(NOT_OWNER);
        }

        this.removeNow(RemovalReason.REMOVED);
    }

    @Override
    public boolean visible() {
        return this.look.visible();
    }

    @Override
    public Model visible(final boolean visible) {
        this.look.visible(visible);

        return this;
    }

    @Override
    public @Nullable Color tint() {
        return this.look.tint();
    }

    @Override
    public Model tint(final @Nullable Color tint) {
        this.look.tint(tint);

        return this;
    }

    @Override
    public boolean glowing() {
        return this.look.glowing();
    }

    @Override
    public Model glowing(final boolean glowing) {
        this.look.glowing(glowing);

        return this;
    }

    @Override
    public @Nullable Color glowColor() {
        return this.look.glowColor();
    }

    @Override
    public Model glowColor(final @Nullable Color color) {
        this.look.glowColor(color);

        return this;
    }

    @Override
    public boolean glint() {
        return this.look.glint();
    }

    @Override
    public Model glint(final boolean glint) {
        this.look.glint(glint);

        return this;
    }

    @Override
    public Display.@Nullable Billboard billboard() {
        return this.look.billboard();
    }

    @Override
    public Model billboard(final Display.@Nullable Billboard billboard) {
        this.look.billboard(billboard);

        return this;
    }

    @Override
    public Display.@Nullable Brightness brightness() {
        return this.look.brightness();
    }

    @Override
    public Model brightness(final Display.@Nullable Brightness brightness) {
        this.look.brightness(brightness);

        return this;
    }

    @Override
    public String toString() {
        return "HyperionModel[" + this.blueprint.key().asString() + ", " + this.uniqueId + "]";
    }
}
