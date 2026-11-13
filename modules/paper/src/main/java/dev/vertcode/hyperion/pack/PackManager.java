package dev.vertcode.hyperion.pack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.bedrock.BedrockPack;
import dev.vertcode.hyperion.bedrock.BedrockPackBuilder;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.config.PackHosting;
import dev.vertcode.hyperion.config.PackSettings;
import dev.vertcode.hyperion.pack.armor.ArmorBuild;

/**
 * Build, host and send the resource pack.
 *
 * <p>Builds run on the async scheduler and swap the current pack in when done, so players never
 * get half a pack. A rebuild asked for while one runs waits for it and then runs once, however
 * many were asked for, so a burst of registrations builds the pack twice at most. Sending to a player always happens on the region that owns that player.
 * {@link #configure(PackSettings)} follows {@code config.yml}: it starts, moves or stops the
 * built-in web server as the hosting settings change.
 */
public final class PackManager {

    private static final UUID PACK_ID = UUID.nameUUIDFromBytes(
            "hyperion:resource-pack".getBytes(StandardCharsets.UTF_8)
    );
    private static final String EXPORT_FILE = "hyperion-pack.zip";
    private static final String SERVER_FAILED = "The pack server could not start on port %d; is the port in use?";
    private static final String BAD_LINK = "The pack link '%s' is not a valid URL";
    private static final String BUILT = "Built the resource pack: %s, %d files in %d ms";
    private static final String SELF_HOSTED_LINK = "http://%s:%d/%s.zip";
    private static final String BUILD_FAILED = "Building the resource pack failed";
    private static final String NEVER_REACHED = """
            %s could not download the pack from %s, and no request from them reached the pack \
            server. Port %d is not reachable at that address: open it in the firewall or the \
            host's panel, or set pack.self-host.address to the public address of this machine, \
            which is needed behind a proxy such as Velocity.""";
    private static final String CUT_OFF = """
            %s could not download the pack from %s, although the pack server sent it. The \
            download was cut off, or something between them and this server changed the file.""";
    private static final String STALE = """
            %s could not download the pack from %s: the pack server answered %d, so the link was \
            for an older pack. Run /hyperion pack send to offer the current one.""";
    private static final String URL_FAILED = """
            %s could not download the pack from %s. Check that the link downloads the zip itself, \
            not a web page, and that the upload is the current pack: run /hyperion pack export \
            and upload it again after every change.""";
    private static final String NOT_RUNNING = """
            %s could not download the pack from %s: the pack server is not running. The startup \
            log says why, usually because port %d is in use.""";
    private static final int SERVED = 200;
    private static final char CLIENT_MARKER = '\0';
    private static final List<PlayerResourcePackStatusEvent.Status> FAILURES = List.of(
            PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD,
            PlayerResourcePackStatusEvent.Status.INVALID_URL,
            PlayerResourcePackStatusEvent.Status.FAILED_RELOAD,
            PlayerResourcePackStatusEvent.Status.DISCARDED
    );

    private final Plugin plugin;
    private final BlueprintRegistry registry;
    private final Executor async;
    private final MinecraftVersion version;
    private final PackFormat format;
    private final Map<UUID, String> hosts = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerResourcePackStatusEvent.Status> statuses = new ConcurrentHashMap<>();
    private volatile PackSettings settings;
    private volatile @Nullable ResourcePack pack;
    private volatile @Nullable BedrockPack bedrock;
    private volatile boolean bedrockEnabled;
    private volatile Predicate<BoneBlueprint> hiddenBones = bone -> false;
    private volatile Supplier<@Nullable ArmorBuild> armor = () -> null;
    private volatile Consumer<ResourcePack> builtListener = built -> {
    };
    private volatile @Nullable PackServer server;
    private @Nullable CompletableFuture<ResourcePack> running;
    private @Nullable CompletableFuture<ResourcePack> queued;

    /**
     * Create the manager. Nothing is built or served until {@link #configure(PackSettings)} and
     * {@link #rebuild()} are called.
     *
     * @param plugin the plugin, for scheduling and logging
     * @param registry the blueprints the pack is built from
     * @param async where builds and exports run, never a tick thread
     * @param settings the pack settings from {@code config.yml}
     * @param version the server's version
     * @param format the pack format the server reads
     */
    public PackManager(
            final Plugin plugin,
            final BlueprintRegistry registry,
            final Executor async,
            final PackSettings settings,
            final MinecraftVersion version,
            final PackFormat format
    ) {
        this.plugin = plugin;
        this.registry = registry;
        this.async = async;
        this.settings = settings;
        this.version = version;
        this.format = format;
    }

