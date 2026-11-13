package dev.vertcode.hyperion.api.player;

import java.util.Optional;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.ModelBlueprint;

/**
 * Plays animations on players: a player model takes a player's place, wearing their skin, and
 * plays an animation.
 *
 * <pre>{@code
 * ModelBlueprint rig = Hyperion.models().find(Key.key("hyperion", "player")).orElseThrow();
 *
 * // An emote: over when the animation is.
 * Hyperion.playerAnimations().play(player, rig, "wave");
 *
 * // A pose that lasts: hands up while the player runs with the egg, until you stop it.
 * Hyperion.playerAnimations().play(player, rig, "hands_up", options -> options.loop(LoopMode.LOOP));
 * Hyperion.playerAnimations().stop(player);
 * }</pre>
 *
 * <p>The player is hidden while the animation plays and shown again when it ends: when it is
 * over, when it is stopped, or when the player leaves, dies or changes worlds. A player has one
 * at a time; a new one replaces the one before. To layer more animations, play them on
 * {@link PlayerAnimation#model()}.
 *
 * <p>{@link #play} runs on the thread that owns the player. The other methods are safe from any
 * thread.
 */
@ApiStatus.NonExtendable
public interface PlayerAnimations {

    /**
     * Play an animation on a player with the default options.
     *
     * @param player the player
     * @param blueprint the player model to show in their place
     * @param animation the name of the animation to play
     * @return what is playing, or empty when a plugin cancelled it
     * @throws IllegalArgumentException if the blueprint has no animation by that name
     * @throws IllegalStateException if not called on the thread that owns the player
     */
    Optional<PlayerAnimation> play(Player player, ModelBlueprint blueprint, String animation);

    /**
     * Play an animation on a player.
     *
     * @param player the player
     * @param blueprint the player model to show in their place
     * @param animation the name of the animation to play
     * @param options changes to the defaults
     * @return what is playing, or empty when a plugin cancelled it
     * @throws IllegalArgumentException if the blueprint has no animation by that name
     * @throws IllegalStateException if not called on the thread that owns the player
     */
    Optional<PlayerAnimation> play(
            Player player,
            ModelBlueprint blueprint,
            String animation,
            Consumer<PlayerAnimationOptions> options
    );

    /**
     * Return the animation playing on a player.
     *
     * @param player the player
     * @return what is playing, or empty when nothing is
     */
    Optional<PlayerAnimation> playing(Player player);

    /**
     * Stop the animation playing on a player.
     *
     * @param player the player
     * @return true when one was playing
     */
    boolean stop(Player player);
}
