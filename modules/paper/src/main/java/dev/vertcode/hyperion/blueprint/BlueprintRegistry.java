package dev.vertcode.hyperion.blueprint;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.source.FileSource;
import dev.vertcode.hyperion.api.model.source.ModelFormat;
import dev.vertcode.hyperion.api.model.source.ModelSource;
import dev.vertcode.hyperion.blueprint.bbmodel.BbmodelReader;
import dev.vertcode.hyperion.blueprint.data.ModelData;

/**
 * Every loaded blueprint, by key, and the loading that fills it.
 *
 * <p>Loading always runs on the executor given at construction, which is the async scheduler on
 * a server, so reading files and parsing JSON never touches a tick thread. Lookups are safe
 * from any thread. A reload keeps the old blueprint in place until the new one has loaded, so a
 * model in use never sees a half-loaded file, and a failed reload keeps the old one.
 *
 * <p>Models come from two places: files in Hyperion's {@code models/} folder, keyed by their
 * path under the {@code hyperion} namespace, and models other plugins register. A plugin's
 * models are dropped with {@link #unregisterAll(Plugin)}.
 */
public final class BlueprintRegistry {

    private static final String EXTENSION = ".bbmodel";
    private static final String NO_FORMAT = "no format can read this file; Hyperion reads .bbmodel files";
    private static final String UNREADABLE = "the file could not be read: %s";
    private static final String TAKEN = "the key %s is already registered by %s";
    private static final String NOT_REGISTERED = "No model is registered under %s";
    private static final String WARNING = "%s: %s";
    private static final String SCAN_FAILED = "Could not read the models folder %s";

    private final Path folder;
    private final HyperionFormatRegistry formats;
    private final Executor executor;
    private final Logger logger;
    private final Map<Key, Blueprint> blueprints = new ConcurrentHashMap<>();
    private final Map<Key, Registration> registrations = new ConcurrentHashMap<>();
    private final Map<Key, LoadReport> reports = new ConcurrentHashMap<>();
    private final Map<Key, Integer> revisions = new ConcurrentHashMap<>();

    /**
     * Create an empty registry. Nothing loads until {@link #reloadAll()} or
     * {@link #register(Plugin, Key, ModelSource)} is called.
     *
     * @param folder Hyperion's {@code models/} folder, made when missing
     * @param formats the formats files are read with
     * @param executor where loading runs, never a tick thread on a server
     * @param logger where warnings and errors are logged
     */
    public BlueprintRegistry(
            final Path folder,
            final HyperionFormatRegistry formats,
            final Executor executor,
            final Logger logger
    ) {
        this.folder = folder;
        this.formats = formats;
        this.executor = executor;
        this.logger = logger;
    }

    /**
     * Find a blueprint by key.
     *
     * @param key the key
     * @return the blueprint, or empty when none is loaded under that key
     */
    public Optional<Blueprint> find(final Key key) {
        return Optional.ofNullable(this.blueprints.get(key));
    }

    /**
     * Return every loaded blueprint, sorted by key.
     *
     * @return the blueprints
     */
    public List<Blueprint> all() {
        List<Blueprint> all = new ArrayList<>(this.blueprints.values());
        all.sort((first, second) -> first.key().asString().compareTo(second.key().asString()));

        return Collections.unmodifiableList(all);
    }

    /**
     * Return the report of the last load of a key, whether it worked or not.
     *
     * @param key the key
     * @return the report, or empty when the key was never loaded
     */
    public Optional<LoadReport> report(final Key key) {
        return Optional.ofNullable(this.reports.get(key));
    }

