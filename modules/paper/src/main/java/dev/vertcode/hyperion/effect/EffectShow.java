package dev.vertcode.hyperion.effect;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.effect.Effect;
import dev.vertcode.hyperion.api.effect.EffectActor;
import dev.vertcode.hyperion.api.effect.EffectAnchor;
import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.api.effect.EffectHandle;
import dev.vertcode.hyperion.api.effect.EffectPart;
import dev.vertcode.hyperion.api.effect.EffectPartType;
import dev.vertcode.hyperion.api.effect.EffectViewers;
import dev.vertcode.hyperion.api.event.EffectEndEvent;
import dev.vertcode.hyperion.api.event.EffectStartEvent;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.api.model.locator.Locator;
import dev.vertcode.hyperion.model.ModelTask;

/**
 * One play of an effect: it follows its anchor, runs the keyframes each tick reaches, and feeds
 * the parts that act every tick.
 *
 * <p>Everything but {@link #stop()} and the getters runs on the thread that owns the anchor: in
 * the model's frame for an effect on a model, else on the entity's or the region's scheduler.
 */
final class EffectShow implements EffectHandle, EffectContext, ModelTask {

    private static final String PART_FAILED = "The part '%s' of the effect %s failed and was stopped";
    private static final String NO_PART_TYPE = "The effect %s uses part type %s, which is not registered";
    private static final String BONE = "bone:";
    private static final String LOCATOR = "locator:";
    private static final String ANCHOR = "anchor";

    private final HyperionEffects effects;
    private final CompiledEffect compiled;
    private final EffectAnchor anchor;
    private final EffectViewers viewers;
    private final Map<String, String> parameters;
    private final List<Runnable> endActions;
    private final @Nullable EffectActor[] actors;
    private final Map<String, @Nullable Placement> placements = new HashMap<>();
    private volatile boolean playing = true;
    private volatile boolean stopWanted;
    private volatile int shownTick;
    private int tick;
    private volatile Location location;
    private boolean started;
    private int cursor;
    private @Nullable List<Player> viewersNow;

    /**
     * Prepare a play. Nothing runs until its first tick.
     *
     * @param effects the registry, for part types, settings and logging
     * @param compiled the effect
     * @param anchor what it is attached to
     * @param options this play's options
     */
    EffectShow(
            final HyperionEffects effects,
            final CompiledEffect compiled,
            final EffectAnchor anchor,
            final EffectPlay options
    ) {
        this.effects = effects;
        this.compiled = compiled;
        this.anchor = anchor;
        this.viewers = options.viewers();
        this.parameters = options.parameters();
        this.endActions = options.endActions();
        this.actors = new EffectActor[compiled.effect().parts().size()];
        // Only a fixed spot is safe to read here; anything else is read on its own thread.
        this.location = anchor instanceof EffectAnchor.AtLocation(Location at) ? at.clone() : new Location(null, 0d, 0d, 0d);
    }

    // Ticks

    @Override
    public boolean step() {
        if (!this.playing) {
            return false;
        }

        if (!this.started) {
            this.start();
        }

        Placement here = place(this.anchor);
        if (this.stopWanted || here == null) {
            this.end(false);
            return false;
        }

        this.location = here.origin().clone();
        this.placements.clear();
        this.placements.put(ANCHOR, here);
        this.viewersNow = null;
        List<CompiledEffect.Step> steps = this.compiled.steps();
        while (this.cursor < steps.size() && steps.get(this.cursor).tick() <= this.tick) {
            CompiledEffect.Step step = steps.get(this.cursor++);
            this.act(step.part(), step.frame());
        }

        for (int part = 0; part < this.actors.length; part++) {
            if (this.compiled.sampled(part)) {
                EffectFrame frame = this.compiled.state(part, this.tick);
                if (frame != null) {
                    this.act(part, frame);
                }
            }
        }

        if (this.tick >= this.compiled.effect().duration()) {
            if (this.compiled.effect().loop()) {
                this.tick = 0;
                this.shownTick = 0;
                this.cursor = 0;
                return true;
            }

            this.end(true);
            return false;
        }

        this.tick++;
        this.shownTick = this.tick;

        return true;
    }

    /**
     * Make every part's actor and tell listeners the effect starts.
     */
    private void start() {
        this.started = true;
        List<EffectPart> parts = this.compiled.effect().parts();
        for (int i = 0; i < parts.size(); i++) {
            EffectPart part = parts.get(i);
            EffectPartType type = this.effects.partType(part.type());
            if (type == null) {
                this.effects.warnOnce(String.format(NO_PART_TYPE, this.key(), part.type().asString()));
                continue;
            }

            this.actors[i] = type.create(part, this);
        }

        Bukkit.getPluginManager().callEvent(new EffectStartEvent(this));
    }

