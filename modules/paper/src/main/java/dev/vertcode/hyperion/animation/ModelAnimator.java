package dev.vertcode.hyperion.animation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.animation.AnimationHandle;
import dev.vertcode.hyperion.api.animation.Animator;
import dev.vertcode.hyperion.api.animation.PlayOptions;
import dev.vertcode.hyperion.api.event.AnimationEndEvent;
import dev.vertcode.hyperion.api.event.AnimationStartEvent;
import dev.vertcode.hyperion.api.model.Model;

/**
 * Plays animations on one model, for everyone or for one player.
 *
 * <p>The api methods are safe from any thread. The model's frame calls {@link #tick()} and
 * {@link #apply} on the thread that owns it, which advances every animation, fires the start
 * and end events and blends the animations into each bone's pose, in the order they started.
 */
public final class ModelAnimator implements Animator {

    private static final String NO_ANIMATION = "The model %s has no animation named '%s'";
    private static final String END_FAILED = "An animation end action on %s failed";

    private final Model model;
    private final Function<String, Optional<BakedAnimation>> animations;
    private final @Nullable Player viewer;
    private final Logger logger;
    private final List<PlayingAnimation> playing = new CopyOnWriteArrayList<>();
    private final Queue<Runnable> later = new ConcurrentLinkedQueue<>();
    private final float[] scratch = new float[9];

    /**
     * Create an animator.
     *
     * @param model the model it animates
     * @param animations finds the model's baked animations by name, following reloads
     * @param viewer the player it plays for, or null when it plays for everyone
     * @param logger where failing end actions are logged
     */
    public ModelAnimator(
            final Model model,
            final Function<String, Optional<BakedAnimation>> animations,
            final @Nullable Player viewer,
            final Logger logger
    ) {
        this.model = model;
        this.animations = animations;
        this.viewer = viewer;
        this.logger = logger;
    }

    @Override
    public AnimationHandle play(final String name) {
        return this.play(name, options -> {
            // Play as made.
        });
    }

    @Override
    public AnimationHandle play(final String name, final Consumer<PlayOptions> options) {
        BakedAnimation baked = this.animations.apply(name).orElseThrow(() -> new IllegalArgumentException(
                String.format(NO_ANIMATION, this.model.blueprint().key().asString(), name)
        ));
        PlaySettings settings = new PlaySettings(baked.loop());
        options.accept(settings);
        PlayingAnimation started = new PlayingAnimation(this, baked, settings);
        synchronized (this.playing) {
            for (PlayingAnimation existing : this.playing) {
                if (existing.name().equals(name)) {
                    existing.drop();
                }
            }

            this.playing.add(started);
        }

        return started;
    }

    @Override
    public void stop(final String name) {
        for (PlayingAnimation animation : this.playing) {
            if (animation.name().equals(name)) {
                animation.stop();
            }
        }
    }

    @Override
    public void stopAll() {
        for (PlayingAnimation animation : this.playing) {
            animation.stop();
        }
    }

    @Override
    public Optional<AnimationHandle> playing(final String name) {
        for (PlayingAnimation animation : this.playing) {
            if (animation.name().equals(name) && animation.isPlaying()) {
                return Optional.of(animation);
            }
        }

        return Optional.empty();
    }

    @Override
    public Collection<AnimationHandle> playing() {
        List<AnimationHandle> current = new ArrayList<>();
        for (PlayingAnimation animation : this.playing) {
            if (animation.isPlaying()) {
                current.add(animation);
            }
        }

        return List.copyOf(current);
    }

    // Frames

    /**
     * Return whether anything plays, so the frame knows the bones move.
     *
     * @return true while at least one animation plays or waits to end
     */
    public boolean active() {
        return !this.playing.isEmpty();
    }

    /**
     * Advance every animation one tick, firing the start and end events. Call once per frame on
     * the owning thread.
     */
    public void tick() {
        Runnable action;
        while ((action = this.later.poll()) != null) {
            this.run(action);
        }

        List<PlayingAnimation> ended = new ArrayList<>();
        for (PlayingAnimation animation : this.playing) {
            if (!animation.started()) {
                animation.markStarted();
                Bukkit.getPluginManager().callEvent(new AnimationStartEvent(this.model, animation, this.viewer));
            }

            animation.advance();
            if (animation.ended()) {
                ended.add(animation);
            }
        }

        this.playing.removeAll(ended);
        for (PlayingAnimation animation : ended) {
            Bukkit.getPluginManager().callEvent(new AnimationEndEvent(this.model, animation, animation.finished(), this.viewer));
            for (Runnable end : animation.endActions()) {
                this.run(end);
            }
        }
    }

    /**
     * Blend every applying animation into the bones' poses, in the order they started. An
     * animation made to override clears what came before it on the bones it moves.
     *
     * @param bones the bones' places in {@code poses}, by name
     * @param poses the poses to add to
     */
    public void apply(final Map<String, Integer> bones, final BonePose[] poses) {
        for (PlayingAnimation animation : this.playing) {
            if (!animation.applies()) {
                continue;
            }

            BakedAnimation baked = animation.baked();
            for (Map.Entry<String, BoneTrack> track : baked.tracks().entrySet()) {
                Integer index = bones.get(track.getKey());
                if (index == null) {
                    continue;
                }

                if (baked.override()) {
                    poses[index].reset();
                }

                track.getValue().apply(animation.frame(), poses[index], animation.weight(), this.scratch);
            }
        }
    }

    /**
     * Follow a reloaded blueprint: every animation moves to the new one of the same name, or
     * stops when it is gone.
     */
    public void rebind() {
        for (PlayingAnimation animation : this.playing) {
            Optional<BakedAnimation> next = this.animations.apply(animation.name());
            if (next.isPresent()) {
                animation.rebind(next.get());
            } else {
                animation.drop();
            }
        }
    }

    /**
     * Stop every animation at once, without fades or events, as when the model is removed.
     */
    public void clear() {
        this.playing.clear();
        this.later.clear();
    }

    /**
     * Run something on the next frame, for an end action added after the animation ended.
     *
     * @param action what to run
     */
    void later(final Runnable action) {
        this.later.add(action);
    }

    /**
     * Run an end action, logging what it throws.
     *
     * @param action the action
     */
    private void run(final Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            this.logger.log(Level.WARNING, String.format(END_FAILED, this.model), exception);
        }
    }
}
