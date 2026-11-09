package dev.vertcode.hyperion.api.effect;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * What a part's actor can ask of the effect it plays in.
 */
@ApiStatus.NonExtendable
public interface EffectContext {

    /**
     * Return the effect that plays.
     *
     * @return the handle
     */
    EffectHandle handle();

    /**
     * Work out where a frame puts its part: its {@code at}, then its {@code offset} turned with
     * that spot.
     *
     * @param frame the part's values now
     * @return the location, or null while what it is at is gone
     */
    @Nullable Location location(EffectFrame frame);

    /**
     * Turn an offset in the anchor's space, right, up and forward, into the world's.
     *
     * @param frame the part's values now, for its {@code at}
     * @param right blocks to the right
     * @param up blocks up
     * @param forward blocks forward
     * @return the offset in the world, as x, y and z
     */
    Vector3f turn(EffectFrame frame, float right, float up, float forward);

    /**
     * Return how a frame's part is turned in the world: forward is where its {@code at} faces.
     * A prop's own {@code rotation} goes on top of this.
     *
     * @param frame the part's values now, for its {@code at}
     * @return the rotation, from a space where X is left, Y up and Z forward
     */
    Quaternionf rotation(EffectFrame frame);

    /**
     * Return who sees and hears the effect this tick.
     *
     * @return the players
     */
    List<Player> viewers();

    /**
     * Fill the effect's {@code %parameters%} into a text.
     *
     * @param text the text
     * @return the text with every known parameter filled in
     */
    String fill(String text);

    /**
     * Scale a particle count by {@code effects.particle-multiplier} in {@code config.yml}.
     *
     * @param wanted the count the effect asks for
     * @return the count to send, at least 1 when the effect asks for any
     */
    int particles(int wanted);
}
