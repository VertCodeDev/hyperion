package dev.vertcode.hyperion.player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import dev.vertcode.hyperion.api.animation.AnimationHandle;
import dev.vertcode.hyperion.api.animation.LoopMode;
import dev.vertcode.hyperion.api.event.PlayerAnimationEndEvent;
import dev.vertcode.hyperion.api.event.PlayerAnimationStartEvent;
import dev.vertcode.hyperion.api.model.EntityAnchor;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.ModelBlueprint;
import dev.vertcode.hyperion.api.player.PlayerAnimation;
import dev.vertcode.hyperion.api.player.PlayerAnimationOptions;
import dev.vertcode.hyperion.api.player.PlayerAnimations;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.config.PlayerAnimationSettings;
import dev.vertcode.hyperion.model.ModelManager;

/**
 * Plays animations on players and keeps track of who has one playing.
 *
 * <p>Playing one puts a player model on the player, with the player hidden behind it. The model
 * wears the player's skin by itself, follows them around, and is removed when the animation
 * ends. Starting and ending happen on the thread that owns the player.
 */
public final class HyperionPlayerAnimations implements PlayerAnimations {

    private static final String NOT_OWNER = "An animation must be played on the thread that owns the player";
    private static final String NO_ANIMATION = "The model %s has no animation named '%s'";
    private static final String UNREGISTERED = "The model %s is not registered";
    // A player model is 32 pixels tall, two blocks; the game draws a player a little smaller.
    private static final float PLAYER_SCALE = 0.9375f;

    private final Plugin plugin;
    private final ModelManager models;
    private final BlueprintRegistry registry;
    private final Map<UUID, HyperionPlayerAnimation> playing = new ConcurrentHashMap<>();
    private final AtomicInteger locked = new AtomicInteger();
    private volatile PlayerAnimationSettings settings;

    /**
     * Create the service.
     *
     * @param plugin Hyperion, to run on a player's thread
     * @param models where the player models are spawned
     * @param registry the loaded blueprints
     * @param settings the settings from {@code config.yml}
     */
    public HyperionPlayerAnimations(
            final Plugin plugin,
            final ModelManager models,
            final BlueprintRegistry registry,
            final PlayerAnimationSettings settings
    ) {
        this.plugin = plugin;
        this.models = models;
        this.registry = registry;
        this.settings = settings;
    }

    /**
     * Use new settings from {@code config.yml}, for animations played from now on.
     *
     * @param settings the settings
     */
    public void configure(final PlayerAnimationSettings settings) {
        this.settings = settings;
    }

    @Override
    public Optional<PlayerAnimation> play(final Player player, final ModelBlueprint blueprint, final String animation) {
        return this.play(player, blueprint, animation, options -> { });
    }

    @Override
    public Optional<PlayerAnimation> play(
            final Player player,
            final ModelBlueprint blueprint,
            final String animation,
            final Consumer<PlayerAnimationOptions> options
    ) {
        if (!Bukkit.isOwnedByCurrentRegion(player)) {
            throw new IllegalStateException(NOT_OWNER);
        }

        String key = blueprint.key().asString();
        Blueprint current = this.registry.find(blueprint.key())
                .orElseThrow(() -> new IllegalStateException(String.format(UNREGISTERED, key)));
        if (!current.animations().contains(animation)) {
            throw new IllegalArgumentException(String.format(NO_ANIMATION, key, animation));
        }

        PlayerAnimationSettings settings = this.settings;
        HyperionPlayerAnimationOptions chosen = new HyperionPlayerAnimationOptions(
                settings.stopOnSneak(),
                settings.stopOnDamage()
        );
        options.accept(chosen);
        PlayerAnimationStartEvent event = new PlayerAnimationStartEvent(player, current, animation);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return Optional.empty();
        }

        HyperionPlayerAnimation before = this.playing.get(player.getUniqueId());
        if (before != null) {
            this.end(before, false);
        }

