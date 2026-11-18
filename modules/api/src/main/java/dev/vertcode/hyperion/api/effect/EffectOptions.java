package dev.vertcode.hyperion.api.effect;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

/**
 * Settings for one play of an effect, on top of what the effect says.
 */
@ApiStatus.NonExtendable
public interface EffectOptions {

    /**
     * Choose who sees and hears it, in place of the effect's own choice.
     *
     * @param viewers the viewers
     * @return these options
     */
    EffectOptions viewers(EffectViewers viewers);

    /**
     * Fill in a parameter, written {@code %name%} in the effect.
     *
     * @param name the parameter's name
     * @param value its value for this play
     * @return these options
     */
    EffectOptions parameter(String name, String value);

    /**
     * Let the effect's camera parts take these players' view. A camera never takes anyone's
     * view unless a play names them.
     *
     * @param players the players
     * @return these options
     */
    EffectOptions camera(Player... players);

    /**
     * Make the play this player's own. They always see and hear it, it ends when they leave, and
     * it takes the turn of the effect they owned before, which stops. An
     * {@linkplain Effect#ambient() ambient} effect plays beside it and stops nothing.
     *
     * <p>Use it for a moment that belongs to one player, such as a crate they open:
     *
     * <pre>{@code
     * Hyperion.effects().play(Key.key("crates", "open"), EffectAnchor.at(crate), options -> options
     *         .owner(player)
     *         .parameter("prize", prize.name())
     *         .onReveal(() -> announce(player, prize))
     *         .onEnd(() -> give(player, prize)));
     * }</pre>
     *
     * @param player the owner
     * @return these options
     */
    EffectOptions owner(Player player);

    /**
     * Run something when the effect shows what it is about: at its {@linkplain Effect#reveal()
     * reveal} tick, on the thread it plays on. It runs once, also when the effect loops, and
     * still runs as the effect ends when it was stopped sooner, so news held back for the moment
     * is never lost. An effect without a reveal tick reveals as it starts.
     *
     * @param action what to run
     * @return these options
     */
    EffectOptions onReveal(Runnable action);

    /**
     * Run something when the effect ends, on the thread it played on.
     *
     * @param action what to run
     * @return these options
     */
    EffectOptions onEnd(Runnable action);
}
