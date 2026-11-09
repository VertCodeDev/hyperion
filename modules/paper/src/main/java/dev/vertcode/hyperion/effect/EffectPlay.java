package dev.vertcode.hyperion.effect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.vertcode.hyperion.api.effect.Effect;
import dev.vertcode.hyperion.api.effect.EffectOptions;
import dev.vertcode.hyperion.api.effect.EffectViewers;

/**
 * The options of one play, starting from what the effect says. Used on the calling thread only,
 * then read once into the play.
 */
final class EffectPlay implements EffectOptions {

    private final Map<String, String> parameters;
    private final List<Runnable> endActions = new ArrayList<>();
    private EffectViewers viewers;

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

    List<Runnable> endActions() {
        return List.copyOf(this.endActions);
    }
}
