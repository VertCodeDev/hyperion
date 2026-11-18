package dev.vertcode.hyperion.effect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.effect.Effect;
import dev.vertcode.hyperion.api.effect.EffectOptions;
import dev.vertcode.hyperion.api.effect.EffectViewers;

/**
 * The options of one play, starting from what the effect says. Used on the calling thread only,
 * then read once into the play.
 */
final class EffectPlay implements EffectOptions {

    private final Map<String, String> parameters;
    private final List<Runnable> revealActions = new ArrayList<>();
    private final List<Runnable> endActions = new ArrayList<>();
    private final List<UUID> camera = new ArrayList<>();
    private EffectViewers viewers;
    private @Nullable UUID owner;

    /**
     * Start from an effect's own viewers and parameters.
     *
     * @param effect the effect
     */
    EffectPlay(final Effect effect) {
        this.viewers = effect.viewers();
        this.parameters = new LinkedHashMap<>(effect.parameters());
    }

    @Override
    public EffectOptions viewers(final EffectViewers viewers) {
        this.viewers = viewers;

        return this;
    }

    @Override
    public EffectOptions parameter(final String name, final String value) {
        this.parameters.put(name, value);

        return this;
    }

    @Override
    public EffectOptions camera(final Player... players) {
        for (Player player : players) {
            this.camera.add(player.getUniqueId());
        }

        return this;
    }

    @Override
    public EffectOptions owner(final Player player) {
        this.owner = player.getUniqueId();

        return this;
    }

    @Override
    public EffectOptions onReveal(final Runnable action) {
        this.revealActions.add(action);

        return this;
    }

    @Override
    public EffectOptions onEnd(final Runnable action) {
        this.endActions.add(action);

        return this;
    }

    EffectViewers viewers() {
        return this.viewers;
    }

    Map<String, String> parameters() {
        return Map.copyOf(this.parameters);
    }

    @Nullable UUID owner() {
        return this.owner;
    }

    List<Runnable> revealActions() {
        return List.copyOf(this.revealActions);
    }

    List<Runnable> endActions() {
        return List.copyOf(this.endActions);
    }

    List<UUID> camera() {
        return List.copyOf(this.camera);
    }
}
