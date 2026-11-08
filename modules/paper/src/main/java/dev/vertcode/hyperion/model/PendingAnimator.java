package dev.vertcode.hyperion.model;

import java.util.Collection;
import java.util.List;

import dev.vertcode.hyperion.api.animation.AnimationHandle;
import dev.vertcode.hyperion.api.animation.Animator;

/**
 * The animator every model has until animations are played: playing refuses, stopping does
 * nothing, and nothing is ever playing.
 */
final class PendingAnimator implements Animator {

    /**
     * The one instance, shared by every model.
     */
    static final PendingAnimator INSTANCE = new PendingAnimator();

    private static final String NOT_YET = "Hyperion does not play animations yet; they arrive in a later version";

    private PendingAnimator() {
    }

    @Override
    public AnimationHandle play(final String name) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public void stop(final String name) {
        // Nothing plays, so nothing stops.
    }

    @Override
    public void stopAll() {
        // Nothing plays, so nothing stops.
    }

    @Override
    public Collection<AnimationHandle> playing() {
        return List.of();
    }
}
