package dev.vertcode.hyperion.pack;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jspecify.annotations.Nullable;

/**
 * The small web server that hands out the resource pack, for {@code hosting: self-host}.
 *
 * <p>It answers one path, {@code /<sha1>.zip} of the current pack, and nothing else. Requests
 * run on two daemon threads of its own, never on a tick thread. It remembers what it last answered
 * each address, so a failed download can be told apart from one that never reached it.
 */
final class PackServer {

    private static final int THREADS = 2;
    private static final int OK = 200;
    private static final int NOT_FOUND = 404;
    private static final int NO_BODY = -1;
    private static final String THREAD_NAME = "Hyperion pack server";
    private static final int MAX_REMEMBERED = 512;

    private final Map<InetAddress, Integer> answers = new ConcurrentHashMap<>();
    private final HttpServer server;
    private final ExecutorService executor;
    private final Supplier<@Nullable ResourcePack> pack;

    private PackServer(
            final HttpServer server,
            final ExecutorService executor,
            final Supplier<@Nullable ResourcePack> pack
    ) {
        this.server = server;
        this.executor = executor;
        this.pack = pack;
    }

    /**
     * Start serving the pack.
     *
     * @param port the port to listen on
     * @param pack hands out the current pack, or null before the first build
     * @return the running server
     * @throws IOException if the port cannot be opened, usually because it is in use
     */
    static PackServer start(final int port, final Supplier<@Nullable ResourcePack> pack) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS, task -> {
            Thread thread = new Thread(task, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        PackServer running = new PackServer(server, executor, pack);
        server.setExecutor(executor);
        server.createContext("/", running::answer);
        server.start();

        return running;
    }

    /**
     * Return what the server last answered an address, such as 200 for the pack or 404 for a
     * stale link.
     *
     * @param address the address the request came from
     * @return the HTTP status, or empty when nothing from that address reached the server
     */
    OptionalInt answered(final InetAddress address) {
        Integer status = this.answers.get(address);

        return status == null ? OptionalInt.empty() : OptionalInt.of(status);
    }

    /**
     * Stop serving and release the port.
     */
    void stop() {
        this.server.stop(0);
        this.executor.shutdownNow();
        this.answers.clear();
    }

    /**
     * Answer one request: the zip for the current pack's path, not found for anything else.
     *
     * @param exchange the request
     * @throws IOException if the connection fails
     */
    private void answer(final HttpExchange exchange) throws IOException {
        try (exchange) {
            ResourcePack current = this.pack.get();
            boolean get = exchange.getRequestMethod().equals("GET");
            String path = exchange.getRequestURI().getPath();
            boolean found = current != null && get && path.equals("/" + current.sha1() + ".zip");
            this.remember(exchange.getRemoteAddress().getAddress(), found ? OK : NOT_FOUND);
            if (!found) {
                exchange.sendResponseHeaders(NOT_FOUND, NO_BODY);
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(OK, current.size());
            try (OutputStream body = exchange.getResponseBody()) {
                current.writeZip(body);
            }
        }
    }

    /**
     * Note what an address was answered. Anyone can reach the port, so the map is emptied once it
     * holds more addresses than a server has players, rather than growing with every scanner.
     *
     * @param address the address the request came from
     * @param status the HTTP status sent back
     */
    private void remember(final InetAddress address, final int status) {
        if (this.answers.size() >= MAX_REMEMBERED) {
            this.answers.clear();
        }

        this.answers.put(address, status);
    }
}
