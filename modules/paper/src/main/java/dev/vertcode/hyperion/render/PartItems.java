package dev.vertcode.hyperion.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemCustomModelData;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemDyeColor;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemModel;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Color;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.pack.ModelPart;
import dev.vertcode.hyperion.skin.HyperionSkin;
import dev.vertcode.hyperion.skin.SkinColors;
import dev.vertcode.hyperion.skin.SkinMap;

/**
 * Build the items displays show: a model part on the base item, or an item a bone was given.
 *
 * <p>From 1.21.4 a part names its model through the {@code item_model} component; before, it
 * picks it with custom model data. The tint is the dye color, which the pack's item definitions
 * apply to any base item from 1.21.4 and only dyeable base items show before.
 *
 * <p>A part of a player model also carries the colors of the skin the model wears, one for each
 * skin pixel the part shows.
 */
public final class PartItems {

    private static final String MINECRAFT = "minecraft:";

    private final ItemType base;
    private final boolean itemModels;

    /**
     * Prepare to build items.
     *
     * @param baseItem the item every part is shown as, such as {@code paper}
     * @param itemModels whether parts name their model with {@code item_model}, from 1.21.4
     */
    public PartItems(final String baseItem, final boolean itemModels) {
        ItemType type = ItemTypes.getByName(MINECRAFT + baseItem.toLowerCase(Locale.ROOT));
        this.base = type == null ? ItemTypes.PAPER : type;
        this.itemModels = itemModels;
    }

    /**
     * Build the item that shows a model part.
     *
     * @param part the part
     * @param look how the part looks
     * @param skin the skin the model wears, or null when it wears none
     * @return the item, {@link ItemStack#EMPTY} when the part is hidden
     */
    public ItemStack part(final ModelPart part, final DisplayLook look, final @Nullable HyperionSkin skin) {
        if (!look.visible()) {
            return ItemStack.EMPTY;
        }

        ItemStack item = ItemStack.builder().type(this.base).amount(1).build();
        if (this.itemModels) {
            item.setComponent(ComponentTypes.ITEM_MODEL, new ItemModel(new ResourceLocation(part.model())));
            SkinMap pixels = part.skin();
            if (pixels != null && skin != null) {
                item.setComponent(ComponentTypes.CUSTOM_MODEL_DATA_LISTS, modelData(skin.colors(pixels)));
            }
        } else {
            item.setComponent(ComponentTypes.CUSTOM_MODEL_DATA, part.customModelData());
        }

        Color tint = look.tint();
        if (tint != null) {
            item.setComponent(ComponentTypes.DYED_COLOR, new ItemDyeColor(tint.asRGB()));
        }

        if (look.glint()) {
            item.setComponent(ComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        return item;
    }

    /**
     * Turn the colors of a skin on a part into the component that carries them.
     *
     * @param colors the colors and flags
     * @return the custom model data
     */
    private static ItemCustomModelData modelData(final SkinColors colors) {
        // PacketEvents has a Color of its own; Bukkit's is imported for the tint.
        List<com.github.retrooper.packetevents.protocol.color.Color> list = new ArrayList<>(colors.colors().size());
        for (int argb : colors.colors()) {
            list.add(new com.github.retrooper.packetevents.protocol.color.Color(argb));
        }

        return new ItemCustomModelData(List.of(), colors.flags(), List.of(), list);
    }

    /**
     * Build the display item for an item a bone was given in place of its cubes.
     *
     * @param item the item, or null or empty for nothing
     * @param look how the bone looks; only visibility and glint apply to a given item
     * @return the item, {@link ItemStack#EMPTY} for nothing
     */
    // Bukkit's ItemStack is qualified: the PacketEvents ItemStack this class builds has the same name.
    public ItemStack given(final org.bukkit.inventory.@Nullable ItemStack item, final DisplayLook look) {
        if (!look.visible() || item == null || item.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack converted = SpigotConversionUtil.fromBukkitItemStack(item);
        if (look.glint()) {
            converted.setComponent(ComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        return converted;
    }
}
