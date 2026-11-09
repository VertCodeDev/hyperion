package dev.vertcode.hyperion.animation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.vertcode.hyperion.api.animation.AnimationHandle;
import dev.vertcode.hyperion.api.animation.LoopMode;

/**
 * One animation playing on a model: where it is, how strongly it applies, and whether it ended.
 *
 * <p>The api methods are safe from any thread: they record a wish the next frame acts on. The
 * frame state belongs to the thread that owns the model.
 */
final class PlayingAnimation implements AnimationHandle {

    private final ModelAnimator animator;
    private final String name;
    private final LoopMode loop;
    private final int fadeIn;
    private final int fadeOut;
    private final List<Runnable> endActions = new CopyOnWriteArrayList<>();
    private volatile BakedAnimation baked;
    private volatile float speed;
    private volatile float seconds;
    private volatile boolean playing = true;
    private volatile boolean stopWanted;
    private volatile boolean dropWanted;
    private boolean started;
    private boolean ended;
    private boolean finished;
    private int delay;
    private boolean waiting;
    private int loopWait;
    private int age;
    private int fadeLeft = -1;
    private float frame;
    private float weight;
    private boolean fresh = true;
    private float passedFrom;
    private float passedTo;
    private boolean passedWrapped;

    /**
     * Start an animation.
     *
     * @param animator the animator playing it
     * @param baked the baked animation
     * @param settings the caller's options
     */
    PlayingAnimation(final ModelAnimator animator, final BakedAnimation baked, final PlaySettings settings) {
        this.animator = animator;
        this.name = baked.name();
        this.baked = baked;
        this.speed = settings.speed();
        this.loop = settings.loop();
        this.fadeIn = settings.fadeIn();
        this.fadeOut = settings.fadeOut();
        this.delay = baked.startDelay();
        this.waiting = this.delay > 0;
    }

    // Frames

    /**
     * Move one tick on. Call once per frame on the owning thread.
     */
    void advance() {
        this.passedFrom = 0f;
        this.passedTo = 0f;
        this.passedWrapped = false;
        if (this.ended) {
            return;
        }

        if (this.dropWanted) {
            this.end(false);
            return;
        }

        if (this.stopWanted && this.fadeLeft < 0) {
            if (this.fadeOut == 0) {
                this.end(false);
                return;
            }

            this.fadeLeft = this.fadeOut;
        }

        if (this.delay > 0) {
            this.delay--;
            this.waiting = true;
            return;
        }

        this.waiting = false;
        this.age++;
        if (!this.step()) {
            return;
        }

        this.weight = this.fadeIn > 0 ? Math.min(1f, (float) this.age / this.fadeIn) : 1f;
        if (this.fadeLeft >= 0) {
            this.fadeLeft--;
            this.weight *= (float) Math.max(0, this.fadeLeft) / this.fadeOut;
            if (this.fadeLeft <= 0) {
                this.end(this.finished);
                return;
            }
        }

        this.seconds = this.frame / AnimationBaker.TICKS_PER_SECOND;
    }

    /**
     * Move the frame on by the speed and handle the end of a run.
     *
     * @return false when the animation ended
     */
    private boolean step() {
        float length = this.baked.length();
        if (this.loopWait > 0) {
            this.loopWait--;
            if (this.loopWait == 0) {
                this.frame = 0f;
                this.passed(-1f, 0f, false);
            }

            return true;
        }

        // The first step starts just before 0, so keyframes on tick 0 fire.
        float before = this.fresh ? -1f : this.frame;
        this.fresh = false;
        this.frame += this.speed;
        if (this.frame < length) {
            this.passed(before, this.frame, false);
            return true;
        }

        switch (this.loop) {
            case LOOP -> {
                if (this.baked.loopDelay() > 0) {
                    this.frame = length;
                    this.loopWait = this.baked.loopDelay();
                    this.passed(before, length, false);
                } else {
                    this.frame = length > 0f ? this.frame % length : 0f;
                    this.passed(before, this.frame, true);
                }
            }
            case HOLD -> {
                this.frame = length;
                this.passed(before, length, false);
            }
            case ONCE -> {
                this.frame = length;
                this.passed(before, length, false);
                this.finished = true;
                if (this.fadeLeft < 0) {
                    if (this.fadeOut == 0) {
                        this.end(true);
                        return false;
                    }

                    this.fadeLeft = this.fadeOut;
                }
            }
        }

        return true;
    }

    /**
     * Note the stretch of frames this tick moved over, for the effect keyframes in it.
     *
     * @param from the frame before, left out
     * @param to the frame after, included
     * @param wrapped true when the stretch ran past the end and on from the start
     */
    private void passed(final float from, final float to, final boolean wrapped) {
        this.passedFrom = from;
        this.passedTo = to;
        this.passedWrapped = wrapped;
    }

    /**
     * Return whether this tick moved over a tick of the animation, so an effect keyframe there
     * fires once, loops included.
     *
     * @param tick the tick of the animation
     * @return true when the playhead passed it this tick
     */
    boolean passed(final int tick) {
        if (this.passedWrapped) {
            return (tick > this.passedFrom && tick <= this.baked.length()) || tick <= this.passedTo;
        }

        return tick > this.passedFrom && tick <= this.passedTo;
    }

    /**
     * End the animation.
     *
     * @param reachedEnd true when it played to its end, false when it was stopped
     */
    private void end(final boolean reachedEnd) {
        this.ended = true;
        this.finished = reachedEnd;
        this.playing = false;
    }

    /**
     * Return whether the animation moves bones this frame.
     *
     * @return false while it waits for its start delay or after it ended
     */
    boolean applies() {
        return !this.ended && this.started && !this.waiting;
    }

    boolean started() {
        return this.started;
    }

    void markStarted() {
        this.started = true;
    }

    boolean ended() {
        return this.ended;
    }

    boolean finished() {
        return this.finished;
    }

    /**
     * Drop the animation on the next frame without a fade, as when it is started again.
     */
    void drop() {
        this.dropWanted = true;
    }

    /**
     * Follow a reloaded blueprint's animation of the same name.
     *
     * @param next the new baked animation
     */
    void rebind(final BakedAnimation next) {
        this.baked = next;
        this.frame = Math.min(this.frame, next.length());
    }

    BakedAnimation baked() {
        return this.baked;
    }

    float frame() {
        return this.frame;
    }

    float weight() {
        return this.weight;
    }

    List<Runnable> endActions() {
        return this.endActions;
    }

    // Api

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public boolean isPlaying() {
        return this.playing;
    }

    @Override
    public float time() {
        return this.seconds;
    }

    @Override
    public float length() {
        return this.baked.seconds();
    }

    @Override
    public float speed() {
        return this.speed;
    }

    @Override
    public AnimationHandle speed(final float speed) {
        this.speed = PlaySettings.checkSpeed(speed);

        return this;
    }

    @Override
    public LoopMode loop() {
        return this.loop;
    }

    @Override
    public AnimationHandle onEnd(final Runnable action) {
        if (this.playing) {
            this.endActions.add(action);
        } else {
            this.animator.later(action);
        }

        return this;
    }

    @Override
    public void stop() {
        this.stopWanted = true;
    }
}
