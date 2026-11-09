package dev.vertcode.hyperion.api.effect;

import net.kyori.adventure.key.Keyed;

/**
 * A kind of part effects can use, such as Hyperion's {@code particle} and {@code sound}, or one
 * a plugin adds:
 *
 * <pre>{@code
 * Hyperion.effects().registerPartType(this, new EffectPartType() {
 *     public Key key() { return Key.key("myplugin", "shake"); }
 *     public boolean sampled() { return false; }
 *     public EffectActor create(EffectPart part, EffectContext context) {
 *         return (tick, frame) -> context.viewers().forEach(player -> shake(player, frame.number("power", 1f)));
 *     }
 * });
 * }</pre>
 *
 * <p>An effect names the type in its part's {@code type}, such as {@code myplugin:shake}.
 */
public interface EffectPartType extends Keyed {

    /**
     * Return whether the part acts every tick on values eased between keyframes, like an
     * emitter, rather than once at each keyframe, like a sound.
     *
     * @return true for every tick
     */
    boolean sampled();

    /**
     * Create the actor for one part of one playing effect.
     *
     * @param part the part
     * @param context the effect it plays in
     * @return the actor
     */
    EffectActor create(EffectPart part, EffectContext context);
}
