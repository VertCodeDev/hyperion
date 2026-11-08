package dev.vertcode.hyperion.api.animation;

/**
 * What an animation does when it reaches its end.
 */
public enum LoopMode {

    /**
     * Stop, and let the bones go back to how they are without it.
     */
    ONCE,

    /**
     * Start again from the first frame.
     */
    LOOP,

    /**
     * Stay on the last frame until stopped.
     */
    HOLD
}
