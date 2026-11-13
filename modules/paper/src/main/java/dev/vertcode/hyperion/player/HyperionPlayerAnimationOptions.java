package dev.vertcode.hyperion.player;

import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.animation.LoopMode;
import dev.vertcode.hyperion.api.player.PlayerAnimationOptions;

/**
 * The options one animation is played on a player with, starting from the defaults.
 *
 * <p>Used on the thread that plays the animation only.
 */
final class HyperionPlayerAnimationOptions implements PlayerAnimationOptions {

    private static final String SPEED_NOT_POSITIVE = "An animation's speed must be above 0, got %s";

    @Nullable LoopMode loop;
    boolean locked;
    boolean walkAnimations = true;
    boolean stopOnSneak;
    boolean stopOnDamage;
    float speed = 1f;

    /**
     * Start from the defaults.
     *
     * @param stopOnSneak whether sneaking ends the animation, from {@code config.yml}
     * @param stopOnDamage whether damage ends the animation, from {@code config.yml}
     */
    HyperionPlayerAnimationOptions(final boolean stopOnSneak, final boolean stopOnDamage) {
        this.stopOnSneak = stopOnSneak;
        this.stopOnDamage = stopOnDamage;
    }

    @Override
    public PlayerAnimationOptions loop(final LoopMode loop) {
        this.loop = loop;

        return this;
    }

    @Override
    public PlayerAnimationOptions locked(final boolean locked) {
        this.locked = locked;

        return this;
    }

    @Override
    public PlayerAnimationOptions walkAnimations(final boolean enabled) {
        this.walkAnimations = enabled;

        return this;
    }

    @Override
    public PlayerAnimationOptions stopOnSneak(final boolean stop) {
        this.stopOnSneak = stop;

        return this;
    }

    @Override
    public PlayerAnimationOptions stopOnDamage(final boolean stop) {
        this.stopOnDamage = stop;

        return this;
    }

    @Override
    public PlayerAnimationOptions speed(final float speed) {
        if (!(speed > 0f)) {
            throw new IllegalArgumentException(String.format(SPEED_NOT_POSITIVE, speed));
        }

        this.speed = speed;

        return this;
    }
}