        boolean walks = chosen.walkAnimations && !chosen.locked;
        Model model = this.models.spawn(
                current,
                new EntityAnchor(player),
                spawn -> {
                    spawn.autoAnimations(walks);
                    if (current.skinned()) {
                        spawn.scale(PLAYER_SCALE);
                    }
                },
                settings.showToSelf()
        );
        HyperionPlayerAnimation started = new HyperionPlayerAnimation(this, player, model, animation, chosen);
        this.playing.put(player.getUniqueId(), started);
        if (chosen.locked) {
            this.locked.incrementAndGet();
        }

        LoopMode loop = chosen.loop;
        AnimationHandle handle = model.animator().play(animation, play -> {
            play.speed(chosen.speed).fadeIn(settings.fadeTicks()).fadeOut(settings.fadeTicks());
            if (loop != null) {
                play.loop(loop);
            }
        });
        handle.onEnd(() -> this.end(started, true));

        return Optional.of(started);
    }

    @Override
    public Optional<PlayerAnimation> playing(final Player player) {
        return Optional.ofNullable(this.playing.get(player.getUniqueId()));
    }

    @Override
    public boolean stop(final Player player) {
        HyperionPlayerAnimation current = this.playing.get(player.getUniqueId());
        if (current == null) {
            return false;
        }

        this.end(current, false);

        return true;
    }

    /**
     * Return whether a player is held in place by an animation. Cheap enough for the move
     * event: it looks nothing up while no animation holds anyone.
     *
     * @param player the player
     * @return true when they may not walk
     */
    public boolean holds(final Player player) {
        if (this.locked.get() == 0) {
            return false;
        }

        HyperionPlayerAnimation current = this.playing.get(player.getUniqueId());

        return current != null && current.locked();
    }

    /**
     * End a player's animation because they started sneaking, when it was played to end that
     * way.
     *
     * @param player the player
     */
    public void sneaked(final Player player) {
        HyperionPlayerAnimation current = this.playing.get(player.getUniqueId());
        if (current != null && current.stopOnSneak()) {
            this.end(current, false);
        }
    }

    /**
     * End a player's animation because they took damage, when it was played to end that way.
     *
     * @param player the player
     */
    public void damaged(final Player player) {
        HyperionPlayerAnimation current = this.playing.get(player.getUniqueId());
        if (current != null && current.stopOnDamage()) {
            this.end(current, false);
        }
    }

    /**
     * End the animation that plays on a model, when that model was removed by something else:
     * the player left, a command removed it, or Hyperion reloads.
     *
     * @param model the removed model
     */
    public void removed(final Model model) {
        for (HyperionPlayerAnimation current : this.playing.values()) {
            if (current.model().equals(model)) {
                this.end(current, false);
                return;
            }
        }
    }

    /**
     * Forget every animation, as Hyperion disables. The models are removed with all the others.
     */
    public void stopAll() {
        for (HyperionPlayerAnimation current : this.playing.values()) {
            current.markEnded();
        }

        this.playing.clear();
        this.locked.set(0);
    }

    /**
     * End an animation: forget it, remove its model and tell plugins. Does nothing when it
     * already ended. Safe from any thread; the model goes on the thread that owns the player.
     *
     * @param ending the animation
     * @param finished whether it played to its end
     */
    void end(final HyperionPlayerAnimation ending, final boolean finished) {
        if (!ending.markEnded()) {
            return;
        }

        Player player = ending.player();
        this.playing.remove(player.getUniqueId(), ending);
        if (ending.locked()) {
            this.locked.decrementAndGet();
        }

        if (Bukkit.isOwnedByCurrentRegion(player)) {
            this.finish(ending, finished);
            return;
        }

        // Retired runs when the player is gone, whose models went with them: nothing is left to do.
        player.getScheduler().run(this.plugin, task -> this.finish(ending, finished), () -> { });
    }

    /**
     * Remove an ended animation's model and tell plugins, on the thread that owns the player.
     *
     * @param ended the animation
     * @param finished whether it played to its end
     */
    private void finish(final HyperionPlayerAnimation ended, final boolean finished) {
        Model model = ended.model();
        if (!model.isRemoved()) {
            model.remove();
        }

        PlayerAnimationEndEvent event = new PlayerAnimationEndEvent(ended.player(), ended.animation(), finished);
        Bukkit.getPluginManager().callEvent(event);
    }
}
