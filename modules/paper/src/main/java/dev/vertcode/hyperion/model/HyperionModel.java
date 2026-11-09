package dev.vertcode.hyperion.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
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
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.animation.BonePose;
import dev.vertcode.hyperion.animation.ModelAnimator;
import dev.vertcode.hyperion.api.animation.AnimationHandle;
import dev.vertcode.hyperion.api.animation.Animator;
import dev.vertcode.hyperion.api.animation.LoopMode;
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
import dev.vertcode.hyperion.api.model.behavior.builtin.GhostBehavior;
import dev.vertcode.hyperion.api.model.behavior.builtin.SeatBehavior;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.model.bone.BoneTransform;
import dev.vertcode.hyperion.api.model.bone.ItemContext;
import dev.vertcode.hyperion.api.model.bone.ItemSource;
import dev.vertcode.hyperion.api.model.hitbox.HitboxBehavior;
import dev.vertcode.hyperion.api.model.hitbox.HitboxMode;
import dev.vertcode.hyperion.api.model.locator.Locator;
import dev.vertcode.hyperion.api.model.view.PlayerView;
import dev.vertcode.hyperion.api.model.view.Viewers;
import dev.vertcode.hyperion.bedrock.BedrockIds;
import dev.vertcode.hyperion.bedrock.BedrockLink;
import dev.vertcode.hyperion.bedrock.BedrockPackBuilder;
import dev.vertcode.hyperion.bedrock.BedrockState;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintBone;
import dev.vertcode.hyperion.blueprint.BlueprintLocator;
import dev.vertcode.hyperion.config.AutoAnimations;
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
    private static final double MOVE_THRESHOLD = 0.0004d;
    private static final int STILL_TICKS = 4;
    private static final int AUTO_FADE = 4;
    private static final String BEHAVIOR_FAILED = "The behavior %s on bone %s of %s failed";
    private static final String CREATE_FAILED = "The behavior type %s could not be created for bone %s of %s";
    private static final String TASK_FAILED = "An effect on %s failed and was stopped";

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
    private final Quaternionf turn = new Quaternionf();
    private final ModelAnimator animator;
    private final Map<UUID, HyperionPlayerView> views = new ConcurrentHashMap<>();
    private final @Nullable String startAnimation;
    private volatile boolean autoAnimations;
    private BonePose[] poses = new BonePose[0];
    private Matrix4f[] matrices = new Matrix4f[0];
    private Matrix4f[] viewMatrices = new Matrix4f[0];
    private int[] parentIndex = new int[0];
    private Map<String, Integer> boneIndex = Map.of();
    private @Nullable AnimationHandle autoPlaying;
    private boolean animatedBefore;
    private boolean moving;
    private boolean lookChangedThisFrame;
    private final int standIn = SpigotReflectionUtil.generateEntityId();
    private @Nullable BedrockState bedrockState;
    private final HitboxMode hitboxMode;
    private final List<ModelTask> tasks = new ArrayList<>();
    private @Nullable ModelBox box;
    private int stillTicks = STILL_TICKS;
    private double lastX;
    private double lastZ;

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
        this.hitboxMode = options.hitboxMode != null ? options.hitboxMode : manager.settings().behaviors().hitboxMode();
        this.buildBones(blueprint, false);
        this.buildDisplays();
        this.animator = this.newAnimator(null);
        this.startAnimation = options.animation;
        Boolean auto = options.autoAnimations;
        this.autoAnimations = this.entity != null
                && (auto != null ? auto : manager.settings().autoAnimations().enabledFor(this.entity.getType()));
        this.lastX = this.origin.getX();
        this.lastZ = this.origin.getZ();
    }

    /**
     * Start the frames, tell every behavior the model spawned, and play the animation the spawn
     * options asked for. Call on the owning thread.
     */
    void start() {
        this.schedule();
        for (HyperionBone bone : this.bones) {
            for (BoneBehavior behavior : bone.behaviors()) {
                this.runBehavior(bone, behavior, BoneBehavior::onSpawn);
            }
        }

        this.syncBox();
        if (this.startAnimation != null) {
            this.animator.play(this.startAnimation);
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

        this.autoAnimate(here);
        this.animator.tick();
        for (HyperionPlayerView view : this.views.values()) {
            view.modelAnimator().tick();
        }

        this.updateViewers(here);
        if (this.viewers.isEmpty()) {
            this.runTasks();
            return;
        }

        this.render(here);
        this.publishBedrock();
        this.afterFrame();
        this.runTasks();
    }

    /**
     * Run the tasks attached to the model, such as effects on it, dropping those that are done.
     */
    private void runTasks() {
        if (this.tasks.isEmpty()) {
            return;
        }

        for (ModelTask task : List.copyOf(this.tasks)) {
            boolean going;
            try {
                going = task.step();
            } catch (RuntimeException exception) {
                this.manager.plugin().getLogger().log(Level.WARNING, String.format(TASK_FAILED, this), exception);
                going = false;
            }

            if (!going) {
                this.tasks.remove(task);
            }
        }
    }

    /**
     * Run a task inside every frame from the next one on, until it is done. Call on the owning
     * thread.
     *
     * @param task the task
     */
    void attach(final ModelTask task) {
        if (this.removed) {
            task.cancel();
            return;
        }

        this.tasks.add(task);
    }

    /**
     * Let the behaviors and the box around the model place their own entities, now that the
     * bones are where this frame put them.
     */
    private void afterFrame() {
        List<User> java = new ArrayList<>();
        for (Viewer viewer : this.viewers.viewers()) {
            if (!viewer.bedrock()) {
                java.add(viewer.user());
            }
        }

        for (HyperionBone bone : this.bones) {
            for (BoneBehavior behavior : bone.behaviors()) {
                if (behavior instanceof FrameBehavior placed) {
                    this.runBehavior(bone, behavior, each -> placed.afterFrame(java));
                }
            }
        }

        ModelBox current = this.box;
        if (current != null) {
            current.place(java);
        }
    }

    /**
     * Give the model one box around all of it when its hitbox mode asks for one, or when it has
     * no hitbox bones, and take the box away when neither holds anymore.
     */
    private void syncBox() {
        boolean wanted = this.hitboxMode == HitboxMode.MODEL || this.behaviors(HitboxBehavior.class).isEmpty();
        ModelBox current = this.box;
        if (wanted && current == null) {
            boolean forwards = this.manager.settings().behaviors().forwardDamage();
            this.box = new ModelBox(this, this.manager.interactions(), forwards);
        } else if (!wanted && current != null) {
            current.remove();
            this.box = null;
        }
    }

    /**
     * Play the idle animation while the entity stands and the walk animation while it moves,
     * when automatic animations are on and the model has them. The entity counts as standing
     * after a few ticks without moving, so a mob that steps now and then does not flicker
     * between the two.
     *
     * @param here where the entity is
     */
    private void autoAnimate(final Location here) {
        if (!this.autoAnimations) {
            return;
        }

        double dx = here.getX() - this.lastX;
        double dz = here.getZ() - this.lastZ;
        this.lastX = here.getX();
        this.lastZ = here.getZ();
        this.stillTicks = dx * dx + dz * dz > MOVE_THRESHOLD ? 0 : this.stillTicks + 1;
        AutoAnimations names = this.manager.settings().autoAnimations();
        Set<String> available = this.blueprint.animations();
        String wanted = this.stillTicks < STILL_TICKS && available.contains(names.walk()) ? names.walk() : names.idle();
        if (!available.contains(wanted)) {
            wanted = null;
        }

        AnimationHandle current = this.autoPlaying;
        if (current != null && current.isPlaying() && current.name().equals(wanted)) {
            return;
        }

        if (current != null) {
            current.stop();
        }

        this.autoPlaying = wanted == null
                ? null
                : this.animator.play(wanted, options -> options.loop(LoopMode.LOOP).fadeIn(AUTO_FADE).fadeOut(AUTO_FADE));
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
            this.joined.add(this.viewers.add(player, user, this.manager.isBedrock(player.getUniqueId())));
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

        if (viewer.bedrock()) {
            this.dropBedrock(List.of(viewer));
        } else {
            Packets.send(viewer.user(), Packets.destroy(this.displayIds));
            if (restoreEntity && this.entity != null) {
                this.restoreEntity(viewer, this.entity);
            }
        }

        ModelBox current = this.box;
        if (current != null) {
            current.hide(viewer.player().getUniqueId());
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
     * A viewer whose view changes something is rendered on their own.
     *
     * @param here where the model is
     */
    private void render(final Location here) {
        this.moving = false;
        this.lookChangedThisFrame = false;
        this.syncItemDisplays();
        this.updateItems();
        boolean everything = !this.joined.isEmpty() || !this.respawn.isEmpty() || !this.added.isEmpty();
        boolean animated = this.animator.active() || this.animatedBefore;
        this.animatedBefore = this.animator.active();
        if (everything || animated || this.needsMove()) {
            this.computePoses(this.poses, null);
            this.computeTransforms(this.poses, null, this.matrices, PartDisplay::transform);
        }

        if (everything || this.lookDirty) {
            this.lookDirty = false;
            this.computeLooks();
        }

        List<PacketWrapper<?>> structure = this.structure(here);
        List<PacketWrapper<?>> shared = this.changes();
        boolean hidden = this.hiddenDirty;
        this.hiddenDirty = false;
        boolean restructured = !this.added.isEmpty() || !this.dropped.isEmpty();
        for (Viewer viewer : this.viewers.viewers()) {
            if (this.joined.contains(viewer) || this.respawn.contains(viewer)) {
                continue;
            }

            if (viewer.bedrock()) {
                if (this.teleported && this.entity == null) {
                    Packets.send(viewer.user(), Packets.teleport(this.standIn, here));
                }

                continue;
            }

            for (PacketWrapper<?> packet : structure) {
                Packets.send(viewer.user(), packet);
            }

            HyperionPlayerView view = this.views.get(viewer.player().getUniqueId());
            if (view != null && view.active()) {
                this.renderView(viewer, view, here, false);
            } else {
                this.renderShared(viewer, view, shared, here);
            }

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
     * Send a viewer who sees what everyone sees this frame's shared changes. A viewer who was
     * rendered on their own until now gets the whole shared state back first.
     *
     * @param viewer the viewer
     * @param view the viewer's view, or null without one
     * @param shared the shared changes
     * @param here where the model is
     */
    private void renderShared(
            final Viewer viewer,
            final @Nullable HyperionPlayerView view,
            final List<PacketWrapper<?>> shared,
            final Location here
    ) {
        boolean resync = view != null && view.rendered();
        if (view != null) {
            view.rendered(false);
        }

        for (PartDisplay display : this.displays) {
            if (this.added.contains(display)) {
                Packets.send(viewer.user(), Packets.spawn(display.entityId(), display.uniqueId(), here));
                Packets.send(viewer.user(), Packets.metadata(display.entityId(), this.spawnData(display, this.itemFor(display, viewer))));
            } else if (resync) {
                Packets.send(viewer.user(), Packets.metadata(display.entityId(), this.fullData(display, this.itemFor(display, viewer))));
            }
        }

        if (!resync) {
            for (PacketWrapper<?> packet : shared) {
                Packets.send(viewer.user(), packet);
            }
        }

        this.sendViewerItems(viewer);
    }

    /**
     * Render one viewer on their own: their animations and overrides on top of the shared
     * state, compared with what they were last sent.
     *
     * @param viewer the viewer
     * @param view the viewer's view
     * @param here where the model is
     * @param spawn true to spawn every display for them, as for a new viewer
     */
    private void renderView(final Viewer viewer, final HyperionPlayerView view, final Location here, final boolean spawn) {
        if (!view.rendered()) {
            view.rendered(true);
            if (!spawn) {
                for (PartDisplay display : this.displays) {
                    if (!this.added.contains(display)) {
                        view.seed(display, this.itemFor(display, viewer));
                    }
                }
            }
        }

        boolean moveDirty = view.takeMoveDirty();
        boolean animated = view.modelAnimator().active() || view.animatedBefore();
        view.animatedBefore(view.modelAnimator().active());
        Map<PartDisplay, DisplayTransform> own = view.transforms();
        if (animated || view.movesBones()) {
            if (moveDirty || animated || this.moving) {
                BonePose[] viewPoses = view.poses(this.bones.size());
                this.computePoses(viewPoses, view.modelAnimator());
                this.computeTransforms(viewPoses, view, this.viewMatrices, own::put);
            }
        } else {
            own.clear();
        }

        boolean restyle = view.takeLookDirty() || this.lookChangedThisFrame || spawn;
        PartItems items = this.manager.items();
        for (HyperionBone bone : this.bones) {
            DisplayLook look = this.lookOf(bone, view);
            HyperionViewBone override = view.existing(bone.name());
            ItemSource source = override != null && override.itemSourceOverride() != null
                    ? override.itemSourceOverride()
                    : bone.itemSource();
            DisplayLook partLook = source == null ? look : hidden(look);
            for (PartDisplay display : bone.displays()) {
                DisplayLook before = view.sentLooks().get(display);
                ItemStack item = null;
                if (restyle || before == null || partLook.itemDiffers(before)) {
                    item = items.part(Objects.requireNonNull(display.part()), partLook);
                }

                this.sendView(viewer, view, display, own.getOrDefault(display, display.transform()), partLook, item, spawn, here);
            }

            PartDisplay itemDisplay = bone.itemDisplay();
            if (itemDisplay == null) {
                continue;
            }

            ItemStack item = null;
            boolean due = source != null && this.ticks % source.interval() == 0;
            if (restyle || due || !view.sentItems().containsKey(itemDisplay)) {
                // Bukkit's ItemStack is qualified: this class sends PacketEvents' ItemStack, of the same name.
                org.bukkit.inventory.ItemStack picked = source == null ? null : source.item(new ItemContext(bone, viewer.player()));
                item = items.given(picked, look);
                if (override != null) {
                    override.shownItem(picked);
                }
            }

            this.sendView(viewer, view, itemDisplay, own.getOrDefault(itemDisplay, itemDisplay.transform()), look, item, spawn, here);
        }
    }

    /**
     * Send one display to a viewer rendered on their own: everything when it spawns for them,
     * else only what differs from what they were last sent.
     *
     * @param viewer the viewer
     * @param view the viewer's view, which keeps what they were sent
     * @param display the display
     * @param transform where the display is for them
     * @param look how it looks for them
     * @param item the item they see, or null when it did not have to be built again
     * @param spawn true to spawn it for them
     * @param here where the model is
     */
    private void sendView(
            final Viewer viewer,
            final HyperionPlayerView view,
            final PartDisplay display,
            final @Nullable DisplayTransform transform,
            final DisplayLook look,
            final @Nullable ItemStack item,
            final boolean spawn,
            final Location here
    ) {
        if (transform == null) {
            return;
        }

        List<EntityData<?>> data = new ArrayList<>();
        boolean fresh = spawn || this.added.contains(display);
        ItemStack shown = item != null ? item : view.sentItems().getOrDefault(display, ItemStack.EMPTY);
        if (fresh) {
            Packets.send(viewer.user(), Packets.spawn(display.entityId(), display.uniqueId(), here));
            DisplayData.spawn(data, transform, look, shown, this.interpolation, this.viewers.viewDistance());
        } else {
            DisplayTransform sentTransform = view.sentTransforms().get(display);
            if (sentTransform == null || !transform.closeTo(sentTransform, EPSILON)) {
                DisplayData.transform(data, transform, this.interpolation);
            }

            DisplayLook sentLook = view.sentLooks().get(display);
            if (sentLook == null || look.styleDiffers(sentLook)) {
                DisplayData.look(data, look);
            }

            if (item != null && !Objects.equals(item, view.sentItems().get(display))) {
                DisplayData.item(data, item);
            }
        }

        view.sentTransforms().put(display, transform);
        view.sentLooks().put(display, look);
        view.sentItems().put(display, shown);
        if (!data.isEmpty()) {
            Packets.send(viewer.user(), Packets.metadata(display.entityId(), data));
        }
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
     * Work out what the playing animations do to every bone.
     *
     * @param into the poses, one per bone
     * @param extra a viewer's own animator, added on top of the shared one, or null
     */
    private void computePoses(final BonePose[] into, final @Nullable ModelAnimator extra) {
        for (BonePose pose : into) {
            pose.reset();
        }

        this.animator.apply(this.boneIndex, into);
        if (extra != null) {
            extra.apply(this.boneIndex, into);
        }
    }

    /**
     * Work out the matrix of every bone and the transform of every display. The shared pass also
     * runs the bones' modifiers and publishes where each bone is.
     *
     * @param poses what the animations do to each bone
     * @param view the viewer this pass is for, or null for the shared pass
     * @param matrices where the bones' matrices go, one per bone
     * @param sink where each display's transform goes
     */
    private void computeTransforms(
            final BonePose[] poses,
            final @Nullable HyperionPlayerView view,
            final Matrix4f[] matrices,
            final BiConsumer<PartDisplay, DisplayTransform> sink
    ) {
        boolean shared = view == null;
        if (shared) {
            this.moving = true;
            this.moveDirty = false;
            this.lastRotation = this.rotation;
            this.lastScale = this.scale;
            this.lastLift = this.lift();
        }

        ModelSpace.root(this.root, this.rotation, this.scale, this.transform);
        if (shared) {
            this.rootWorld = new Matrix4f(this.root);
        }

        List<HyperionBone> all = this.bones;
        for (int i = 0; i < all.size(); i++) {
            HyperionBone bone = all.get(i);
            int parent = this.parentIndex[i];
            Matrix4fc parentMatrix = parent < 0 ? IDENTITY : matrices[parent];
            BlueprintBone shape = bone.blueprintBone();
            BoneTransform extra = view == null ? bone.frameTransform() : viewTransform(bone, view);
            if (view == null) {
                bone.lastTransform(extra);
            }

            ModelSpace.bone(matrices[i], parentMatrix, shape.pivot(), shape.angles(), poses[i], extra, this.turn);
            if (view == null) {
                bone.world(new Matrix4f(this.root).mul(matrices[i]));
            }

            for (PartDisplay display : bone.displays()) {
                ModelPart part = Objects.requireNonNull(display.part());
                ModelSpace.part(this.scratch, this.root, matrices[i], shape.pivot(), part);
                sink.accept(display, DisplayTransform.of(this.scratch.translateLocal(0f, -this.lastLift, 0f)));
            }

            PartDisplay item = bone.itemDisplay();
            if (item != null) {
                ModelSpace.item(this.scratch, this.root, matrices[i], shape.pivot());
                sink.accept(item, DisplayTransform.of(this.scratch.translateLocal(0f, -this.lastLift, 0f)));
            }
        }
    }

    /**
     * Return a bone's transform for one viewer: theirs when set, else this frame's shared one.
     *
     * @param bone the bone
     * @param view the viewer's view
     * @return the transform
     */
    private static BoneTransform viewTransform(final HyperionBone bone, final HyperionPlayerView view) {
        HyperionViewBone override = view.existing(bone.name());
        BoneTransform own = override == null ? null : override.transformOverride();

        return own != null ? own : bone.lastTransform();
    }

    /**
     * Work out how every display looks, and build the items whose look changed.
     */
    private void computeLooks() {
        this.lookChangedThisFrame = true;
        PartItems items = this.manager.items();
        for (HyperionBone bone : this.bones) {
            DisplayLook look = this.lookOf(bone, null);
            PartDisplay item = bone.itemDisplay();
            DisplayLook partLook = bone.itemSource() == null ? look : hidden(look);
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
     * Combine a bone's look with the model's, and a viewer's overrides over both: a bone shows
     * when both do, glows or shimmers when either does, and its own tint, glow color, billboard
     * and light win over the model's.
     *
     * @param bone the bone
     * @param view the viewer's view, or null for the shared look
     * @return the look
     */
    private DisplayLook lookOf(final HyperionBone bone, final @Nullable HyperionPlayerView view) {
        LookState shared = bone.look();
        ViewLook model = view == null ? null : view.look();
        HyperionViewBone override = view == null ? null : view.existing(bone.name());
        ViewLook own = override == null ? null : override.look();

        boolean modelVisible = pick(model == null ? null : model.visible(), this.look.visible());
        boolean boneVisible = pick(own == null ? null : own.visible(), shared.visible());
        boolean modelGlint = pick(model == null ? null : model.glint(), this.look.glint());
        boolean boneGlint = pick(own == null ? null : own.glint(), shared.glint());
        boolean modelGlowing = pick(model == null ? null : model.glowing(), this.look.glowing());
        boolean boneGlowing = pick(own == null ? null : own.glowing(), shared.glowing());
        Color tint = first(own == null ? null : own.tint(), shared.tint(), model == null ? null : model.tint(), this.look.tint());
        Color glowColor = first(
                own == null ? null : own.glowColor(),
                shared.glowColor(),
                model == null ? null : model.glowColor(),
                this.look.glowColor()
        );
        Display.Billboard billboard = first(
                own == null ? null : own.billboard(),
                shared.billboard(),
                model == null ? null : model.billboard(),
                this.look.billboard()
        );
        Display.Brightness brightness = first(
                own == null ? null : own.brightness(),
                shared.brightness(),
                model == null ? null : model.brightness(),
                this.look.brightness()
        );

        return new DisplayLook(
                modelVisible && boneVisible,
                tint,
                modelGlint || boneGlint,
                modelGlowing || boneGlowing,
                glowColor,
                billboard == null ? Display.Billboard.FIXED : billboard,
                brightness
        );
    }

    /**
     * Return an override when set, else the shared value.
     *
     * @param override the override, or null when unset
     * @param shared the shared value
     * @return the value
     */
    private static boolean pick(final @Nullable Boolean override, final boolean shared) {
        return override != null ? override : shared;
    }

    /**
     * Return the first value that is set.
     *
     * @param first the strongest value, or null
     * @param second the next, or null
     * @param third the next, or null
     * @param fourth the weakest, or null
     * @param <T> the value's type
     * @return the first set value, or null when none is
     */
    private static <T> @Nullable T first(
            final @Nullable T first,
            final @Nullable T second,
            final @Nullable T third,
            final @Nullable T fourth
    ) {
        if (first != null) {
            return first;
        }

        if (second != null) {
            return second;
        }

        return third != null ? third : fourth;
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
     * Build the packets every viewer who already sees the model needs, whatever their view:
     * displays that went, and moves of a model on its own.
     *
     * @param here where the model is
     * @return the packets, in order
     */
    private List<PacketWrapper<?>> structure(final Location here) {
        List<PacketWrapper<?>> packets = new ArrayList<>();
        if (!this.dropped.isEmpty()) {
            int[] ids = new int[this.dropped.size()];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = this.dropped.get(i).entityId();
            }

            packets.add(Packets.destroy(ids));
        }

        if (this.teleported && this.entity == null) {
            for (PartDisplay display : this.displays) {
                if (!this.added.contains(display)) {
                    packets.add(Packets.teleport(display.entityId(), here));
                }
            }
        }

        return packets;
    }

    /**
     * Build the shared changes of the displays every viewer already has.
     *
     * @return the packets
     */
    private List<PacketWrapper<?>> changes() {
        List<PacketWrapper<?>> packets = new ArrayList<>();
        for (PartDisplay display : this.displays) {
            if (this.added.contains(display)) {
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
     * Build the whole shared state of a display a viewer already has, for a viewer leaving
     * their own render.
     *
     * @param display the display
     * @param item the item this viewer sees on it
     * @return the data
     */
    private List<EntityData<?>> fullData(final PartDisplay display, final ItemStack item) {
        List<EntityData<?>> data = new ArrayList<>();
        DisplayData.transform(data, Objects.requireNonNull(display.transform()), this.interpolation);
        DisplayData.look(data, Objects.requireNonNull(display.look()));
        DisplayData.item(data, item);

        return data;
    }

    /**
     * Send a viewer every display, and on an entity, mount them and hide the entity's parts.
     *
     * @param viewer the viewer
     * @param here where the model is
     */
    private void spawnFor(final Viewer viewer, final Location here) {
        if (viewer.bedrock()) {
            this.spawnBedrock(viewer, here);
            return;
        }

        HyperionPlayerView view = this.views.get(viewer.player().getUniqueId());
        if (view != null && view.active()) {
            view.rendered(false);
            this.renderView(viewer, view, here, true);
        } else {
            if (view != null) {
                view.rendered(false);
            }

            for (PartDisplay display : this.displays) {
                Packets.send(viewer.user(), Packets.spawn(display.entityId(), display.uniqueId(), here));
                ItemStack item = this.itemFor(display, viewer);
                Packets.send(viewer.user(), Packets.metadata(display.entityId(), this.spawnData(display, item)));
            }
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

    // Bedrock

    /**
     * Show the model to a Bedrock player. A model on an entity needs nothing sent: the entity's
     * own spawn packet carries the model in its UUID. A model on its own gets a stand-in entity
     * that only Bedrock players are sent, which Geyser turns into the model.
     *
     * @param viewer the Bedrock player
     * @param here where the model is
     */
    private void spawnBedrock(final Viewer viewer, final Location here) {
        // Without the extension Geyser would show the stand-in as a plain armor stand.
        BedrockLink link = this.manager.link();
        if (link == null || !link.connected()) {
            return;
        }

        if (this.entity == null) {
            UUID uuid = BedrockIds.uuid(BedrockIds.id(this.blueprint.key()), this.standIn);
            Packets.send(viewer.user(), Packets.standIn(this.standIn, uuid, here));
        }

        link.publish(this.bedrockState().event(this.bedrockEntity(), List.of(viewer.player().getUniqueId())));
    }

    /**
     * Stop showing the model to some Bedrock players: remove the stand-in, and tell the
     * extension to forget them.
     *
     * @param viewers the Bedrock players
     */
    private void dropBedrock(final List<Viewer> viewers) {
        if (viewers.isEmpty()) {
            return;
        }

        List<UUID> players = new ArrayList<>(viewers.size());
        for (Viewer viewer : viewers) {
            players.add(viewer.player().getUniqueId());
            if (this.entity == null) {
                Packets.send(viewer.user(), Packets.destroy(new int[] {this.standIn}));
            }
        }

        BedrockLink link = this.manager.link();
        if (link != null) {
            link.publish(BedrockState.gone(this.bedrockEntity(), players));
        }
    }

    /**
     * Tell the extension what the model's Bedrock viewers should see, when it changed.
     */
    private void publishBedrock() {
        BedrockLink link = this.manager.link();
        if (link == null) {
            return;
        }

        List<UUID> players = new ArrayList<>();
        for (Viewer viewer : this.viewers.viewers()) {
            if (viewer.bedrock()) {
                players.add(viewer.player().getUniqueId());
            }
        }

        if (players.isEmpty()) {
            return;
        }

        BedrockState state = this.bedrockState();
        if (state.equals(this.bedrockState)) {
            return;
        }

        this.bedrockState = state;
        link.publish(state.event(this.bedrockEntity(), players));
    }

    /**
     * Work out what Bedrock players should see: the animations that play, numbered as the
     * Bedrock pack numbers them, the speed of the first, and the scale.
     *
     * @return the state
     */
    private BedrockState bedrockState() {
        List<String> names = new ArrayList<>(this.blueprint.animations());
        List<Integer> playing = new ArrayList<>();
        float speed = 1f;
        for (AnimationHandle handle : this.animator.playing()) {
            int index = names.indexOf(handle.name());
            if (index >= 0 && index < BedrockPackBuilder.MAX_ANIMATIONS) {
                if (playing.isEmpty()) {
                    speed = handle.speed();
                }

                playing.add(index);
            }
        }

        playing.sort(Integer::compare);

        return new BedrockState(playing, speed, this.scale);
    }

    /**
     * Return the Java id of the entity Bedrock players see as the model.
     *
     * @return the carrying entity's id, or the stand-in's for a model on its own
     */
    private int bedrockEntity() {
        return this.entity != null ? this.entity.getEntityId() : this.standIn;
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
        if (bone == null || source == null || !source.perViewer() || display.part() != null) {
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
     * Give a bone an item display while it, or any player's view of it, has an item source,
     * and take it away once none has.
     */
    private void syncItemDisplays() {
        for (HyperionBone bone : this.bones) {
            boolean wanted = this.wantsItem(bone);
            PartDisplay display = bone.itemDisplay();
            if (wanted && display == null) {
                PartDisplay created = new PartDisplay(null);
                bone.itemDisplay(created);
                bone.markItemDirty();
                this.displays.add(created);
                this.added.add(created);
                this.lookDirty = true;
            } else if (!wanted && display != null) {
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
     * Return whether a bone needs an item display: it has an item source, or a player's view of
     * it does.
     *
     * @param bone the bone
     * @return true when it needs one
     */
    private boolean wantsItem(final HyperionBone bone) {
        if (bone.itemSource() != null) {
            return true;
        }

        for (HyperionPlayerView view : this.views.values()) {
            HyperionViewBone override = view.existing(bone.name());
            if (override != null && override.itemSourceOverride() != null) {
                return true;
            }
        }

        return false;
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
        this.index(this.bones);
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
     * Size the per-bone arrays to the bones and note where each bone and its parent are.
     *
     * @param all the bones, parents first
     */
    private void index(final List<HyperionBone> all) {
        int count = all.size();
        Map<String, Integer> indexes = new HashMap<>();
        this.poses = new BonePose[count];
        this.matrices = new Matrix4f[count];
        this.viewMatrices = new Matrix4f[count];
        this.parentIndex = new int[count];
        for (int i = 0; i < count; i++) {
            indexes.put(all.get(i).name(), i);
            this.poses[i] = new BonePose();
            this.matrices[i] = new Matrix4f();
            this.viewMatrices[i] = new Matrix4f();
        }

        for (int i = 0; i < count; i++) {
            HyperionBone parent = all.get(i).parentBone();
            Integer at = parent == null ? null : indexes.get(parent.name());
            this.parentIndex[i] = at == null ? -1 : at;
        }

        this.boneIndex = Map.copyOf(indexes);
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
            if (current == null || hidesCubes(bone)) {
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
     * Return whether a bone's own cubes are never drawn: a ghost, or a hitbox, whose cubes only
     * give its size.
     *
     * @param bone the bone
     * @return true when the bone gets no displays for its cubes
     */
    private static boolean hidesCubes(final HyperionBone bone) {
        for (BoneBehavior behavior : bone.behaviors()) {
            if (behavior instanceof GhostBehavior || behavior instanceof HitboxBehavior) {
                return true;
            }
        }

        return false;
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
            this.syncBox();
            this.animator.rebind();
            for (HyperionPlayerView view : this.views.values()) {
                view.modelAnimator().rebind();
                view.rendered(false);
            }
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
        boolean entityStays = reason == RemovalReason.ENTITY_REMOVED || reason == RemovalReason.ENTITY_UNLOADED;
        boolean carrierGoes = this.entity != null && !entityStays && this.manager.isCarrier(this.entity);
        List<Viewer> bedrock = new ArrayList<>();
        for (Viewer viewer : this.viewers.snapshot()) {
            this.viewers.remove(viewer.player().getUniqueId());
            if (viewer.bedrock()) {
                bedrock.add(viewer);
                continue;
            }

            Packets.send(viewer.user(), Packets.destroy(this.displayIds));
            if (this.entity != null && !carrierGoes) {
                this.restoreEntity(viewer, this.entity);
            }
        }

        this.dropBedrock(bedrock);
        this.removeBox();

        for (HyperionBone bone : this.bones) {
            for (BoneBehavior behavior : bone.behaviors()) {
                this.runBehavior(bone, behavior, each -> each.onRemove(reason));
            }
        }

        for (ModelTask task : List.copyOf(this.tasks)) {
            task.cancel();
        }

        this.tasks.clear();
        this.animator.clear();
        this.views.clear();
        this.manager.forget(this);
        if (this.entity != null && carrierGoes) {
            this.entity.remove();
        }

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
        List<Viewer> bedrock = new ArrayList<>();
        for (Viewer viewer : this.viewers.snapshot()) {
            if (viewer.bedrock()) {
                bedrock.add(viewer);
            } else {
                Packets.send(viewer.user(), Packets.destroy(this.displayIds));
            }
        }

        this.dropBedrock(bedrock);
        this.removeBox();
        this.tasks.clear();
        this.manager.forget(this);
    }

    /**
     * Take the box around the model down, when it has one.
     */
    private void removeBox() {
        ModelBox current = this.box;
        if (current != null) {
            current.remove();
            this.box = null;
        }
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
            this.views.remove(player);
        } else {
            this.viewers.remove(player);
        }

        for (PartDisplay display : List.copyOf(this.displays)) {
            display.viewerItems().remove(player);
        }
    }

    /**
     * Make an animator for this model's baked animations, following reloads.
     *
     * @param viewer the player it plays for, or null for everyone
     * @return the animator
     */
    ModelAnimator newAnimator(final @Nullable Player viewer) {
        ModelAnimator made = new ModelAnimator(this, name -> this.blueprint.baked(name), viewer, this.manager.plugin().getLogger());
        made.effects((animation, effect, player) -> this.manager.effects().passed(this, animation, effect, player));

        return made;
    }

    /**
     * Find a bone by name.
     *
     * @param name the bone's name
     * @return the bone, or null when there is none by that name
     */
    @Nullable HyperionBone boneNamed(final String name) {
        return this.bonesByName.get(name);
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
        return this.views.computeIfAbsent(player.getUniqueId(), id -> new HyperionPlayerView(this, player));
    }

    @Override
    public Animator animator() {
        return this.animator;
    }

    @Override
    public boolean autoAnimations() {
        return this.autoAnimations;
    }

    @Override
    public Model autoAnimations(final boolean enabled) {
        if (this.entity == null) {
            return this;
        }

        this.autoAnimations = enabled;
        AnimationHandle current = this.autoPlaying;
        if (!enabled && current != null) {
            current.stop();
            this.autoPlaying = null;
        }

        return this;
    }

    @Override
    public <B extends BoneBehavior> List<B> behaviors(final Class<B> type) {
        List<B> found = new ArrayList<>();
        for (HyperionBone bone : this.bones) {
            for (BoneBehavior behavior : bone.behaviors()) {
                if (type.isInstance(behavior)) {
                    found.add(type.cast(behavior));
                }
            }
        }

        return List.copyOf(found);
    }

    @Override
    public List<SeatBehavior> seats() {
        return this.behaviors(SeatBehavior.class);
    }

    @Override
    public List<HitboxBehavior> hitboxes() {
        return this.behaviors(HitboxBehavior.class);
    }

    @Override
    public HitboxMode hitboxMode() {
        return this.hitboxMode;
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