    /**
     * Use new pack settings: start the web server for self-hosting, move it to a new port, or
     * stop it when hosting changed away from self-host.
     *
     * @param next the new settings
     */
    public synchronized void configure(final PackSettings next) {
        PackSettings previous = this.settings;
        this.settings = next;
        boolean wanted = next.hosting() == PackHosting.SELF_HOST;
        boolean moved = previous.port() != next.port();
        if (this.server != null && (!wanted || moved)) {
            this.server.stop();
            this.server = null;
        }

        if (!wanted || this.server != null) {
            return;
        }

        try {
            this.server = PackServer.start(next.port(), () -> this.pack);
        } catch (IOException exception) {
            this.plugin.getLogger().log(Level.SEVERE, String.format(SERVER_FAILED, next.port()), exception);
        }
    }

    /**
     * Run something after every build, with the pack in use: the new one, or the previous one
     * when the build came out the same.
     *
     * @param listener what to run, on the build's thread
     */
    public void onBuilt(final Consumer<ResourcePack> listener) {
        this.builtListener = listener;
    }

    /**
     * Build the pack from every loaded blueprint and use it. When it differs from the last one
     * and {@code resend-on-change} is on, everyone online gets it. While a build runs, this
     * queues one more build after it, shared by every call until it starts.
     *
     * @return the pack in use after the build
     */
    public synchronized CompletableFuture<ResourcePack> rebuild() {
        if (this.running == null) {
            return this.startBuild();
        }

        if (this.queued == null) {
            this.queued = new CompletableFuture<>();
        }

        return this.queued;
    }

    /**
     * Start one build and note it as running; the queued one starts after it. Call holding the
     * lock.
     *
     * @return the build
     */
    private CompletableFuture<ResourcePack> startBuild() {
        CompletableFuture<ResourcePack> build = CompletableFuture
                .supplyAsync(this::build, this.async)
                .thenApply(this::use);
        this.running = build;
        build.whenComplete((built, error) -> {
            if (error != null) {
                this.plugin.getLogger().log(Level.SEVERE, BUILD_FAILED, error);
            }

            this.next();
        });

        return build;
    }

    /**
     * Finish a build: start the queued one, if any, and hand its result to whoever waits on it.
     */
    private synchronized void next() {
        this.running = null;
        CompletableFuture<ResourcePack> waiting = this.queued;
        if (waiting == null) {
            return;
        }

        this.queued = null;
        CompletableFuture<ResourcePack> build = this.startBuild();
        build.whenComplete((built, error) -> {
            if (error != null) {
                waiting.completeExceptionally(error);
            } else {
                waiting.complete(built);
            }
        });
    }

    /**
     * Use a freshly built pack: keep the previous one when nothing changed, send a changed one
     * to everyone when configured to, and tell the listener.
     *
     * @param built the new pack
     * @return the pack in use
     */
    private ResourcePack use(final ResourcePack built) {
        String report = String.format(BUILT, built.describeSize(), built.files(), built.millis());
        this.plugin.getLogger().info(report);
        ResourcePack previous = this.pack;
        if (previous != null && previous.sha1().equals(built.sha1())) {
            this.builtListener.accept(previous);
            return previous;
        }

        this.pack = built;
        if (previous != null && this.settings.resendOnChange()) {
            this.sendToEveryone();
        }

        this.builtListener.accept(built);

        return built;
    }

    /**
     * Return the current pack.
     *
     * @return the pack, or empty before the first build
     */
    public Optional<ResourcePack> current() {
        return Optional.ofNullable(this.pack);
    }

    /**
     * Return the pack settings in use.
     *
     * @return the settings
     */
    public PackSettings settings() {
        return this.settings;
    }

    /**
     * Return whether Hyperion sends the pack at all.
     *
     * @return false for {@code hosting: off}
     */
    public boolean sends() {
        return this.settings.hosting() != PackHosting.OFF;
    }

