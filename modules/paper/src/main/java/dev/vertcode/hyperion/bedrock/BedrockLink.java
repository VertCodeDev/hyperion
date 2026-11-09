package dev.vertcode.hyperion.bedrock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jspecify.annotations.Nullable;

/**
 * The link the Geyser extension follows, wherever Geyser runs.
 *
 * <p>A small web server on its own port, every request checked against a shared token:
 * <ul>
 *     <li>{@code GET /manifest}: the models the extension registers with Geyser</li>
 *     <li>{@code GET /pack}: the Bedrock pack Geyser sends to Bedrock players</li>
 *     <li>{@code GET /stream}: one line of JSON per event, such as which animations play on a
 *     model a Bedrock player sees; open for as long as the extension is connected</li>
 *     <li>{@code POST /players}: the extension's list of Bedrock players who joined or left</li>
 * </ul>
 * Events are queued for each stream and written on its own thread, so publishing from a tick
 * thread never waits on the network.
 */
public final class BedrockLink {

    private static final int OK = 200;
    private static final int NO_CONTENT = 204;
    private static final int UNAUTHORIZED = 401;
    private static final int NOT_FOUND = 404;
    private static final int UNAVAILABLE = 503;
    private static final int NO_BODY = -1;
    private static final int STREAMING = 0;
    private static final int QUEUE = 4096;
    private static final long HEARTBEAT_SECONDS = 15L;
    private static final String THREAD_NAME = "Hyperion Bedrock link";

    private final HttpServer server;
    private final ExecutorService executor;
    private final Set<BlockingQueue<String>> streams = ConcurrentHashMap.newKeySet();
    private final byte[] token;
    private final Supplier<@Nullable BedrockPack> pack;
    private final BedrockPlayers players;

    private BedrockLink(
            final HttpServer server,
            final ExecutorService executor,
            final String token,
            final Supplier<@Nullable BedrockPack> pack,
            final BedrockPlayers players
    ) {
        this.server = server;
        this.executor = executor;
        this.token = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
        this.pack = pack;
        this.players = players;
    }

    /**
     * Start the link.
     *
     * @param port the port to listen on
     * @param token the token every request must carry
     * @param pack the current Bedrock pack, or null before the first build
     * @param players where the extension's player reports go
     * @return the running link
     * @throws IOException if the port cannot be opened
     */
    public static BedrockLink start(
            final int port,
            final String token,
            final Supplier<@Nullable BedrockPack> pack,
            final BedrockPlayers players
    ) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ExecutorService executor = Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        BedrockLink link = new BedrockLink(server, executor, token, pack, players);
        server.createContext("/", link::handle);
        server.start();

        return link;
    }

    /**
     * Send an event to every connected extension. Safe from any thread; never waits.
     *
     * @param event the event
     */
    public void publish(final JsonObject event) {
        String line = event.toString();
        for (BlockingQueue<String> stream : this.streams) {
            // A stream that fell this far behind is dead; dropping keeps the server's memory safe.
            stream.offer(line);
        }
    }

    /**
     * Return whether any extension is connected.
     *
     * @return true while at least one stream is open
     */
    public boolean connected() {
        return !this.streams.isEmpty();
    }

    /**
     * Stop the link and close every stream.
     */
    public void stop() {
        this.server.stop(0);
        this.executor.shutdownNow();
        this.streams.clear();
    }

    private void handle(final HttpExchange exchange) throws IOException {
        try (exchange) {
            String given = exchange.getRequestHeaders().getFirst("Authorization");
            if (given == null || !MessageDigest.isEqual(this.token, given.getBytes(StandardCharsets.UTF_8))) {
                exchange.sendResponseHeaders(UNAUTHORIZED, NO_BODY);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            switch (path) {
                case "/manifest" -> this.manifest(exchange);
                case "/pack" -> this.pack(exchange);
                case "/stream" -> this.stream(exchange);
                case "/players" -> this.players(exchange);
                default -> exchange.sendResponseHeaders(NOT_FOUND, NO_BODY);
            }
        }
    }

    private void manifest(final HttpExchange exchange) throws IOException {
        BedrockPack current = this.pack.get();
        if (current == null) {
            exchange.sendResponseHeaders(UNAVAILABLE, NO_BODY);
            return;
        }

        send(exchange, "application/json", current.manifest().getBytes(StandardCharsets.UTF_8));
    }

    private void pack(final HttpExchange exchange) throws IOException {
        BedrockPack current = this.pack.get();
        if (current == null) {
            exchange.sendResponseHeaders(UNAVAILABLE, NO_BODY);
            return;
        }

        send(exchange, "application/zip", current.zip());
    }

    /**
     * Keep a stream open and write its events as they come, with a heartbeat while quiet.
     *
     * @param exchange the request
     * @throws IOException when the extension goes away
     */
    private void stream(final HttpExchange exchange) throws IOException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(QUEUE);
        this.streams.add(queue);
        exchange.getResponseHeaders().set("Content-Type", "application/x-ndjson");
        exchange.sendResponseHeaders(OK, STREAMING);
        try (OutputStream body = exchange.getResponseBody()) {
            while (!Thread.currentThread().isInterrupted()) {
                String line = queue.poll(HEARTBEAT_SECONDS, TimeUnit.SECONDS);
                body.write(((line == null ? "" : line) + "\n").getBytes(StandardCharsets.UTF_8));
                body.flush();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            this.streams.remove(queue);
        }
    }

    private void players(final HttpExchange exchange) throws IOException {
        JsonElement body;
        try (InputStream stream = exchange.getRequestBody()) {
            body = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }

        if (body instanceof JsonObject report) {
            this.players.report(uuids(report.get("join")), uuids(report.get("leave")));
        }

        exchange.sendResponseHeaders(NO_CONTENT, NO_BODY);
    }

    private static List<UUID> uuids(final @Nullable JsonElement element) {
        List<UUID> uuids = new ArrayList<>();
        if (element instanceof JsonArray array) {
            for (JsonElement entry : array) {
                try {
                    uuids.add(UUID.fromString(entry.getAsString()));
                } catch (IllegalArgumentException | UnsupportedOperationException | IllegalStateException exception) {
                    // A broken entry is skipped; the rest of the report still counts.
                }
            }
        }

        return uuids;
    }

    private static void send(final HttpExchange exchange, final String type, final byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(OK, body.length);
        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(body);
        }
    }
}
