package dev.vertcode.hyperion.effect.part;

import java.util.Locale;

import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import dev.vertcode.hyperion.api.effect.EffectActor;
import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;

/**
 * A sound at each keyframe.
 *
 * <p>Values: {@code sound}, a key such as {@code entity.player.levelup}, {@code volume},
 * {@code pitch} and {@code category}, such as {@code hostile}, which players' sound sliders
 * follow.
 */
final class SoundActor implements EffectActor {

    private final EffectContext context;

    /**
     * Create the sounds of one part.
     *
     * @param context the effect it plays in
     */
    SoundActor(final EffectContext context) {
        this.context = context;
    }

    @Override
    public void frame(final int tick, final EffectFrame frame) {
        String sound = frame.string("sound").orElse(null);
        Location at = this.context.location(frame);
        if (sound == null || at == null || !frame.bool("visible", true)) {
            return;
        }

        SoundCategory category = category(frame.string("category", "neutral"));
        float volume = frame.number("volume", 1f);
        float pitch = frame.number("pitch", 1f);
        for (Player viewer : this.context.viewers()) {
            viewer.playSound(at, sound, category, volume, pitch);
        }
    }

    /**
     * Find a sound category by name.
     *
     * @param name the name, any case
     * @return the category, neutral for an unknown name
     */
    private static SoundCategory category(final String name) {
        try {
            return SoundCategory.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return SoundCategory.NEUTRAL;
        }
    }
}
