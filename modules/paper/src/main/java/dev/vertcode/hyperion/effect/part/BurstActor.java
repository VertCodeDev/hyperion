package dev.vertcode.hyperion.effect.part;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.effect.EffectActor;
import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;

/**
 * A burst of particles at each keyframe.
 *
 * <p>Values: {@code particle}, {@code count}, {@code spread}, {@code speed}, {@code direction},
 * and {@code color}, {@code to-color} and {@code size} for dust.
 */
final class BurstActor implements EffectActor {

    private static final int DEFAULT_COUNT = 10;
    private static final float DEFAULT_SPREAD = 0.2f;

    private final EffectContext context;
    private @Nullable Location here;

    /**
     * Create the bursts of one part.
     *
     * @param context the effect it plays in
     */
    BurstActor(final EffectContext context) {
        this.context = context;
    }

    @Override
    public void frame(final int tick, final EffectFrame frame) {
        Location at = this.context.location(frame);
        this.here = at;
        Particle particle = ParticleData.particle(frame.string("particle", "poof"));
        if (at == null || particle == null || !ParticleData.supported(particle) || !frame.bool("visible", true)) {
            return;
        }

        int count = this.context.particles(Math.round(frame.number("count", DEFAULT_COUNT)));
        List<Player> viewers = count > 0 ? this.context.viewers() : List.of();
        if (viewers.isEmpty()) {
            return;
        }

        Vector3f local = frame.vector("direction");
        Vector3f direction = local == null ? null : this.context.turn(frame, local.x, local.y, local.z);
        ParticleData.send(
                viewers,
                particle,
                at,
                count,
                frame.number("spread", DEFAULT_SPREAD),
                frame.number("speed", 0f),
                ParticleData.data(particle, frame),
                direction
        );
    }

    @Override
    public @Nullable Location location() {
        Location at = this.here;

        return at == null ? null : at.clone();
    }
}
