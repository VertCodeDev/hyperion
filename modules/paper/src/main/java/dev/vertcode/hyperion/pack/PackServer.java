package dev.vertcode.hyperion.pack;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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
 * run on two daemon threads of its own, never on a tick thread.
 */
final class PackServer {

    private static final int THREADS = 2;
    private static final int OK = 200;
    private static final int NOT_FOUND = 404;
    private static final int NO_BODY = -1;
    private static final String THREAD_NAME = "Hyperion pack server";

    private final HttpServer server;
    private final ExecutorService executor;

    private PackServer(final HttpServer server, final ExecutorService executor) {
        this.server = server;
        this.executor = executor;
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
        server.setExecutor(executor);
        server.createContext("/", exchange -> answer(exchange, pack.get()));
        server.start();

        return new PackServer(server, executor);
    }

    /**
     * Stop serving and release the port.
     */
    void stop() {
        this.server.stop(0);
        this.executor.shutdownNow();
    }

    /**
     * Answer one request: the zip for the current pack's path, not found for anything else.
     *
     * @param exchange the request
     * @param pack the current pack, or null before the first build
     * @throws IOException if the connection fails
     */
    private static void answer(final HttpExchange exchange, final @Nullable ResourcePack pack) throws IOException {
        try (exchange) {
            boolean get = exchange.getRequestMethod().equals("GET");
            String path = exchange.getRequestURI().getPath();
            if (pack == null || !get || !path.equals("/" + pack.sha1() + ".zip")) {
                exchange.sendResponseHeaders(NOT_FOUND, NO_BODY);
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(OK, pack.size());
            try (OutputStream body = exchange.getResponseBody()) {
                pack.writeZip(body);
            }
        }
    }
}
