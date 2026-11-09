package dev.vertcode.hyperion.geyser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.geysermc.geyser.api.extension.ExtensionLogger;

/**
 * The extension's side of one server's Bedrock link: download the manifest and pack once, then
 * follow the event stream on its own thread, connecting again whenever it drops.
 */
final class LinkClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final long RETRY_MILLIS = 3_000L;
    private static final String STREAM_LOST = "Lost the Bedrock link to %s, connecting again: %s";

    private final LinkServer server;
    private final ExtensionLogger logger;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private volatile boolean running = true;

    /**
     * Create the client.
     *
     * @param server the server to link to
     * @param logger where problems are logged
     */
    LinkClient(final LinkServer server, final ExtensionLogger logger) {
        this.server = server;
        this.logger = logger;
    }

    /**
     * Download the models the server has.
     *
     * @return the models
     * @throws IOException if the server cannot be reached or refuses the token
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    List<BedrockModel> manifest() throws IOException, InterruptedException {
        HttpResponse<String> response = this.http.send(this.get("/manifest"), HttpResponse.BodyHandlers.ofString());
        check(response.statusCode());
        List<BedrockModel> models = new ArrayList<>();
        JsonElement json = JsonParser.parseString(response.body());
        if (json instanceof JsonObject root && root.get("models") instanceof JsonArray entries) {
            for (JsonElement entry : entries) {
                if (entry instanceof JsonObject model) {
                    List<String> animations = new ArrayList<>();
                    if (model.get("animations") instanceof JsonArray names) {
                        names.forEach(name -> animations.add(name.getAsString()));
                    }

                    models.add(new BedrockModel(model.get("id").getAsInt(), model.get("identifier").getAsString(), animations));
                }
            }
        }

        return models;
    }

    /**
     * Download the server's Bedrock pack into a file.
     *
     * @param file where to write it
     * @throws IOException if the server cannot be reached or refuses the token
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void pack(final Path file) throws IOException, InterruptedException {
        HttpResponse<byte[]> response = this.http.send(this.get("/pack"), HttpResponse.BodyHandlers.ofByteArray());
        check(response.statusCode());
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.write(file, response.body());
    }

    /**
     * Follow the event stream on a new thread until {@link #stop()}.
     *
     * @param events what to run for every event, on the stream's thread
     */
    void follow(final Consumer<JsonObject> events) {
        Thread thread = new Thread(() -> this.loop(events), "Hyperion link " + this.server.url());
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Tell the server who joined or left through Geyser. Does not wait.
     *
     * @param joined the players who joined
     * @param left the players who left
     */
    void report(final Collection<UUID> joined, final Collection<UUID> left) {
        JsonObject body = new JsonObject();
        body.add("join", uuids(joined));
        body.add("leave", uuids(left));
        HttpRequest request = HttpRequest.newBuilder(URI.create(this.server.url() + "/players"))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + this.server.token())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        this.http.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    /**
     * Stop following the stream.
     */
    void stop() {
        this.running = false;
    }

    String url() {
        return this.server.url();
    }

    private void loop(final Consumer<JsonObject> events) {
        while (this.running) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(this.server.url() + "/stream"))
                        .header("Authorization", "Bearer " + this.server.token())
                        .GET()
                        .build();
                HttpResponse<InputStream> response = this.http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                check(response.statusCode());
                try (BufferedReader lines = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while (this.running && (line = lines.readLine()) != null) {
                        if (!line.isBlank() && JsonParser.parseString(line) instanceof JsonObject event) {
                            events.accept(event);
                        }
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException | RuntimeException exception) {
                this.logger.warning(String.format(STREAM_LOST, this.server.url(), exception.getMessage()));
            }

            try {
                Thread.sleep(RETRY_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private HttpRequest get(final String path) {
        return HttpRequest.newBuilder(URI.create(this.server.url() + path))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + this.server.token())
                .GET()
                .build();
    }

    private static void check(final int status) throws IOException {
        if (status == 401) {
            throw new IOException("the server refused the token; copy it from plugins/Hyperion/bedrock-token.txt");
        }

        if (status != 200) {
            throw new IOException("the server answered " + status);
        }
    }

    private static JsonArray uuids(final Collection<UUID> players) {
        JsonArray array = new JsonArray();
        for (UUID player : players) {
            array.add(player.toString());
        }

        return array;
    }
}
