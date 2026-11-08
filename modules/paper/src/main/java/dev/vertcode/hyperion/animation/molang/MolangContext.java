package dev.vertcode.hyperion.animation.molang;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * What a Molang expression reads while it runs: the animation's time, its variables and a source
 * of randomness. One context is reused for every sample of a bake, so it is not thread-safe.
 */
public final class MolangContext {

    private final Map<String, Float> variables = new HashMap<>();
    private final Random random;
    private float animTime;

    /**
     * Create a context.
     *
     * @param random where {@code math.random} gets its numbers
     */
    public MolangContext(final Random random) {
        this.random = random;
    }

    /**
     * Return {@code query.anim_time}: seconds into the animation.
     *
     * @return the time
     */
    public float animTime() {
        return this.animTime;
    }

    /**
     * Set the animation time for the next evaluations.
     *
     * @param seconds the time in seconds
     */
    public void animTime(final float seconds) {
        this.animTime = seconds;
    }

    /**
     * Read a variable.
     *
     * @param name the variable's name without its prefix
     * @return the value, 0 when it was never set
     */
    float variable(final String name) {
        return this.variables.getOrDefault(name, 0f);
    }

    /**
     * Set a variable.
     *
     * @param name the variable's name without its prefix
     * @param value the value
     */
    void variable(final String name, final float value) {
        this.variables.put(name, value);
    }

    Random random() {
        return this.random;
    }
}