    /**
     * Offer the pack to a player. Call on the region that owns the player.
     *
     * @param player the player
     * @return false when there is nothing to send: no pack yet, hosting off, or no valid link
     */
    public boolean send(final Player player) {
        ResourcePack current = this.pack;
        Optional<String> link = this.link(player);
        if (current == null || link.isEmpty()) {
            return false;
        }

        URI uri;
        try {
            uri = URI.create(link.get());
        } catch (IllegalArgumentException exception) {
            this.plugin.getLogger().warning(String.format(BAD_LINK, link.get()));
            return false;
        }

        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(ResourcePackInfo.resourcePackInfo(PACK_ID, uri, current.sha1()))
                .required(this.settings.required())
                .prompt(HyperionMessages.PACK__PROMPT.component())
                .replace(false)
                .build();
        player.sendResourcePacks(request);

        return true;
    }

    /**
     * Offer the pack to some players, each on the region that owns them. Safe from any thread.
     *
     * @param players the players
     * @return how many players it was offered to, 0 when there is nothing to send
     */
    public int sendTo(final Collection<? extends Player> players) {
        if (this.pack == null || !this.sends()) {
            return 0;
        }

        for (Player player : players) {
            player.getScheduler().run(this.plugin, scheduled -> this.send(player), null);
        }

        return players.size();
    }

    /**
     * Offer the pack to everyone online. Safe from any thread: the player list is read on the
     * global region, which owns it.
     */
    public void sendToEveryone() {
        Bukkit.getGlobalRegionScheduler().execute(this.plugin, () -> this.sendTo(Bukkit.getOnlinePlayers()));
    }

    /**
     * Return the link a player downloads the pack from.
     *
     * @param player the player
     * @return the link, or empty when there is no pack yet or hosting is off
     */
    public Optional<String> link(final Player player) {
        ResourcePack current = this.pack;
        if (current == null) {
            return Optional.empty();
        }

        PackSettings now = this.settings;

        return switch (now.hosting()) {
            case SELF_HOST -> Optional.of(String.format(
                    SELF_HOSTED_LINK,
                    this.address(player, now),
                    now.port(),
                    current.sha1()
            ));
            case URL -> now.url().isEmpty() ? Optional.empty() : Optional.of(now.url());
            case OFF -> Optional.empty();
        };
    }

    /**
     * Write the current pack into the data folder, to upload or merge by hand.
     *
     * @return the written file
     */
    public CompletableFuture<Path> export() {
        ResourcePack current = this.pack;
        if (current == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("The pack is not built yet"));
        }

