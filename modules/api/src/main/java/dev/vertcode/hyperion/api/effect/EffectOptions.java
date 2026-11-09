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
     * Run something when the effect ends, on the thread it played on.
     *
     * @param action what to run
     * @return these options
     */
    EffectOptions onEnd(Runnable action);
}
