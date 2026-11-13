package dev.vertcode.hyperion.api.skin;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.ApiStatus;

/**
 * Gets player skins for models to wear.
 *
 * <p>A model draws a skin on every face that uses a texture named {@code player_skin} in its Blockbench
 * file. Give a model the skin of any player, online or not:
 *
 * <pre>{@code
 * Hyperion.skins().of(Bukkit.createProfile("Notch")).thenAccept(model::skin);
 * }</pre>
 *
 * <p>Skins are drawn from Minecraft 1.21.4. On an older server a model shows the skin texture of
 * its own file.
 *
 * <p>Every method is safe from any thread. Skins are downloaded off the tick threads and kept
 * for a while, so asking again is cheap.
 */
@ApiStatus.NonExtendable
public interface SkinService {

    /**
     * Return whether this server can draw skins: Minecraft 1.21.4 or newer.
     *
     * @return true when models wear the skins they are given
     */
    boolean supported();

    /**
     * Get the skin an online player wears now.
     *
     * @param player the player
     * @return the skin, or {@link #fallback()} when it cannot be downloaded
     */
    CompletableFuture<Skin> of(Player player);

    /**
     * Get the skin of a profile. A profile without textures is completed first, which asks
     * Mojang for the player by name or id.
     *
     * @param profile the profile, such as {@code Bukkit.createProfile("Notch")}
     * @return the skin, or {@link #fallback()} when the profile has none or it cannot be
     *     downloaded
     */
    CompletableFuture<Skin> of(PlayerProfile profile);

    /**
     * Make a skin from an image of your own.
     *
     * @param image the skin, 64 by 64 pixels, or 64 by 32 in the old layout
     * @param slim whether it is made for the slim arms
     * @return the skin
     * @throws IllegalArgumentException if the image is not 64 pixels wide and 64 or 32 high
     */
    Skin of(BufferedImage image, boolean slim);

    /**
     * Return the skin used when a player's own cannot be had.
     *
     * @return the fallback skin
     */
    Skin fallback();
}
