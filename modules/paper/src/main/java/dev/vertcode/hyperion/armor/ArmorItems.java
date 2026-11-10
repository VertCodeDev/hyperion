package dev.vertcode.hyperion.armor;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.StringJoiner;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.armor.ArmorStats;
import dev.vertcode.hyperion.api.version.Feature;
import dev.vertcode.hyperion.api.version.MinecraftVersion;

/**
 * Build the items of armor pieces, the way the server's version wears and draws them.
 *
 * <ul>
 *   <li>From 1.21.4: the base armor item with {@code item_model} for its icon and
 *       {@code equippable} naming the set's equipment asset. A helmet is worn without an asset,
 *       so its item model shows on the head.</li>
 *   <li>1.21.2 to 1.21.3: the same, with custom model data for the icon.</li>
 *   <li>1.21 to 1.21.1: leather armor in the color that names the set. A helmet is the pack's
 *       base item with custom model data, which Hyperion lets players put on.</li>
 * </ul>
 *
 * <p>Thread-safe: the pack's numbers are swapped in whole after every build.
 */
public final class ArmorItems {

    /**
     * The tag every piece carries: its set's key and its slot.
     */
    public static final NamespacedKey TAG = new NamespacedKey("hyperion", "armor");

    private static final String MINECRAFT = "minecraft:";
    private static final String NOT_BUILT = "The resource pack has not been built yet, so armor items have no models";
    private static final String SEPARATOR = "|";
    private static final String[][] ATTRIBUTES = {
        {"armor", "generic.armor"},
        {"armor_toughness", "generic.armor_toughness"},
        {"knockback_resistance", "generic.knockback_resistance"}
    };

    private final MinecraftVersion version;
    private volatile String baseItem;
    private volatile @Nullable ArmorPackInfo info;

    /**
     * Prepare to build items.
     *
     * @param version the server's version
     * @param baseItem the pack's base item, which 1.21 to 1.21.1 helmets are made from
     */
    public ArmorItems(final MinecraftVersion version, final String baseItem) {
        this.version = version;
        this.baseItem = baseItem;
    }

    /**
     * Use the numbers of a newly built pack.
     *
     * @param next the pack's armor numbers
     */
    public void use(final ArmorPackInfo next) {
        this.info = next;
    }

    /**
     * Use a new base item after a reload.
     *
     * @param next the base item, such as {@code paper}
     */
    public void baseItem(final String next) {
        this.baseItem = next;
    }

    /**
     * Build a piece's item.
     *
     * @param piece the piece
     * @param color the dye color, or null for none
     * @return a new item
     * @throws IllegalStateException if the pack has not been built yet
     */
    public ItemStack build(final HyperionArmorPiece piece, final @Nullable Color color) {
        ArmorPackInfo current = this.info;
        if (current == null) {
            throw new IllegalStateException(NOT_BUILT);
        }

        HyperionArmorSet set = piece.set();
        ArmorSlot slot = piece.slot();
        ItemStack item = Bukkit.getItemFactory().createItemStack(this.describe(piece, current, color));
        item.editMeta(meta -> {
            meta.displayName(name(set, slot));
            if (!Feature.EQUIPPABLE.availableOn(this.version) && slot != ArmorSlot.HEAD) {
                meta.addItemFlags(ItemFlag.HIDE_DYE);
            }

            stats(meta, slot, piece.stats());
            meta.getPersistentDataContainer().set(TAG, PersistentDataType.STRING, tag(set.key(), slot));
        });

        return item;
    }

