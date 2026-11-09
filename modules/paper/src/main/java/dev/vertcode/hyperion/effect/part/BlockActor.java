package dev.vertcode.hyperion.effect.part;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.render.PropData;

/**
 * A block prop, centred on its spot.
 *
 * <p>Values: {@code block}, block data such as {@code minecraft:oak_stairs[facing=north]}.
 */
final class BlockActor extends DisplayActor {

    private static final Map<String, Integer> STATES = new ConcurrentHashMap<>();

    /**
     * Create a block prop.
     *
     * @param context the effect it plays in
     */
    BlockActor(final EffectContext context) {
        super(context, EntityTypes.BLOCK_DISPLAY);
    }

    @Override
    protected void content(final List<EntityData<?>> into, final EffectFrame frame) {
        PropData.block(into, STATES.computeIfAbsent(frame.string("block", "stone"), BlockActor::state));
    }

    @Override
    protected String contentKey(final EffectFrame frame) {
        return frame.string("block", "stone");
    }

    @Override
    protected Vector3f centre(final Quaternionf rotation, final Vector3f scale) {
        // A block display is drawn from its corner; this moves its middle onto the spot.
        return rotation.transform(new Vector3f(scale).mul(-0.5f));
    }

    /**
     * Find a block state's global id.
     *
     * @param data the block data, as text
     * @return the id, 0 (air) when the text is not a block
     */
    private static int state(final String data) {
        try {
            return SpigotConversionUtil.fromBukkitBlockData(Bukkit.createBlockData(data)).getGlobalId();
        } catch (IllegalArgumentException exception) {
            return 0;
        }
    }
}