    /**
     * Hand one part its values, with the parameters filled in. A part that throws is stopped and
     * logged, so the rest of the effect plays on.
     *
     * @param part the part's place in the effect
     * @param frame its values
     */
    private void act(final int part, final EffectFrame frame) {
        EffectActor actor = this.actors[part];
        if (actor == null) {
            return;
        }

        try {
            actor.frame(this.tick, this.filled(frame));
        } catch (RuntimeException exception) {
            this.actors[part] = null;
            String name = this.compiled.effect().parts().get(part).name();
            this.effects.logger().log(Level.WARNING, String.format(PART_FAILED, name, this.key()), exception);
        }
    }

    /**
     * End the play: stop the parts, tell listeners, run the end actions and leave the registry.
     *
     * @param finished true when it played to its end
     */
    private void end(final boolean finished) {
        if (!this.playing) {
            return;
        }

        this.playing = false;
        for (EffectActor actor : this.actors) {
            if (actor != null) {
                actor.stop();
            }
        }

        if (this.started) {
            Bukkit.getPluginManager().callEvent(new EffectEndEvent(this, finished));
        }

        for (Runnable action : this.endActions) {
            action.run();
        }

        this.effects.forget(this);
    }

    @Override
    public void cancel() {
        this.end(false);
    }

    /**
     * Drop a play that never got to run, as when its anchor was already gone.
     */
    void abandon() {
        this.playing = false;
        this.effects.forget(this);
    }

    // Placing

    /**
     * Work out where an anchor is and how it is turned.
     *
     * @param anchor the anchor
     * @return the placement, or null while the anchor is gone
     */
    private static @Nullable Placement place(final EffectAnchor anchor) {
        return switch (anchor) {
            case EffectAnchor.AtLocation(Location at) -> new Placement(at, yaw(at.getYaw()));
            case EffectAnchor.OnEntity(Entity entity) -> {
                if (!entity.isValid()) {
                    yield null;
                }

                float facing = entity instanceof LivingEntity living ? living.getBodyYaw() : entity.getYaw();
                yield new Placement(entity.getLocation(), yaw(facing));
            }
            case EffectAnchor.OnModel(Model model) -> model.isRemoved()
                    ? null
                    : new Placement(model.location(), yaw(model.rotation().yaw()));
            case EffectAnchor.OnBone(Model model, String name) -> {
                Bone bone = model.isRemoved() ? null : model.bone(name).orElse(null);
                yield bone == null ? null : new Placement(bone.location(), fromBone(bone.rotation()));
            }
            case EffectAnchor.OnLocator(Model model, String name) -> {
                Locator locator = model.isRemoved() ? null : model.locator(name).orElse(null);
                yield locator == null ? null : new Placement(locator.location(), fromBone(locator.rotation()));
            }
        };
    }

    /**
     * Return the model an anchor belongs to.
     *
     * @param anchor the anchor
     * @return the model, or null for an entity or a location
     */
    static @Nullable Model modelOf(final EffectAnchor anchor) {
        return switch (anchor) {
            case EffectAnchor.OnModel(Model model) -> model;
            case EffectAnchor.OnBone(Model model, String bone) -> model;
            case EffectAnchor.OnLocator(Model model, String locator) -> model;
            case EffectAnchor.AtLocation at -> null;
            case EffectAnchor.OnEntity on -> null;
        };
    }

    /**
     * Find where a part's {@code at} puts it: the anchor, a bone or locator of the anchor's
     * model, or another part.
     *
     * @param frame the part's values
     * @return the placement, or null while what it is at is gone
     */
    private @Nullable Placement placementOf(final EffectFrame frame) {
        String at = frame.string("at", ANCHOR);
        if (this.placements.containsKey(at)) {
            return this.placements.get(at);
        }

        Placement found = this.find(at);
        this.placements.put(at, found);

        return found;
    }

    /**
     * Look up an {@code at} the first time a tick asks for it.
     *
     * @param at the {@code at}
     * @return the placement, or null
     */
    private @Nullable Placement find(final String at) {
        Placement anchored = this.placements.get(ANCHOR);
        Model model = modelOf(this.anchor);
        if (model != null && at.startsWith(BONE)) {
            return place(EffectAnchor.bone(model, at.substring(BONE.length())));
        }

        if (model != null && at.startsWith(LOCATOR)) {
            return place(EffectAnchor.locator(model, at.substring(LOCATOR.length())));
        }

        List<EffectPart> parts = this.compiled.effect().parts();
        for (int i = 0; i < parts.size(); i++) {
            EffectActor actor = this.actors[i];
            Location there = actor == null ? null : actor.location();
            if (parts.get(i).name().equals(at) && there != null && anchored != null) {
                return new Placement(there, anchored.rotation());
            }
        }

        return anchored;
    }

    @Override
    public @Nullable Location location(final EffectFrame frame) {
        Placement placement = this.placementOf(frame);
        if (placement == null) {
            return null;
        }

        Vector3f offset = frame.vector("offset");
        if (offset == null) {
            return placement.origin().clone();
        }

        Vector3f turned = placement.turn(offset.x, offset.y, offset.z);

        return placement.origin().clone().add(turned.x, turned.y, turned.z);
    }

