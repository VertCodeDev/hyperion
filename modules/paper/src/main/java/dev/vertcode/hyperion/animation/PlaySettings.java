package dev.vertcode.hyperion.animation;

import dev.vertcode.hyperion.api.animation.LoopMode;
import dev.vertcode.hyperion.api.animation.PlayOptions;

/**
 * The options a caller gives one animation, starting from how it was made. Used on the calling
 * thread only, then read once into the playing animation.
 */
final class PlaySettings implements PlayOptions {

    private static final String SPEED_NOT_POSITIVE = "An animation speed must be above 0, got %s";
    private static final String NEGATIVE_TICKS = "Fade ticks must be 0 or more, got %d";

    private float speed = 1f;
    private LoopMode loop;
    private int fadeIn;
    private int fadeOut;

    /**
     * Start from how the animation was made.
     *
     * @param loop the loop mode set in Blockbench
     */
    PlaySettings(final LoopMode loop) {
        this.loop = loop;
    }

    @Override
    public PlayOptions speed(final float speed) {
        this.speed = checkSpeed(speed);

        return this;
    }

    @Override
    public PlayOptions loop(final LoopMode loop) {
        this.loop = loop;

        return this;
    }

    @Override
    public PlayOptions fadeIn(final int ticks) {
        this.fadeIn = checkTicks(ticks);

        return this;
    }

    @Override
    public PlayOptions fadeOut(final int ticks) {
        this.fadeOut = checkTicks(ticks);

        return this;
    }

    float speed() {
        return this.speed;
    }

    LoopMode loop() {
        return this.loop;
    }

    int fadeIn() {
        return this.fadeIn;
    }

    int fadeOut() {
        return this.fadeOut;
    }

    /**
     * Check a speed.
     *
     * @param speed the speed
     * @return the speed
     * @throws IllegalArgumentException if it is not above 0
     */
    static float checkSpeed(final float speed) {
        if (!(speed > 0f) || !Float.isFinite(speed)) {
            throw new IllegalArgumentException(String.format(SPEED_NOT_POSITIVE, speed));
        }

        return speed;
    }

    private static int checkTicks(final int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException(String.format(NEGATIVE_TICKS, ticks));
        }

        return ticks;
    }
}
