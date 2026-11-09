package dev.vertcode.hyperion.effect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.animation.TimedEffect;
import dev.vertcode.hyperion.api.animation.AnimationHandle;
import dev.vertcode.hyperion.api.effect.Effect;
import dev.vertcode.hyperion.api.effect.EffectAnchor;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.api.effect.EffectHandle;
import dev.vertcode.hyperion.api.effect.EffectOptions;
import dev.vertcode.hyperion.api.effect.EffectPartType;
import dev.vertcode.hyperion.api.effect.EffectRegistry;
import dev.vertcode.hyperion.api.effect.EffectViewers;
import dev.vertcode.hyperion.api.event.AnimationKeyframeEvent;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.locator.Locator;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.blueprint.LoadReport;
import dev.vertcode.hyperion.blueprint.metadata.JsonMetadata;
import dev.vertcode.hyperion.config.EffectSettings;
import dev.vertcode.hyperion.effect.part.BuiltinParts;
import dev.vertcode.hyperion.effect.part.CameraLock;
import dev.vertcode.hyperion.effect.part.PartServices;
import dev.vertcode.hyperion.effect.part.ParticleData;
import dev.vertcode.hyperion.model.ModelEffects;
import dev.vertcode.hyperion.model.ModelManager;

/**
 * Every effect, the part types they use, and the plays that run now.
 *
 * <p>Effects come from folders (Hyperion's own {@code effects/} as {@code hyperion:<name>}, and
 * any a plugin registers), from code, and from models, whose {@code hyperion_effects} are read
 * the first time one is asked for. It also plays the effect keyframes models' animations pass.
 *
 * <p>Safe from any thread: the maps are concurrent and a play runs on its anchor's thread.
 */
public final class HyperionEffects implements EffectRegistry, ModelEffects {

    /**
     * The namespace of the effects in Hyperion's own folder.
     */
    public static final String NAMESPACE = "hyperion";

    /**
     * The field of a model that holds its own effects.
     */
    public static final String MODEL_FIELD = "hyperion_effects";

    private static final String TAKEN = "The effect key %s is already registered by %s";
    private static final String TYPE_TAKEN = "The part type %s is already registered by %s";
    private static final String NO_EFFECT = "No effect is named %s";
    private static final String DUPLICATE = "is already loaded from another file; this one is skipped";
    private static final String BAD_KEY = "has a name that cannot be a key: lower case letters, digits, _, -, . and /";
    private static final String FILE_PROBLEMS = "%s: %d problem(s) loading effects, see /hy validate";
    private static final String NOT_AN_EFFECT = "The model %s plays the particle '%s', which is neither an effect nor a particle";
    private static final String UNREADABLE_FOLDER = "Could not read the effects in %s";
    private static final String FOLDER = "effects";
    private static final String EXAMPLE = "example.yml";
    private static final int BURST_COUNT = 10;
    private static final double BURST_SPREAD = 0.1d;

    private final Plugin plugin;
    private final ModelManager models;
    private final BlueprintRegistry blueprints;
    private final Map<Key, Registered> effects = new ConcurrentHashMap<>();
    private final Map<Key, RegisteredPart> parts = new ConcurrentHashMap<>();
    private final List<Folder> folders = new CopyOnWriteArrayList<>();
    private final Map<String, LoadReport> reports = new ConcurrentHashMap<>();
    private final Map<Key, Embedded> embedded = new ConcurrentHashMap<>();
    private final Set<EffectShow> playing = ConcurrentHashMap.newKeySet();
    private final Set<String> warned = ConcurrentHashMap.newKeySet();
    private final CameraLock cameras;
    private volatile EffectSettings settings;

    /**
     * Create the registry with Hyperion's own part types.
     *
     * @param plugin Hyperion, for scheduling and logging
     * @param models the live models, which effects on a model run inside
     * @param blueprints the models, for the effects they carry
     * @param settings the {@code effects} section of {@code config.yml}
     */
    public HyperionEffects(
            final Plugin plugin,
            final ModelManager models,
            final BlueprintRegistry blueprints,
            final EffectSettings settings
    ) {
        this.plugin = plugin;
        this.models = models;
        this.blueprints = blueprints;
        this.settings = settings;
        this.cameras = new CameraLock(plugin);
        for (EffectPartType type : BuiltinParts.all(new PartServices(plugin, blueprints, models, this.cameras))) {
            this.registerPartType(plugin, type);
        }
    }