    @Override
    public Vector3f turn(final EffectFrame frame, final float right, final float up, final float forward) {
        Placement placement = this.placementOf(frame);

        return placement == null ? new Vector3f(-right, up, forward) : placement.turn(right, up, forward);
    }

    @Override
    public Quaternionf rotation(final EffectFrame frame) {
        Placement placement = this.placementOf(frame);

        return placement == null ? new Quaternionf() : new Quaternionf(placement.rotation());
    }

    // Audience

    @Override
    public List<Player> viewers() {
        List<Player> current = this.viewersNow;
        if (current == null) {
            current = this.collectViewers();
            this.viewersNow = current;
        }

        return current;
    }

    /**
     * Find who sees and hears the effect this tick.
     *
     * @return the players
     */
    private List<Player> collectViewers() {
        Location at = this.location;
        if (at.getWorld() == null) {
            return List.of();
        }

        return switch (this.viewers) {
            case EffectViewers.Only(UUID player) -> {
                Player online = Bukkit.getPlayer(player);
                yield online == null ? List.of() : List.of(online);
            }
            case EffectViewers.Everyone everyone -> List.copyOf(at.getWorld().getPlayers());
            case EffectViewers.Nearby(double radius) -> List.copyOf(at.getWorld().getNearbyPlayers(at, radius));
            case EffectViewers.Model model -> {
                Model shown = modelOf(this.anchor);
                yield shown != null
                        ? List.copyOf(shown.viewers().current())
                        : List.copyOf(at.getWorld().getNearbyPlayers(at, this.effects.settings().viewDistance()));
            }
        };
    }

    @Override
    public int particles(final int wanted) {
        float multiplier = this.effects.settings().particleMultiplier();
        if (wanted <= 0 || multiplier <= 0f) {
            return 0;
        }

        return Math.max(1, Math.round(wanted * multiplier));
    }

    @Override
    public String fill(final String text) {
        if (this.parameters.isEmpty() || text.indexOf('%') < 0) {
            return text;
        }

        String filled = text;
        for (Map.Entry<String, String> parameter : this.parameters.entrySet()) {
            filled = filled.replace("%" + parameter.getKey() + "%", parameter.getValue());
        }

        return filled;
    }

    /**
     * Fill the parameters into every text value of a frame.
     *
     * @param frame the frame
     * @return the frame with parameters filled in, the same frame when none appear
     */
    private EffectFrame filled(final EffectFrame frame) {
        Map<String, Object> values = null;
        for (Map.Entry<String, Object> entry : frame.values().entrySet()) {
            if (entry.getValue() instanceof String text && text.indexOf('%') >= 0) {
                if (values == null) {
                    values = new LinkedHashMap<>(frame.values());
                }

                values.put(entry.getKey(), this.fill(text));
            }
        }

        return values == null ? frame : new EffectFrame(values);
    }

    // Api

    @Override
    public EffectHandle handle() {
        return this;
    }

    @Override
    public Effect effect() {
        return this.compiled.effect();
    }

    @Override
    public EffectAnchor anchor() {
        return this.anchor;
    }

    @Override
    public Location location() {
        return this.location.clone();
    }

    @Override
    public int tick() {
        return this.shownTick;
    }

    @Override
    public boolean isPlaying() {
        return this.playing;
    }

    @Override
    public void stop() {
        this.stopWanted = true;
    }

    /**
     * Return the effect's key, for messages.
     *
     * @return the key as text
     */
    private String key() {
        return this.compiled.effect().key().asString();
    }

    /**
     * Turn a yaw into the rotation offsets on a model, an entity or a location turn with.
     *
     * @param degrees the yaw
     * @return the rotation
     */
    private static Quaternionf yaw(final float degrees) {
        return new Quaternionf().rotationY((float) Math.toRadians(-degrees));
    }

    /**
     * Turn a bone's or locator's rotation into the one offsets turn with. Bones face north as in
     * Blockbench, so a half turn first makes their forward the same as everything else's.
     *
     * @param rotation the bone's rotation in the world
     * @return the rotation
     */
    private static Quaternionf fromBone(final Quaternionfc rotation) {
        return new Quaternionf(rotation).rotateY((float) Math.PI);
    }

    /**
     * Where something is and how offsets from it turn.
     *
     * @param origin the spot
     * @param rotation the rotation from a space where X is left, Y up and Z forward into the world
     */
    private record Placement(Location origin, Quaternionfc rotation) {

        /**
         * Turn an offset of right, up and forward into the world.
         *
         * @param right blocks to the right
         * @param up blocks up
         * @param forward blocks forward
         * @return the offset in the world
         */
        Vector3f turn(final float right, final float up, final float forward) {
            return this.rotation.transform(new Vector3f(-right, up, forward));
        }
    }
}