    /**
     * Write a piece's item in Minecraft's item syntax, with the components it needs on this
     * version.
     *
     * @param piece the piece
     * @param info the pack's numbers
     * @param color the dye color, or null for none
     * @return the item, such as {@code minecraft:netherite_chestplate[minecraft:item_model=...]}
     */
    String describe(final HyperionArmorPiece piece, final ArmorPackInfo info, final @Nullable Color color) {
        HyperionArmorSet set = piece.set();
        ArmorSlot slot = piece.slot();
        StringJoiner components = new StringJoiner(",", "[", "]");
        components.setEmptyValue("");
        OptionalInt customModelData = info.customModelData(set.key(), slot);
        if (!Feature.ITEM_MODEL_DEFINITIONS.availableOn(this.version) && customModelData.isPresent()) {
            components.add("minecraft:custom_model_data=" + customModelData.getAsInt());
        }

        String item;
        if (!Feature.EQUIPPABLE.availableOn(this.version)) {
            if (slot == ArmorSlot.HEAD) {
                item = this.baseItem;
                components.add("minecraft:max_stack_size=1");
            } else {
                item = ArmorMaterial.LEATHER.item(slot);
                OptionalInt setColor = info.setColor(set.key());
                if (setColor.isPresent()) {
                    components.add("minecraft:dyed_color=" + setColor.getAsInt());
                }
            }

            return MINECRAFT + item + components;
        }

        item = set.material().item(slot);
        if (Feature.ITEM_MODEL_DEFINITIONS.availableOn(this.version)) {
            components.add("minecraft:item_model=\"" + itemModel(set.key(), slot) + "\"");
        }

        String asset = Feature.ITEM_MODEL_DEFINITIONS.availableOn(this.version) ? "asset_id" : "model";
        StringJoiner equippable = new StringJoiner(",", "{", "}");
        equippable.add("slot:\"" + slot.equipmentSlot().name().toLowerCase(Locale.ROOT) + "\"");
        if (slot != ArmorSlot.HEAD) {
            equippable.add(asset + ":\"" + set.key().asString() + "\"");
        }

        equippable.add("equip_sound:\"" + set.material().equipSound() + "\"");
        components.add("minecraft:equippable=" + equippable);
        if (color != null) {
            components.add("minecraft:dyed_color=" + color.asRGB());
        }

        return MINECRAFT + item + components;
    }

    /**
     * Return the item model a piece names from 1.21.4.
     *
     * @param set the set's key
     * @param slot the piece's slot
     * @return the model's id, such as {@code hyperion:armor/frost/chestplate}
     */
    public static String itemModel(final Key set, final ArmorSlot slot) {
        return set.namespace() + ":armor/" + set.value() + "/" + slot.piece();
    }

    /**
     * Write the tag a piece carries.
     *
     * @param set the set's key
     * @param slot the piece's slot
     * @return the tag's value
     */
    public static String tag(final Key set, final ArmorSlot slot) {
        return set.asString() + SEPARATOR + slot.name();
    }

    /**
     * Name a piece for players: the set's name and the piece, not in italics.
     *
     * @param set the set
     * @param slot the piece's slot
     * @return the name
     */
    private static Component name(final HyperionArmorSet set, final ArmorSlot slot) {
        String piece = Character.toUpperCase(slot.piece().charAt(0)) + slot.piece().substring(1);

        return MiniMessage.miniMessage()
                .deserialize(set.displayName() + " " + piece)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * Give an item a piece's stats, in place of its base item's.
     *
     * @param meta the item's meta
     * @param slot the piece's slot
     * @param stats the stats
     */
    private static void stats(final ItemMeta meta, final ArmorSlot slot, final ArmorStats stats) {
        double[] amounts = {stats.armor(), stats.toughness(), stats.knockbackResistance()};
        EquipmentSlotGroup group = switch (slot) {
            case HEAD -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET;
        };
        for (int index = 0; index < ATTRIBUTES.length; index++) {
            // The armor modifier goes on even at zero, so the base item's own armor never shows.
            if (index > 0 && amounts[index] == 0d) {
                continue;
            }

            Attribute attribute = attribute(ATTRIBUTES[index]);
            if (attribute == null) {
                continue;
            }

            NamespacedKey key = new NamespacedKey("hyperion", "armor_" + ATTRIBUTES[index][0] + "_" + slot.piece());
            meta.addAttributeModifier(attribute, new AttributeModifier(key, amounts[index], AttributeModifier.Operation.ADD_NUMBER, group));
        }
    }

    /**
     * Find an attribute by key. Its key lost the {@code generic.} part in 1.21.2, so both are
     * tried.
     *
     * @param names the keys to try
     * @return the attribute, or null when the server has none by those keys
     */
    private static @Nullable Attribute attribute(final String[] names) {
        for (String name : names) {
            Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(name));
            if (attribute != null) {
                return attribute;
            }
        }

        return null;
    }
}
