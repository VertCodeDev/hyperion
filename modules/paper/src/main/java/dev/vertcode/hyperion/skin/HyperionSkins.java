package dev.vertcode.hyperion.skin;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.skin.Skin;
import dev.vertcode.hyperion.api.skin.SkinService;

/**
 * Downloads player skins and keeps them.
 *
 * <p>A skin is found by the address of its image, which a player's profile names. The image
 * never changes under one address, so a downloaded skin stays good: a player who changes skin
 * gets a new address. Downloads run on the async executor, never on a tick thread.
 */
public final class HyperionSkins implements SkinService {

    private static final String DOWNLOAD_FAILED = "Could not download the skin at %s; the plain skin is used instead";
    private static final String NOT_AN_IMAGE = "The skin at %s is not an image";
    private static final String BAD_STATUS = "The skin at %s answered with status %d";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int OK = 200;
    private static final int MAX_REMEMBERED = 512;

    private final boolean supported;
    private final Executor async;
    private final Logger logger;
    private final HttpClient http;
    private final HyperionSkin fallback = HyperionSkin.plain();
    private final Map<String, CompletableFuture<Skin>> skins = new ConcurrentHashMap<>();

    /**
     * Create the skin service.
     *
     * @param supported whether this server can draw skins, from 1.21.4
     * @param async runs downloads off the tick threads
     * @param logger where failed downloads are logged
     */
    public HyperionSkins(final boolean supported, final Executor async, final Logger logger) {
        this.supported = supported;
        this.async = async;
        this.logger = logger;
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).executor(async).build();
    }

    @Override
    public boolean supported() {
        return this.supported;
    }

    @Override
    public CompletableFuture<Skin> of(final Player player) {
        return this.ofTextures(player.getPlayerProfile().getTextures());
    }

    @Override
    public CompletableFuture<Skin> of(final PlayerProfile profile) {
        if (!profile.getTextures().isEmpty()) {
            return this.ofTextures(profile.getTextures());
        }

        return profile.update()
                .thenCompose(updated -> this.ofTextures(updated.getTextures()))
                .exceptionally(error -> this.fallback);
    }

    @Override
    public Skin of(final BufferedImage image, final boolean slim) {
        return HyperionSkin.of(image, slim);
    }

    @Override
    public Skin fallback() {
        return this.fallback;
    }

    /**
     * Get the skin a profile's textures name.
     *
     * @param textures the textures
     * @return the skin, or the fallback when there is none or it cannot be downloaded
     */
    private CompletableFuture<Skin> ofTextures(final PlayerTextures textures) {
        @Nullable URL address = textures.getSkin();
        if (address == null) {
            return CompletableFuture.completedFuture(this.fallback);
        }

        boolean slim = textures.getSkinModel() == PlayerTextures.SkinModel.SLIM;
        String key = address + (slim ? "#slim" : "#wide");
        CompletableFuture<Skin> known = this.skins.get(key);
        if (known != null) {
            return known;
        }

        if (this.skins.size() >= MAX_REMEMBERED) {
            this.skins.clear();
        }

        CompletableFuture<Skin> download = this.download(address, slim);
        this.skins.put(key, download);
        // A failed download gave the fallback: forget it, so the next ask tries again.
        download.thenAccept(skin -> {
            if (skin.equals(this.fallback)) {
                this.skins.remove(key, download);
            }
        });

        return download;
    }

    /**
     * Download a skin image and read it.
     *
     * @param address where the image is
     * @param slim whether the skin is made for the slim arms
     * @return the skin, or the fallback when it fails, which is logged
     */
    private CompletableFuture<Skin> download(final URL address, final boolean slim) {
        URI uri;
        try {
            uri = address.toURI();
        } catch (URISyntaxException exception) {
            this.logger.log(Level.WARNING, String.format(DOWNLOAD_FAILED, address), exception);
            return CompletableFuture.completedFuture(this.fallback);
        }

        HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();

        return this.http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApplyAsync(response -> this.read(address, response, slim), this.async)
                .exceptionally(error -> {
                    this.logger.log(Level.WARNING, String.format(DOWNLOAD_FAILED, address), error);
                    return this.fallback;
                });
    }

    /**
     * Read a downloaded skin image.
     *
     * @param address where the image came from, for the error
     * @param response the download
     * @param slim whether the skin is made for the slim arms
     * @return the skin
     * @throws IllegalStateException if the download did not give a skin image
     */
    private Skin read(final URL address, final HttpResponse<InputStream> response, final boolean slim) {
        try (InputStream body = response.body()) {
            if (response.statusCode() != OK) {
                throw new IllegalStateException(String.format(BAD_STATUS, address, response.statusCode()));
            }

            BufferedImage image = ImageIO.read(body);
            if (image == null) {
                throw new IllegalStateException(String.format(NOT_AN_IMAGE, address));
            }

            return HyperionSkin.of(image, slim);
        } catch (IOException exception) {
            throw new IllegalStateException(String.format(DOWNLOAD_FAILED, address), exception);
        }
    }
}