    /**
     * Return the players cameras hold.
     *
     * @return the lock
     */
    public CameraLock cameras() {
        return this.cameras;
    }

    // Loading

    /**
     * Use new settings from {@code config.yml}.
     *
     * @param settings the {@code effects} section
     */
    public void configure(final EffectSettings settings) {
        this.settings = settings;
    }

    /**
     * Load every registered folder again and forget the models' effects, which are read again
     * when asked for. Reads files, so call it off the tick threads.
     *
     * @return how many effects the folders hold now
     */
    public int reload() {
        this.embedded.clear();
        this.reports.clear();
        int count = 0;
        for (Folder folder : this.folders) {
            count += this.load(folder);
        }

        return count;
    }

    @Override
    public int registerFolder(final Plugin owner, final String namespace, final Path folder) {
        Folder added = new Folder(owner, namespace.toLowerCase(Locale.ROOT), folder);
        this.folders.add(added);

        return this.load(added);
    }

    /**
     * Load one folder, replacing what it held before.
     *
     * @param folder the folder
     * @return how many effects it holds
     */
    private int load(final Folder folder) {
        this.effects.values().removeIf(registered -> folder.path().equals(registered.folder()));
        if (!Files.isDirectory(folder.path())) {
            return 0;
        }

        List<Path> files;
        try (Stream<Path> listed = Files.list(folder.path())) {
            files = listed.filter(HyperionEffects::isYaml).sorted().toList();
        } catch (IOException exception) {
            this.logger().log(Level.WARNING, String.format(UNREADABLE_FOLDER, folder.path()), exception);
            return 0;
        }

        int count = 0;
        for (Path file : files) {
            count += this.loadFile(folder, file);
        }

        return count;
    }

