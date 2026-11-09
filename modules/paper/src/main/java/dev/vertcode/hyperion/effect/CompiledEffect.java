package dev.vertcode.hyperion.effect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.effect.Effect;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.api.effect.EffectKeyframe;
import dev.vertcode.hyperion.api.effect.EffectPart;

/**
 * An effect turned into what a tick needs, once, shared by every play of it.
 *
 * <p>Parts that act at keyframes become steps sorted by tick, each with its keyframe's values over
 * the part's settings, so a tick only moves a cursor. Parts that act every tick keep their
 * keyframes with each one's values laid over all before it, so the values at any tick are one
 * blend of two neighbours.
 */
final class CompiledEffect {

    private final Effect effect;
    private final List<Step> steps;
    private final boolean[] sampled;
    private final EffectFrame[][] layered;

    /**
     * Compile an effect.
     *
     * @param effect the effect
     * @param sampled tells which part types act every tick
     */
    CompiledEffect(final Effect effect, final Predicate<Key> sampled) {
        this.effect = effect;
        List<EffectPart> parts = effect.parts();
        List<Step> found = new ArrayList<>();
        this.sampled = new boolean[parts.size()];
        this.layered = new EffectFrame[parts.size()][];
        for (int i = 0; i < parts.size(); i++) {
            EffectPart part = parts.get(i);
            this.sampled[i] = sampled.test(part.type());
            if (!this.sampled[i]) {
                for (EffectKeyframe keyframe : part.keyframes()) {
                    found.add(new Step(keyframe.tick(), i, keyframe.frame().over(part.settings())));
                }

                continue;
            }

            EffectFrame[] frames = new EffectFrame[part.keyframes().size()];
            EffectFrame running = part.settings();
            for (int k = 0; k < frames.length; k++) {
                running = part.keyframes().get(k).frame().over(running);
                frames[k] = running;
            }

            this.layered[i] = frames;
        }

        found.sort(Comparator.comparingInt(Step::tick));
        this.steps = List.copyOf(found);
    }

    /**
     * Return the effect.
     *
     * @return the effect
     */
    Effect effect() {
        return this.effect;
    }

    /**
     * Return the keyframe steps of every part that acts at keyframes, by tick.
     *
     * @return the steps
     */
    List<Step> steps() {
        return this.steps;
    }

    /**
     * Return whether a part acts every tick.
     *
     * @param part the part's place in the effect
     * @return true for every tick
     */
    boolean sampled(final int part) {
        return this.sampled[part];
    }

    /**
     * Work out a part's values at a tick: its settings without keyframes; nothing before its first
     * keyframe; the last one's after it; and between two, a blend eased by the later one.
     *
     * @param part the part's place in the effect
     * @param tick the tick
     * @return the values, or null while the part waits for its first keyframe
     */
    @Nullable EffectFrame state(final int part, final int tick) {
        EffectPart shape = this.effect.parts().get(part);
        List<EffectKeyframe> keyframes = shape.keyframes();
        if (keyframes.isEmpty()) {
            return shape.settings();
        }

        EffectFrame[] frames = this.layered[part];
        int before = -1;
        for (int k = 0; k < keyframes.size() && keyframes.get(k).tick() <= tick; k++) {
            before = k;
        }

        if (before < 0) {
            return null;
        }

        if (before == keyframes.size() - 1) {
            return frames[before];
        }

        EffectKeyframe from = keyframes.get(before);
        EffectKeyframe to = keyframes.get(before + 1);
        float progress = (float) (tick - from.tick()) / (to.tick() - from.tick());

        return EffectFrame.lerp(frames[before], frames[before + 1], to.easing().apply(progress));
    }

    /**
     * A keyframe of a part that acts at keyframes.
     *
     * @param tick the tick it falls on
     * @param part the part's place in the effect
     * @param frame its values over the part's settings
     */
    record Step(int tick, int part, EffectFrame frame) {
    }
}
