package dev.vertcode.hyperion.effect.part;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import dev.vertcode.hyperion.api.effect.EffectActor;
import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.api.event.EffectKeyframeEvent;

/**
 * An event at each keyframe: {@code EffectKeyframeEvent} with the keyframe's {@code name}, for
 * plugins to act on.
 */
final class EventActor implements EffectActor {

    private final EffectContext context;

    /**
     * Create the events of one part.
     *
     * @param context the effect it plays in
     */
    EventActor(final EffectContext context) {
        this.context = context;
    }

    @Override
    public void frame(final int tick, final EffectFrame frame) {
        String name = frame.string("name").orElse(null);
        Location at = this.context.location(frame);
        if (name != null && at != null) {
            Bukkit.getPluginManager().callEvent(new EffectKeyframeEvent(this.context.handle(), name, at));
        }
    }
}