    /**
     * Load one file of a folder: each top-level section is an effect.
     *
     * @param folder the folder
     * @param file the file
     * @return how many effects it held
     */
    private int loadFile(final Folder folder, final Path file) {
        String source = folder.path().getFileName() + "/" + file.getFileName();
        LoadReport report = new LoadReport(source);
        this.reports.put(source, report);
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file.toFile());
        } catch (IOException | InvalidConfigurationException exception) {
            report.error(String.valueOf(exception.getMessage()));
            this.logger().warning(String.format(FILE_PROBLEMS, source, report.issues().size()));
            return 0;
        }

        int count = 0;
        for (Map.Entry<String, Object> entry : EffectTrees.of(yaml).entrySet()) {
            Key key = key(folder.namespace(), entry.getKey(), report);
            if (key == null) {
                continue;
            }

            if (this.effects.containsKey(key)) {
                report.warn("effect " + key.asString(), DUPLICATE);
                continue;
            }

            Optional<Effect> effect = EffectLoader.effect(key, entry.getValue(), report, this.parts::containsKey, true);
            if (effect.isPresent()) {
                this.effects.put(key, new Registered(folder.owner(), effect.get(), this.compile(effect.get()), folder.path()));
                count++;
            }
        }

        if (!report.issues().isEmpty()) {
            this.logger().warning(String.format(FILE_PROBLEMS, source, report.issues().size()));
        }

        return count;
    }

    /**
     * Return whether a file is YAML.
     *
     * @param file the file
     * @return true for {@code .yml} and {@code .yaml}
     */
    private static boolean isYaml(final Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);

        return Files.isRegularFile(file) && (name.endsWith(".yml") || name.endsWith(".yaml"));
    }

    /**
     * Make Hyperion's own effects folder with the example in it, the first time. Writes files,
     * so call it off the tick threads.
     *
     * @return the folder
     */
    public Path prepareFolder() {
        Path folder = this.plugin.getDataPath().resolve(FOLDER);
        if (!Files.isDirectory(folder)) {
            try {
                this.plugin.saveResource(FOLDER + "/" + EXAMPLE, false);
            } catch (IllegalArgumentException exception) {
                this.logger().log(Level.WARNING, String.format(UNREADABLE_FOLDER, folder), exception);
            }
        }

        return folder;
    }

    /**
     * Make an effect's key from its section's name.
     *
     * @param namespace the folder's namespace
     * @param name the section's name
     * @param report where a bad name goes
     * @return the key, or null when the name cannot be one
     */
    private static @Nullable Key key(final String namespace, final String name, final LoadReport report) {
        try {
            return Key.key(namespace, name.toLowerCase(Locale.ROOT));
        } catch (InvalidKeyException exception) {
            report.error("effect " + name, BAD_KEY);
            return null;
        }
    }

    /**
     * Return the problems from the last load of every file and model, by source.
     *
     * @return a snapshot of the reports
     */
    public Map<String, LoadReport> reports() {
        return Map.copyOf(this.reports);
    }

    // Registry

    @Override
    public void register(final Plugin owner, final Effect effect) {
        Registered existing = this.effects.get(effect.key());
        if (existing != null && !owner.equals(existing.owner())) {
            throw new IllegalArgumentException(String.format(TAKEN, effect.key().asString(), existing.owner().getName()));
        }

        this.effects.put(effect.key(), new Registered(owner, effect, this.compile(effect), null));
    }

    @Override
    public void registerPartType(final Plugin owner, final EffectPartType type) {
        RegisteredPart existing = this.parts.putIfAbsent(type.key(), new RegisteredPart(owner, type));
        if (existing != null) {
            throw new IllegalArgumentException(String.format(TYPE_TAKEN, type.key().asString(), existing.owner().getName()));
        }
    }

    @Override
    public void unregisterAll(final Plugin owner) {
        this.effects.values().removeIf(registered -> owner.equals(registered.owner()));
        this.parts.values().removeIf(registered -> owner.equals(registered.owner()));
        this.folders.removeIf(folder -> owner.equals(folder.owner()));
    }

    @Override
    public Optional<Effect> find(final Key key) {
        return this.registered(key).map(Registered::effect);
    }

    /**
     * Find an effect with its compiled timeline, a model's own effects included.
     *
     * @param key the effect's key
     * @return the effect, or empty
     */
    private Optional<Registered> registered(final Key key) {
        Registered found = this.effects.get(key);
        if (found != null) {
            return Optional.of(found);
        }

        int slash = key.value().indexOf('/');
        if (slash < 0) {
            return Optional.empty();
        }

        Key model = Key.key(key.namespace(), key.value().substring(0, slash));

        return Optional.ofNullable(this.embedded(model).get(key.value().substring(slash + 1)));
    }

    /**
     * Return a model's own effects, read from its {@code hyperion_effects} the first time and
     * again after it reloads.
     *
     * @param model the model's key
     * @return its effects by name, empty when it has none
     */
    private Map<String, Registered> embedded(final Key model) {
        Blueprint blueprint = this.blueprints.find(model).orElse(null);
        if (blueprint == null) {
            return Map.of();
        }

        Embedded cached = this.embedded.get(model);
        if (cached != null && cached.blueprint() == blueprint) {
            return cached.effects();
        }

        Map<String, Registered> found = new HashMap<>();
        JsonObject section = blueprint.metadata() instanceof JsonMetadata json ? json.object(MODEL_FIELD).orElse(null) : null;
        if (section != null) {
            LoadReport report = new LoadReport("model " + model.asString());
            for (Map.Entry<String, Object> entry : EffectTrees.of(section).entrySet()) {
                Key key = key(model.namespace(), model.value() + "/" + entry.getKey(), report);
                Optional<Effect> effect = key == null
                        ? Optional.empty()
                        : EffectLoader.effect(key, entry.getValue(), report, this.parts::containsKey, false);
                effect.ifPresent(loaded -> found.put(
                        entry.getKey().toLowerCase(Locale.ROOT),
                        new Registered(this.plugin, loaded, this.compile(loaded), null)
                ));
            }

            this.reports.put(report.source(), report);
        }

        Map<String, Registered> effectsOfModel = Map.copyOf(found);
        this.embedded.put(model, new Embedded(blueprint, effectsOfModel));

        return effectsOfModel;
    }

    @Override
    public Collection<Effect> all() {
        List<Effect> all = new ArrayList<>(this.effects.size());
        this.effects.values().forEach(registered -> all.add(registered.effect()));

        return List.copyOf(all);
    }

    // Playing

    @Override
    public EffectHandle play(final Key key, final EffectAnchor anchor) {
        return this.play(key, anchor, options -> {
            // Play as made.
        });
    }

    @Override
    public EffectHandle play(final Key key, final EffectAnchor anchor, final Consumer<EffectOptions> options) {
        Registered found = this.registered(key)
                .orElseThrow(() -> new IllegalArgumentException(String.format(NO_EFFECT, key.asString())));

        return this.start(found.compiled(), anchor, options);
    }

    @Override
    public EffectHandle play(final Effect effect, final EffectAnchor anchor, final Consumer<EffectOptions> options) {
        Registered found = this.effects.get(effect.key());
        CompiledEffect compiled = found != null && found.effect().equals(effect) ? found.compiled() : this.compile(effect);

        return this.start(compiled, anchor, options);
    }

    /**
     * Start a play on its anchor's thread.
     *
     * @param compiled the effect
     * @param anchor what it is attached to
     * @param options sets the play's options
     * @return the play
     */
    private EffectHandle start(final CompiledEffect compiled, final EffectAnchor anchor, final Consumer<EffectOptions> options) {
        EffectPlay chosen = new EffectPlay(compiled.effect());
        options.accept(chosen);
        EffectShow show = new EffectShow(this, compiled, anchor, chosen);
        this.playing.add(show);
        Model model = EffectShow.modelOf(anchor);
        if (model != null) {
            if (!this.models.attach(model, show)) {
                show.abandon();
            }

            return show;
        }

        ScheduledTask task = switch (anchor) {
            case EffectAnchor.OnEntity(Entity entity) -> entity.getScheduler().runAtFixedRate(
                    this.plugin,
                    running -> this.run(show, running),
                    show::cancel,
                    1L,
                    1L
            );
            case EffectAnchor.AtLocation(Location at) -> Bukkit.getRegionScheduler().runAtFixedRate(
                    this.plugin,
                    at,
                    running -> this.run(show, running),
                    1L,
                    1L
            );
            default -> null;
        };

        if (task == null) {
            show.abandon();
        }

        return show;
    }

    /**
     * Run one tick of a play on its own task, ending the task with it.
     *
     * @param show the play
     * @param task its task
     */
    private void run(final EffectShow show, final ScheduledTask task) {
        if (!show.step()) {
            task.cancel();
        }
    }

    @Override
    public List<EffectHandle> playing() {
        return List.copyOf(this.playing);
    }

    /**
     * Stop every play, as Hyperion disables.
     */
    public void stopAll() {
        for (EffectShow show : this.playing) {
            show.stop();
        }

        this.playing.clear();
        this.cameras.releaseAll();
    }

    // Model keyframes

    @Override
    public void passed(final Model model, final AnimationHandle animation, final TimedEffect effect, final @Nullable Player viewer) {
        switch (effect.kind()) {
            case SOUND -> {
                Location at = model.location();
                for (Player player : audience(model, viewer)) {
                    player.playSound(at, effect.effect(), SoundCategory.NEUTRAL, 1f, 1f);
                }
            }
            case PARTICLE -> this.particle(model, effect, viewer);
            case EVENT -> Bukkit.getPluginManager().callEvent(new AnimationKeyframeEvent(model, animation, effect.effect(), viewer));
        }
    }

    /**
     * Play a particle keyframe: the effect it names at its locator, or else a burst of the
     * particle it names.
     *
     * @param model the model
     * @param effect the keyframe
     * @param viewer the player the animation plays for, or null for everyone
     */
    private void particle(final Model model, final TimedEffect effect, final @Nullable Player viewer) {
        String locatorName = effect.locator();
        Locator locator = locatorName == null ? null : model.locator(locatorName).orElse(null);
        Registered found = this.effectFor(model, effect.effect());
        if (found != null) {
            EffectAnchor anchor = locator == null ? EffectAnchor.on(model) : EffectAnchor.locator(model, locator.name());
            this.start(found.compiled(), anchor, options -> {
                if (viewer != null) {
                    options.viewers(EffectViewers.only(viewer.getUniqueId()));
                }
            });
            return;
        }

        Particle particle = ParticleData.particle(effect.effect());
        if (particle == null || !ParticleData.supported(particle)) {
            this.warnOnce(String.format(NOT_AN_EFFECT, model.blueprint().key().asString(), effect.effect()));
            return;
        }

        Location at = locator == null ? model.location() : locator.location();
        Object data = ParticleData.data(particle, EffectFrame.EMPTY);
        ParticleData.send(audience(model, viewer), particle, at, BURST_COUNT, BURST_SPREAD, 0d, data, null);
    }

    /**
     * Find the effect a Blockbench particle keyframe names: a full key, the model's own effect,
     * or one of Hyperion's.
     *
     * @param model the model
     * @param name the keyframe's effect
     * @return the effect, or null when the name is none
     */
    private @Nullable Registered effectFor(final Model model, final String name) {
        String lowered = name.toLowerCase(Locale.ROOT);
        try {
            if (lowered.contains(":")) {
                return this.registered(Key.key(lowered)).orElse(null);
            }

            Key own = model.blueprint().key();
            Optional<Registered> found = this.registered(Key.key(own.namespace(), own.value() + "/" + lowered));

            return found.or(() -> this.registered(Key.key(NAMESPACE, lowered))).orElse(null);
        } catch (InvalidKeyException exception) {
            return null;
        }
    }

    /**
     * Return who hears a model's keyframe: its viewers, or the one player a view plays for.
     *
     * @param model the model
     * @param viewer the player, or null for every viewer
     * @return the players
     */
    private static List<Player> audience(final Model model, final @Nullable Player viewer) {
        return viewer != null ? List.of(viewer) : List.copyOf(model.viewers().current());
    }

    // Internals for plays

    /**
     * Compile an effect against the part types registered now.
     *
     * @param effect the effect
     * @return the compiled effect
     */
    private CompiledEffect compile(final Effect effect) {
        return new CompiledEffect(effect, type -> {
            RegisteredPart registered = this.parts.get(type);
            return registered != null && registered.type().sampled();
        });
    }

    /**
     * Find a part type.
     *
     * @param key the type's key
     * @return the type, or null when none is registered
     */
    @Nullable EffectPartType partType(final Key key) {
        RegisteredPart registered = this.parts.get(key);

        return registered == null ? null : registered.type();
    }

    /**
     * Return the settings in use.
     *
     * @return the settings
     */
    EffectSettings settings() {
        return this.settings;
    }

    /**
     * Forget a play that ended.
     *
     * @param show the play
     */
    void forget(final EffectShow show) {
        this.playing.remove(show);
    }

    /**
     * Log a warning once per server run, for problems that would repeat every tick.
     *
     * @param message the warning
     */
    void warnOnce(final String message) {
        if (this.warned.add(message)) {
            this.logger().warning(message);
        }
    }

    Logger logger() {
        return this.plugin.getLogger();
    }

    /**
     * An effect, who added it, its compiled timeline and the folder it came from.
     *
     * @param owner the plugin it belongs to
     * @param effect the effect
     * @param compiled its compiled timeline
     * @param folder the folder it was loaded from, or null when it was not
     */
    private record Registered(Plugin owner, Effect effect, CompiledEffect compiled, @Nullable Path folder) {
    }

    /**
     * A part type and the plugin that added it.
     *
     * @param owner the plugin
     * @param type the part type
     */
    private record RegisteredPart(Plugin owner, EffectPartType type) {
    }

    /**
     * A folder of effect files.
     *
     * @param owner the plugin it belongs to
     * @param namespace the namespace of its effects
     * @param path the folder
     */
    private record Folder(Plugin owner, String namespace, Path path) {
    }

    /**
     * The effects read from one version of a model.
     *
     * @param blueprint the model they were read from
     * @param effects its effects by name
     */
    private record Embedded(Blueprint blueprint, Map<String, Registered> effects) {
    }
}
