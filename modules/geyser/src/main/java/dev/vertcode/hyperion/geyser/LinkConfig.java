package dev.vertcode.hyperion.geyser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The extension's {@code config.json}: the Hyperion servers it links to.
 *
 * <p>Each server is the address of its Bedrock link, {@code http://host:8164} by default, and the
 * token from that server's {@code plugins/Hyperion/bedrock-token.txt}. A network with several
 * backends lists each of them.
 */
final class LinkConfig {

    private static final String FILE = "config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private LinkConfig() {
    }

    /**
     * Read the servers, writing an example file first when there is none.
     *
     * @param folder the extension's data folder
     * @return the servers
     * @throws IOException if the file cannot be read or written
     */
    static List<LinkServer> load(final Path folder) throws IOException {
        Path file = folder.resolve(FILE);
        if (Files.notExists(file)) {
            Files.createDirectories(folder);
            JsonObject server = new JsonObject();
            server.addProperty("url", "http://127.0.0.1:8164");
            server.addProperty("token", "paste the token from plugins/Hyperion/bedrock-token.txt");
            JsonArray servers = new JsonArray();
            servers.add(server);
            JsonObject example = new JsonObject();
            example.add("servers", servers);
            Files.writeString(file, GSON.toJson(example), StandardCharsets.UTF_8);
        }

        JsonElement json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
        List<LinkServer> servers = new ArrayList<>();
        if (json instanceof JsonObject root && root.get("servers") instanceof JsonArray entries) {
            for (JsonElement entry : entries) {
                if (entry instanceof JsonObject server && server.has("url") && server.has("token")) {
                    String url = server.get("url").getAsString().replaceAll("/+$", "");
                    servers.add(new LinkServer(url, server.get("token").getAsString().trim()));
                }
            }
        }

        return servers;
    }
}
