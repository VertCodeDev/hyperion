package dev.vertcode.hyperion.effect.part;

import java.util.List;
import java.util.Locale;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Material;

import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.render.PropData;

/**
 * An item prop.
 *
 * <p>Values: {@code item}, an item name such as {@code diamond}, and {@code display}, how it is
 * held: {@code none}, {@code fixed}, {@code ground}, {@code gui}, {@code head} and the hand ones.
 */
final class ItemActor extends DisplayActor {

    private static final List<String> DISPLAYS = List.of(
            "none", "thirdperson_lefthand", "thirdperson_righthand", "firstperson_lefthand",
            "firstperson_righthand", "head", "gui", "ground", "fixed"
    );

    /**
     * Create an item prop.
     *
     * @param context the effect it plays in
     */
    ItemActor(final EffectContext context) {
        super(context, EntityTypes.ITEM_DISPLAY);
    }

    @Override
    protected void content(final List<EntityData<?>> into, final EffectFrame frame) {
        Material material = Material.matchMaterial(frame.string("item", "stone"));
        // Bukkit's ItemStack is qualified: this class sends PacketEvents' ItemStack, of the same name.
        ItemStack item = material == null || !material.isItem() || material.isAir()
                ? ItemStack.EMPTY
                : SpigotConversionUtil.fromBukkitItemStack(new org.bukkit.inventory.ItemStack(material));
        int display = DISPLAYS.indexOf(frame.string("display", "none").toLowerCase(Locale.ROOT));
        PropData.item(into, item, (byte) Math.max(0, display));
    }

    @Override
    protected String contentKey(final EffectFrame frame) {
        return frame.string("item", "stone") + '\u0000' + frame.string("display", "none");
    }
}
