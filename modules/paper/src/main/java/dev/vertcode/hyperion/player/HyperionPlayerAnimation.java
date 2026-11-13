package dev.vertcode.hyperion.player;

import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.entity.Player;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.player.PlayerAnimation;

/**
 * An animation playing on a player.
 */
final class HyperionPlayerAnimation implements PlayerAnimation {

    private final HyperionPlayerAnimations animations;
    private final Player player;
    private final Model model;
    private final String animation;
    private final boolean locked;
    private final boolean stopOnSneak;
    private final boolean stopOnDamage;
    private final AtomicBoolean ended = new AtomicBoolean();

    /**
     * Create an animation that is playing.
     *
     * @param animations the service that ends it
     * @param player the player
     * @param model the model that stands in for them
     * @param animation the animation's name
     * @param options what it was played with
     */
    HyperionPlayerAnimation(
            final HyperionPlayerAnimations animations,
            final Player player,
            final Model model,
            final String animation,
            final HyperionPlayerAnimationOptions options
    ) {
        this.animations = animations;
        this.player = player;
        this.model = model;
        this.animation = animation;
        this.locked = options.locked;
        this.stopOnSneak = options.stopOnSneak;
        this.stopOnDamage = options.stopOnDamage;
    }

    @Override
    public Player player() {
        return this.player;
    }

    @Override
    public Model model() {
        return this.model;
    }

    @Override
    public String animation() {
        return this.animation;
    }

    @Override
    public boolean locked() {
        return this.locked;
    }

    @Override
    public boolean isPlaying() {
        return !this.ended.get();
    }

    @Override
    public void stop() {
        this.animations.end(this, false);
    }

    boolean stopOnSneak() {
        return this.stopOnSneak;
    }

    boolean stopOnDamage() {
        return this.stopOnDamage;
    }

    /**
     * Mark the animation as over.
     *
     * @return true the first time, false when it was over already
     */
    boolean markEnded() {
        return this.ended.compareAndSet(false, true);
    }
}
