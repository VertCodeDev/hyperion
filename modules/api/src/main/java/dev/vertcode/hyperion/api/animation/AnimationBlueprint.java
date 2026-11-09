package dev.vertcode.hyperion.api.animation;

import java.util.Set;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * An animation as loaded from a model file: how long it runs, what it does at its end and which
 * bones it moves, read before anything plays it.
 *
 * <p>Never changes once loaded, so it is safe to read from any thread.
 *
 * <pre>{@code
 * blueprint.animation("roar").ifPresent(roar -> wait(roar.length()));
 * }</pre>
 */
@ApiStatus.NonExtendable
public interface AnimationBlueprint {

    /**
     * Return the animation's name.
     *
     * @return the name
     */
    String name();

    /**
     * Return how long one run of the animation lasts at speed 1.
     *
     * @return the length in seconds
     */
    float length();

    /**
     * Return what the animation does when it reaches its end, as saved in the file.
     *
     * @return the loop mode
     */
    LoopMode loop();

    /**
     * Return the names of the bones the animation moves.
     *
     * @return the bone names
     */
    @Unmodifiable Set<String> bones();
}
