package dev.vertcode.hyperion.effect;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.api.effect.EffectHandle;

/**
 * What a part marked {@code heavy: true} sees of its effect: everything, but without the viewers
 * who want fewer effects, so no part type has to know about them.
 */
final class ReducedContext implements EffectContext {

    private final EffectShow show;

    /**
     * Wrap a play for one of its heavy parts.
     *
     * @param show the play
     */
    ReducedContext(final EffectShow show) {
        this.show = show;
    }

    @Override
    public EffectHandle handle() {
        return this.show;
    }

    @Override
    public @Nullable Location location(final EffectFrame frame) {
        return this.show.location(frame);
    }

    @Override
    public Vector3f turn(final EffectFrame frame, final float right, final float up, final float forward) {
        return this.show.turn(frame, right, up, forward);
    }

    @Override
    public Quaternionf rotation(final EffectFrame frame) {
        return this.show.rotation(frame);
    }

    @Override
    public List<Player> cameraViewers() {
        return this.show.cameraViewers();
    }

    @Override
    public List<Player> viewers() {
        return this.show.fullViewers();
    }

    @Override
    public String fill(final String text) {
        return this.show.fill(text);
    }

    @Override
    public int particles(final int wanted) {
        return this.show.particles(wanted);
    }
}