    /**
     * Return the report of the last load of every key, sorted by key.
     *
     * @return the reports by key
     */
    public Map<Key, LoadReport> reports() {
        Map<Key, LoadReport> sorted = new LinkedHashMap<>();
        this.reports.entrySet().stream()
                .sorted((first, second) -> first.getKey().asString().compareTo(second.getKey().asString()))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));

        return Collections.unmodifiableMap(sorted);
    }

    /**
     * Load a model for a plugin and keep it under a key.
     *
     * @param owner the plugin the model belongs to
     * @param key the key to register it under
     * @param source where to read it from
     * @return the blueprint once loaded; fails with {@link BlueprintLoadException} when the file
     *     has errors
     */
    public CompletableFuture<Blueprint> register(final Plugin owner, final Key key, final ModelSource source) {
        this.registrations.put(key, new Registration(owner, source));

        return CompletableFuture.supplyAsync(() -> this.load(key, owner, source), this.executor);
    }

    /**
     * Load one key again from where it came from.
     *
     * @param key the key
     * @return the new blueprint; fails with {@link BlueprintLoadException} when the file has
     *     errors, which keeps the old blueprint, or with {@link IllegalArgumentException} when
     *     nothing is registered under the key
     */
    public CompletableFuture<Blueprint> reload(final Key key) {
        Registration registration = this.registrations.get(key);
        if (registration == null) {
            String message = String.format(NOT_REGISTERED, key.asString());
            return CompletableFuture.failedFuture(new IllegalArgumentException(message));
        }

        return CompletableFuture.supplyAsync(
                () -> this.load(key, registration.owner(), registration.source()),
                this.executor
        );
    }

    /**
     * Scan the {@code models/} folder again and load every model: new files are added, files
     * that are gone are dropped, and models from plugins are loaded again from their sources.
     *
     * @return what the reload did
     */
    public CompletableFuture<ReloadResult> reloadAll() {
        return CompletableFuture.supplyAsync(this::reloadAllNow, this.executor);
    }

    /**
     * Drop the blueprint under a key.
     *
     * @param key the key
     * @return true when something was registered under it
     */
    public boolean unregister(final Key key) {
        Registration removed = this.registrations.remove(key);
        this.blueprints.remove(key);
        this.reports.remove(key);

        return removed != null;
    }

    /**
     * Drop every blueprint a plugin registered, as when the plugin disables.
     *
     * @param owner the plugin
     */
    public void unregisterAll(final Plugin owner) {
        List<Key> keys = new ArrayList<>();
        for (Map.Entry<Key, Registration> entry : this.registrations.entrySet()) {
            if (owner.equals(entry.getValue().owner())) {
                keys.add(entry.getKey());
            }
        }

        for (Key key : keys) {
            this.unregister(key);
        }
    }

    /**
     * Do a full reload on the calling thread.
     *
     * @return what the reload did
     */
    private ReloadResult reloadAllNow() {
        long start = System.nanoTime();
        List<Blueprint> loaded = new ArrayList<>();
        List<LoadReport> failed = new ArrayList<>();
        Map<Key, BlueprintChanges> changes = new LinkedHashMap<>();
        Map<Key, ModelSource> files = this.scan(failed);

        List<Key> removed = new ArrayList<>();
        for (Map.Entry<Key, Registration> entry : this.registrations.entrySet()) {
            if (entry.getValue().owner() == null && !files.containsKey(entry.getKey())) {
                removed.add(entry.getKey());
            }
        }

        for (Key key : removed) {
            this.unregister(key);
        }

        for (Map.Entry<Key, ModelSource> file : files.entrySet()) {
            Registration existing = this.registrations.get(file.getKey());
            if (existing != null && existing.owner() != null) {
                LoadReport report = new LoadReport(file.getValue().name());
                report.error(String.format(TAKEN, file.getKey().asString(), existing.owner().getName()));
                failed.add(report);
                continue;
            }

            this.registrations.put(file.getKey(), new Registration(null, file.getValue()));
        }

        for (Map.Entry<Key, Registration> entry : List.copyOf(this.registrations.entrySet())) {
            Blueprint before = this.blueprints.get(entry.getKey());
            try {
                Blueprint after = this.load(entry.getKey(), entry.getValue().owner(), entry.getValue().source());
                loaded.add(after);
                if (before != null) {
                    changes.put(after.key(), BlueprintChanges.between(before, after));
                }
            } catch (BlueprintLoadException exception) {
                failed.add(exception.report());
            }
        }

        long millis = (System.nanoTime() - start) / 1_000_000L;

        return new ReloadResult(loaded, failed, changes, removed, millis);
    }

    /**
     * Find every model file in the {@code models/} folder and work out its key.
     *
     * @param failed where the reports of files with unusable names go
     * @return the files by key, sorted by path
     */
    private Map<Key, ModelSource> scan(final List<LoadReport> failed) {
        Map<Key, ModelSource> files = new LinkedHashMap<>();
        List<Path> paths;
        try {
            Files.createDirectories(this.folder);
            try (Stream<Path> walk = Files.walk(this.folder)) {
                paths = walk.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(EXTENSION))
                        .sorted()
                        .toList();
            }
        } catch (IOException | UncheckedIOException exception) {
            this.logger.log(Level.SEVERE, String.format(SCAN_FAILED, this.folder), exception);
            return files;
        }

        for (Path path : paths) {
            String relative = this.folder.relativize(path).toString().replace('\\', '/');
            LoadReport report = new LoadReport(relative);
            Optional<Key> key = BlueprintKeys.fromPath(relative, report);
            if (key.isEmpty()) {
                failed.add(report);
                this.log(report);
                continue;
            }

            files.put(key.get(), new FileSource(path));
        }

        return files;
    }

    /**
     * Load one model on the calling thread and keep it when it has no errors.
     *
     * @param key the key to keep it under
     * @param owner the plugin it belongs to, or null for Hyperion's own folder
     * @param source where to read it from
     * @return the blueprint
     * @throws BlueprintLoadException when the file has errors; the old blueprint stays
     */
    private Blueprint load(final Key key, final @Nullable Plugin owner, final ModelSource source) {
        LoadReport report = new LoadReport(source.name());
        ModelData data = this.read(source, report);
        if (data == null || report.hasErrors()) {
            this.reports.put(key, report);
            this.log(report);
            throw new BlueprintLoadException(report);
        }

        int revision = this.revisions.merge(key, 1, Integer::sum);
        Blueprint blueprint = BlueprintFactory.build(key, owner, revision, source, data, report);
        this.blueprints.put(key, blueprint);
        this.reports.put(key, report);
        this.log(report);

        return blueprint;
    }

    /**
     * Read a model file into model data.
     *
     * @param source where to read it from
     * @param report where problems are noted
     * @return the model data, or null when the file could not be read at all
     */
    private @Nullable ModelData read(final ModelSource source, final LoadReport report) {
        Optional<ModelFormat> format = this.formats.find(source);
        if (format.isEmpty()) {
            report.error(NO_FORMAT);
            return null;
        }

        JsonObject json;
        try {
            json = format.get().read(source);
        } catch (IOException exception) {
            report.error(String.format(UNREADABLE, exception.getMessage()));
            return null;
        }

        return BbmodelReader.read(json, report);
    }

    /**
     * Log every issue of a report, warnings as warnings and errors as errors.
     *
     * @param report the report
     */
    private void log(final LoadReport report) {
        for (LoadIssue issue : report.issues()) {
            Level level = issue.severity() == IssueSeverity.ERROR ? Level.SEVERE : Level.WARNING;
            this.logger.log(level, String.format(WARNING, report.source(), issue.describe()));
        }
    }
}