        return CompletableFuture.supplyAsync(() -> {
            Path file = this.plugin.getDataPath().resolve(EXPORT_FILE);
            try {
                Files.write(file, current.zip());
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }

            return file;
        }, this.async);
    }

    /**
     * Remember the address a player joined with, which is where they can reach the web server.
     *
     * @param player the player's id
     * @param hostname the address as the client sent it, such as {@code play.example.com:25565}
     */
    public void rememberHost(final UUID player, final String hostname) {
        // Modded clients append their own data after a NUL, such as "\0FML3\0"
        int marker = hostname.indexOf(CLIENT_MARKER);
        String address = marker >= 0 ? hostname.substring(0, marker) : hostname;
        int colon = address.lastIndexOf(':');
        String host = colon > 0 ? address.substring(0, colon) : address;
        if (!host.isBlank()) {
            this.hosts.put(player, host);
        }
    }

    /**
     * Note how a player is doing with the pack.
     *
     * @param player the player's id
     * @param pack the id of the pack the status is about
     * @param status the status
     * @return true when the player should be kicked: the pack is required and failed or was declined
     */
    public boolean record(final UUID player, final UUID pack, final PlayerResourcePackStatusEvent.Status status) {
        if (!PACK_ID.equals(pack)) {
            return false;
        }

        this.statuses.put(player, status);
        boolean refused = status == PlayerResourcePackStatusEvent.Status.DECLINED || FAILURES.contains(status);

        return refused && this.settings.required();
    }

    /**
     * Log why a player's download failed, so the console says what to fix: the port never reached,
     * a download cut off, a stale link, or a bad uploaded link. Other statuses log nothing.
     *
     * @param player the player
     * @param pack the id of the pack the status is about
     * @param status the status the client reported
     */
    public void explain(final Player player, final UUID pack, final PlayerResourcePackStatusEvent.Status status) {
        boolean failed = status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD
                || status == PlayerResourcePackStatusEvent.Status.INVALID_URL;
        if (!PACK_ID.equals(pack) || !failed) {
            return;
        }

        Optional<String> link = this.link(player);
        if (link.isEmpty()) {
            return;
        }

        String name = player.getName();
        if (this.settings.hosting() != PackHosting.SELF_HOST) {
            this.plugin.getLogger().warning(String.format(URL_FAILED, name, link.get()));
            return;
        }

        PackServer running = this.server;
        if (running == null) {
            this.plugin.getLogger().warning(String.format(NOT_RUNNING, name, link.get(), this.settings.port()));
            return;
        }

        InetSocketAddress address = player.getAddress();
        OptionalInt answered = address == null ? OptionalInt.empty() : running.answered(address.getAddress());
        String reason;
        if (answered.isEmpty()) {
            reason = String.format(NEVER_REACHED, name, link.get(), this.settings.port());
        } else if (answered.getAsInt() == SERVED) {
            reason = String.format(CUT_OFF, name, link.get());
        } else {
            reason = String.format(STALE, name, link.get(), answered.getAsInt());
        }

        this.plugin.getLogger().warning(reason);
    }

    /**
     * Forget a player who left.
     *
     * @param player the player's id
     */
    public void forget(final UUID player) {
        this.hosts.remove(player);
        this.statuses.remove(player);
    }

    /**
     * Count how the online players are doing with the pack.
     *
     * @return the counts
     */
    public PackStatus status() {
        int loaded = 0;
        int pending = 0;
        int declined = 0;
        int failed = 0;
        for (PlayerResourcePackStatusEvent.Status status : this.statuses.values()) {
            switch (status) {
                case SUCCESSFULLY_LOADED -> loaded++;
                case DECLINED -> declined++;
                case ACCEPTED, DOWNLOADED -> pending++;
                default -> failed++;
            }
        }

        return new PackStatus(loaded, pending, declined, failed);
    }

    /**
     * Stop the web server. Call on disable.
     */
    public synchronized void shutdown() {
        if (this.server != null) {
            this.server.stop();
            this.server = null;
        }
    }

    /**
     * Build the pack from every loaded blueprint with the settings in use.
     *
     * @return the pack
     */
    private ResourcePack build() {
        List<Blueprint> blueprints = this.registry.all();
        if (this.bedrockEnabled) {
            this.bedrock = BedrockPackBuilder.build(blueprints, this.hiddenBones);
        }

        return PackBuilder.build(blueprints, this.settings, this.version, this.format, this.armor.get());
    }

    /**
     * Put armor sets in the pack from the next build on.
     *
     * @param sets gives the sets and settings at build time, or null for none
     */
    public void armor(final Supplier<@Nullable ArmorBuild> sets) {
        this.armor = sets;
    }

    /**
     * Build the Bedrock pack too from the next build on, for the Geyser extension.
     *
     * @param enabled true to build it
     */
    public void bedrock(final boolean enabled) {
        this.bedrockEnabled = enabled;
    }

    /**
     * Leave some bones' cubes out of the Bedrock pack from the next build on: ghosts and
     * hitboxes, which are never drawn.
     *
     * @param hidden tells which bones to leave out
     */
    public void hiddenBones(final Predicate<BoneBlueprint> hidden) {
        this.hiddenBones = hidden;
    }

    /**
     * Return the Bedrock pack built with the current pack.
     *
     * @return the Bedrock pack, or null before the first build or with Bedrock off
     */
    public @Nullable BedrockPack bedrockPack() {
        return this.bedrock;
    }

    /**
     * Work out the address a player downloads from: the configured one, else the one they joined
     * with, else the server's own.
     *
     * @param player the player
     * @param now the settings in use
     * @return the address
     */
    private String address(final Player player, final PackSettings now) {
        if (!now.address().isEmpty()) {
            return now.address();
        }

        String joined = this.hosts.get(player.getUniqueId());
        if (joined != null) {
            return joined;
        }

        String own = Bukkit.getIp();

        return own.isEmpty() ? "localhost" : own;
    }
}
