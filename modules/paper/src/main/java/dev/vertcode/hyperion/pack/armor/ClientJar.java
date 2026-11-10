package dev.vertcode.hyperion.pack.armor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * The Minecraft client jar of the server's version, downloaded from Mojang once and kept in
 * Hyperion's cache folder.
 *
 * <p>Hyperion reads the vanilla armor shaders and the vanilla leather texture from it, so it can
 * patch and extend them without shipping Mojang's files. The jar is checked against the SHA-1
 * Mojang publishes for it. Downloads block, so call this off the tick threads.
 */
final class ClientJar {

    private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(5);
    private static final int OK = 200;
    private static final String DOWNLOADING = "Downloading the Minecraft %s client from Mojang once, for its armor shaders "
            + "and leather texture; it is kept in %s";
    private static final String FAILED = "Could not get the Minecraft %s client, so custom armor is drawn flat%s: %s";
    private static final String NO_VERSION = "Mojang lists no version %s";
    private static final String BAD_HASH = "the download did not match Mojang's hash";

    private final Path file;

    private ClientJar(final Path file) {
        this.file = file;
    }

    /**
     * Find the client jar in the cache, or download it.
     *
     * @param cache the cache folder
     * @param version the version, as Mojang names it, such as {@code 1.21.4}
     * @param logger where the download and failures are logged
     * @param fallback what the log says custom armor falls back to, such as an empty text
     * @return the jar, or empty when it could not be had
     */
    static Optional<ClientJar> open(final Path cache, final String version, final Logger logger, final String fallback) {
        Path file = cache.resolve("minecraft-" + version + ".jar");
        if (Files.isRegularFile(file)) {
            return Optional.of(new ClientJar(file));
        }

        try {
            Files.createDirectories(cache);
            logger.info(String.format(DOWNLOADING, version, cache));
            download(version, file);
            return Optional.of(new ClientJar(file));
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            logger.log(Level.WARNING, String.format(FAILED, version, fallback, exception.getMessage()));
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /**
     * Read a file from the jar.
     *
     * @param path the file's path in the jar, such as
     *     {@code assets/minecraft/shaders/core/entity.vsh}
     * @return the bytes, or empty when the jar has no such file or cannot be read
     */
    Optional<byte[]> read(final String path) {
        try (ZipFile zip = new ZipFile(this.file.toFile())) {
            ZipEntry entry = zip.getEntry(path);
            if (entry == null) {
                return Optional.empty();
            }

            try (InputStream stream = zip.getInputStream(entry)) {
                return Optional.of(stream.readAllBytes());
            }
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    /**
     * Read a text file from the jar.
     *
     * @param path the file's path in the jar
     * @return the text, or empty when the jar has no such file
     */
    Optional<String> text(final String path) {
        return this.read(path).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * Download a version's client jar, check its hash and move it into place.
     *
     * @param version the version
     * @param target where the jar goes
     * @throws IOException if a download fails
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private static void download(final String version, final Path target) throws IOException, InterruptedException {
        HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build();
        JsonObject manifest = json(http, MANIFEST);
        String url = null;
        for (JsonElement entry : manifest.getAsJsonArray("versions")) {
            JsonObject object = entry.getAsJsonObject();
            if (object.get("id").getAsString().equals(version)) {
                url = object.get("url").getAsString();
                break;
            }
        }

        if (url == null) {
            throw new IllegalStateException(String.format(NO_VERSION, version));
        }

        JsonObject client = json(http, url).getAsJsonObject("downloads").getAsJsonObject("client");
        String sha1 = client.get("sha1").getAsString();
        Path part = target.resolveSibling(target.getFileName() + ".part");
        HttpRequest request = HttpRequest.newBuilder(URI.create(client.get("url").getAsString())).timeout(DOWNLOAD_TIMEOUT).build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != OK) {
            response.body().close();
            throw new IOException("HTTP " + response.statusCode());
        }

        MessageDigest digest = sha1();
        try (InputStream body = new DigestInputStream(response.body(), digest)) {
            Files.copy(body, part, StandardCopyOption.REPLACE_EXISTING);
        }

        if (!HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(sha1)) {
            Files.deleteIfExists(part);
            throw new IOException(BAD_HASH);
        }

        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Fetch and parse a JSON document.
     *
     * @param http the client
     * @param url the document's address
     * @return the parsed object
     * @throws IOException if the request fails
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private static JsonObject json(final HttpClient http, final String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != OK) {
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        }

        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    /**
     * Make a SHA-1 digest.
     *
     * @return the digest
     */
    private static MessageDigest sha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Every Java runtime has SHA-1", exception);
        }
    }

    @Override
    public String toString() {
        return "ClientJar[" + this.file + "]";
    }
}
